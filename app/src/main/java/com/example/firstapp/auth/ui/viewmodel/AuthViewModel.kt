package com.example.firstapp.auth.ui.viewmodel

import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.firstapp.common.data.TokenManager
import com.example.firstapp.common.data.api.ApiClient
import com.example.firstapp.common.data.api.AuthResponse
import com.example.firstapp.common.util.Resource
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

import com.example.firstapp.vocab.domain.repository.UserRepository
import java.util.Locale

@HiltViewModel
class AuthViewModel @Inject constructor(
    private val userRepository: UserRepository
) : ViewModel() {

    private val _authState = MutableStateFlow<Resource<AuthResponse>?>(null)
    val authState: StateFlow<Resource<AuthResponse>?> = _authState.asStateFlow()

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
                        pendingAuthResponse = authResponse
                        val phoneNumber = authResponse.user.phone_number ?: ""
                        if (phoneNumber.length > 0) {
                            requestOTP(phoneNumber)
                        } else {
                            saveToken(authResponse)
                            _authState.value = resource
                        }
                    } else {
                        _authState.value = Resource.Error("Login failed")
                    }
                } else {
                    _authState.value = resource
                }
            }
        }
    }

    fun requestOTP(phoneNumber: String) {
        viewModelScope.launch {
            _otpState.value = Resource.Loading()
            lastRequestedPhone = phoneNumber
            try {
                val response = ApiClient.authApi.requestOTP(mapOf("phone_number" to phoneNumber))
                if (response.isSuccessful) {
                    val body = response.body()
                    val msg = body?.code?.let { "DEBUG OTP: $it" } ?: "Verification code requested"
                    _otpState.value = Resource.Success(msg)
                } else {
                    _otpState.value = Resource.Error(response.errorBody()?.string() ?: "Failed to send code")
                }
            } catch (e: Exception) {
                _otpState.value = Resource.Error("Connection error")
            }
        }
    }

    fun verifyOTP(phoneNumber: String, code: String, onVerified: () -> Unit) {
        viewModelScope.launch {
            _otpState.value = Resource.Loading()
            try {
                val response = ApiClient.authApi.verifyOTP(mapOf("phone_number" to phoneNumber, "code" to code))
                if (response.isSuccessful) {
                    _otpState.value = Resource.Success("Verified")
                    onVerified()
                } else {
                    _otpState.value = Resource.Error("Invalid or expired code")
                }
            } catch (e: Exception) {
                _otpState.value = Resource.Error("Connection error")
            }
        }
    }

    fun register(username: String, email: String, displayName: String, phoneNumber: String, password: String) {
        viewModelScope.launch {
            userRepository.register(username, email, displayName, phoneNumber, password).collect { resource ->
                if (resource is Resource.Success) {
                    saveToken(resource.data)
                }
                _authState.value = resource
            }
        }
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
            TokenManager.setToken(it.token)
            if (it.refreshToken.isNotBlank()) {
                TokenManager.setRefreshToken(it.refreshToken)
            }
        }
    }

    fun resetState() {
        _authState.value = null
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
                val response = ApiClient.authApi.checkAvailability(username, email, phone)
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
