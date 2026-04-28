package com.example.firstapp.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "contacts")
data class ContactEntity(
    @PrimaryKey val id: Int,
    val username: String,
    val displayName: String,
    val avatarUrl: String?,
    val phoneNumber: String,
    val isRegistered: Boolean,
    val isAuthenticated: Boolean,
    val isVerified: Boolean
)
