package com.tpsk.tj.chat.model

data class Contact(
    val id: String,
    val username: String,
    val displayName: String,
    val avatarUrl: String? = null,
    val phoneNumber: String = "",
    val isRegistered: Boolean = false,
    val isAuthenticated: Boolean = true,
    val isVerified: Boolean = false,
    val bio: String? = null,
    val publicKey: String? = null,
    val isFollowing: Boolean = false
)
