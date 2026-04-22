package com.example.firstapp.model

data class User(
    val id: Int,
    val username: String,
    val display_name: String,
    val phone_number: String? = null,
    val created_at: String,
    val avatar_url: String? = null,
    val bio: String? = null,
    val location: String? = null,
    val is_verified: Boolean = false
)
