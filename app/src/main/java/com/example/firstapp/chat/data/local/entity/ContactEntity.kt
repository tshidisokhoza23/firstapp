package com.example.firstapp.chat.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "contacts")
data class ContactEntity(
    @PrimaryKey val id: String,
    val username: String,
    val displayName: String,
    val avatarUrl: String?,
    val phoneNumber: String,
    val isRegistered: Boolean,
    val isAuthenticated: Boolean,
    val isVerified: Boolean,
    val bio: String? = null,
    val publicKey: String? = null,
    val isFollowing: Boolean = false
)
