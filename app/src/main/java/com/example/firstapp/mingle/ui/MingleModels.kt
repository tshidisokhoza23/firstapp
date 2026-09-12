package com.example.firstapp.mingle.ui

data class MingleProfile(
    val id: String,
    val name: String,
    val age: Int,
    val distanceKm: Int,
    val bio: String,
    val photos: List<String>,
    val isLiked: Boolean = false
)

enum class MingleActivityType { MINGLE_LIKE, MATCH, VIEW, PHOTO_LIKE, SUPER_LIKE }

data class MingleActivityNotification(
    val id: String,
    val actorId: String,
    val actorName: String,
    val actorAvatarUrl: String?,
    val type: MingleActivityType,
    val actionText: String,
    val timeAgo: String,
    val photoUrl: String? = null
)
