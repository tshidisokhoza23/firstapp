package com.example.firstapp.mingle.data.api

data class MinglePhotoDto(
    val id: String? = null,
    val url: String? = null,
    val sort_order: Int = 0
)

data class MingleProfileDto(
    val user_id: String? = null,
    val name: String? = null,
    val bio: String? = null,
    val age: Int = 0,
    val gender: String? = null,
    val looking_for: String? = null,
    val max_distance_km: Int = 25,
    val is_discoverable: Boolean = true,
    val has_location: Boolean = false,
    val photos: List<MinglePhotoDto>? = emptyList()
)

data class MingleProfileResponse(
    val profile: MingleProfileDto?
)

data class MingleCandidateDto(
    val id: String? = null,
    val name: String? = null,
    val age: Int = 0,
    val bio: String? = null,
    val distance_km: Double = 0.0,
    val is_liked: Boolean = false,
    val photos: List<MinglePhotoDto>? = emptyList()
)

data class MingleFeedResponse(
    val items: List<MingleCandidateDto>? = emptyList(),
    val radius_km: Int = 25,
    val has_more: Boolean = false,
    val balance: Double = 0.0,
    val like_cost: Double = 10.0
)

data class MingleLocationRequest(
    val latitude: Double,
    val longitude: Double,
    val radius_km: Int? = null
)

data class MingleSwipeRequest(
    val target_user_id: String? = null,
    val action: String
)

data class MingleSwipeResultDto(
    val matched: Boolean = false,
    val match_id: String? = null,
    val conversation_id: String? = null,
    val already_acted: Boolean = false,
    val action: String? = null,
    val balance: Double = 0.0,
    val like_cost: Double = 10.0,
    val error: String? = null
)

data class MinglePhotoResponse(
    val photo: MinglePhotoDto?
)

data class MingleActivityDto(
    val id: String? = null,
    val actor_id: String? = null,
    val actor_name: String? = null,
    val actor_avatar_url: String? = null,
    val type: String? = null,
    val photo_url: String? = null,
    val created_at: Long = 0
)

data class MingleActivityResponse(
    val items: List<MingleActivityDto>? = emptyList()
)

data class MingleChatResponse(
    val conversation_id: String? = null,
    val type: String? = null,
    val error: String? = null
)
