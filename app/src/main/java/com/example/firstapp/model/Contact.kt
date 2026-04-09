package com.example.firstapp.model

data class Contact(
    val id: Int,
    val username: String,
    val displayName: String,
    val avatarUrl: String? = null,
    val phoneNumber: String = "",
    val isRegistered: Boolean = false,
    val isAuthenticated: Boolean = true,
    val isVerified: Boolean = false
)
