package com.example.firstapp.common.data.api

import com.google.gson.annotations.SerializedName

data class StatusResponse(val status: String, val code: String? = null)

data class MuteRequest(val is_muted: Boolean)
data class PinRequest(val is_pinned: Boolean)
data class ArchiveRequest(val is_archived: Boolean)
data class ReportRequest(val reason: String)

data class DevResetRequest(val contacts: List<com.example.firstapp.common.util.ContactUtils.ContactInfo>)
data class DevResetResponse(val status: String, val username: String, val password: String)

data class RegisterRequest(val username: String, val email: String, val display_name: String, val phone_number: String, val password: String)
data class LoginRequest(val username: String, val password: String)
data class AuthResponse(
    @SerializedName("token") val token: String,
    @SerializedName("refresh_token") val refreshToken: String = "",
    @SerializedName("user") val user: com.example.firstapp.common.model.User
)

data class PushTokenRequest(val token: String, val device_id: String, val platform: String = "android")
data class ForgotPasswordRequest(val email_or_username: String)

data class UploadResponse(
    val url: String,
    val file_name: String,
    val file_size: Long
)
