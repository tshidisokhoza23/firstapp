package com.example.firstapp.common.model

data class PrivacySettings(
    val show_last_seen: String = "everyone",
    val show_online_status: String = "everyone",
    val show_read_receipts: Boolean = true,
    val who_can_add_to_groups: String = "everyone",
    val who_can_see_stories: String = "my_contacts",
    val excluded_story_user_ids: List<String> = emptyList()
)
