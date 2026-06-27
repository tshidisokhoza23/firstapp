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

@HiltViewModel
class AuthViewModel @Inject constructor(
    private val userRepository: UserRepository
) : ViewModel() {

    private val _authState = MutableStateFlow<Resource<AuthResponse>?>(null)
    val authState: StateFlow<Resource<AuthResponse>?> = _authState.asStateFlow()

    private var pendingAuthResponse: AuthResponse? = null

    private val _otpState = MutableStateFlow<Resource<String>?>(null)
    val otpState: StateFlow<Resource<String>?> = _otpState.asStateFlow()

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
                    val msg = body?.code?.let { "DEBUG OTP: $it" } ?: "Code sent to $phoneNumber"
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
}
