package com.example.firstapp.auth.ui.viewmodel

import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.firstapp.auth.data.api.AuthApi
import com.example.firstapp.common.domain.auth.SessionManager
import com.example.firstapp.common.data.api.AuthResponse
import com.example.firstapp.common.util.Resource
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

import com.example.firstapp.vocab.domain.repository.UserRepository
import com.example.firstapp.common.data.api.ForgotPasswordRequest

@HiltViewModel
class AuthViewModel @Inject constructor(
    private val userRepository: UserRepository,
    private val sessionManager: SessionManager,
    private val authApi: AuthApi
) : ViewModel() {

    private val _authState = MutableStateFlow<Resource<AuthResponse>?>(null)
    val authState: StateFlow<Resource<AuthResponse>?> = _authState.asStateFlow()

    private val _toastMessage = MutableStateFlow<String?>(null)
    val toastMessage: StateFlow<String?> = _toastMessage.asStateFlow()

    private var pendingAuthResponse: AuthResponse? = null


    private val _otpState = MutableStateFlow<Resource<String>?>(null)
    val otpState: StateFlow<Resource<String>?> = _otpState.asStateFlow()

    private val _fieldErrors = MutableStateFlow<Map<String, String>>(emptyMap())
    val fieldErrors: StateFlow<Map<String, String>> = _fieldErrors.asStateFlow()

    var lastRequestedPhone: String = ""
        private set

    fun login(username: String, password: String) {
        viewModelScope.launch {
            _authState.value = Resource.Loading()
            userRepository.login(username, password).collect { resource ->
                if (resource is Resource.Success) {
                    val authResponse = resource.data
                    if (authResponse != null) {
                        saveToken(authResponse)
                        _authState.value = resource
                    } else {
                        _authState.value = Resource.Error("Login failed")
                    }
                } else {
                    _authState.value = resource
                }
            }
        }
    }

    fun requestOTP(phoneNumber: String, countryCode: String? = null) {
        viewModelScope.launch {
            lastRequestedPhone = phoneNumber
            userRepository.requestOTP(phoneNumber, countryCode).collect { resource ->
                _otpState.value = resource
            }
        }
    }


    fun verifyOTP(phoneNumber: String, code: String, onVerified: () -> Unit) {
        viewModelScope.launch {
            userRepository.verifyOTP(phoneNumber, code).collect { resource ->
                if (resource is Resource.Success && resource.data == true) {
                    _otpState.value = Resource.Success("Verified")
                    onVerified()
                } else if (resource is Resource.Error) {
                    _otpState.value = Resource.Error(resource.message ?: "Verification failed")
                } else if (resource is Resource.Loading) {
                    _otpState.value = Resource.Loading()
                }
            }
        }
    }

    fun register(username: String, email: String, displayName: String, phoneNumber: String, password: String, countryCode: String? = null) {
        viewModelScope.launch {
            userRepository.register(username, email, displayName, phoneNumber, password, countryCode).collect { resource ->
                if (resource is Resource.Success) {
                    saveToken(resource.data)
                }
                _authState.value = resource
            }
        }
    }


    fun forgotPassword(emailOrUsername: String) {
        viewModelScope.launch {
            _authState.value = Resource.Loading()
            try {
                val response = authApi.forgotPassword(ForgotPasswordRequest(emailOrUsername))
                if (response.isSuccessful) {
                    _toastMessage.value = "Reset link sent!"
                    _authState.value = null
                } else {
                    _authState.value = Resource.Error("User not found or request failed")
                }
            } catch (e: Exception) {
                _authState.value = Resource.Error("Connection error: ${e.localizedMessage}")
            }
        }
    }

    fun clearToastMessage() {
        _toastMessage.value = null
    }

    fun commitLogin() {

        pendingAuthResponse?.let {
            saveToken(it)
            _authState.value = Resource.Success(it)
            pendingAuthResponse = null
        }
    }

    private fun saveToken(authResponse: AuthResponse?) {
        authResponse?.let {
            sessionManager.saveSession(it.token, it.refreshToken, it.user)
        }
    }

    fun resetState() {
        _authState.value = null
        _otpState.value = null
    }

    fun resetOtpState() {
        _otpState.value = null
    }

    fun setFieldError(field: String, error: String?) {
        val current = _fieldErrors.value.toMutableMap()
        if (error == null) {
            current.remove(field)
        } else {
            current.put(field, error)
        }
        _fieldErrors.value = current
    }

    fun checkAvailability(username: String? = null, email: String? = null, phone: String? = null, onResult: (Boolean) -> Unit = {}) {
        viewModelScope.launch {
            _authState.value = Resource.Loading()
            var allAvailable = false
            try {
                val response = authApi.checkAvailability(username, email, phone)
                if (response.isSuccessful) {
                    val availability = response.body()
                    if (availability != null) {
                        allAvailable = true
                        for (entry in availability.entries) {
                            val field = entry.key
                            val isAvailable = entry.value
                            if (!isAvailable) {
                                allAvailable = false
                                val label = when(field) {
                                    "phone" -> "Phone number"
                                    "username" -> "Username"
                                    "email" -> "Email"
                                    else -> field
                                }
                                setFieldError(field, "$label is already taken")
                            } else {
                                setFieldError(field, null)
                            }
                        }
                    }
                } else {
                    _authState.value = Resource.Error("Availability check failed")
                }
            } catch (e: Exception) {
                _authState.value = Resource.Error("Connection error: ${e.localizedMessage}")
            }

            if (allAvailable) {
                _authState.value = null
            } else if (_authState.value !is Resource.Error) {
                _authState.value = Resource.Error("Some details are already taken")
            }
            onResult(allAvailable)
        }
    }

    fun clearFieldErrors() {
        _fieldErrors.value = emptyMap()
    }
}
