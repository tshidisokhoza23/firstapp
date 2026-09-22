package com.tpsk.tj.media.model

data class Category(
    val id: String,
    val name: String,
    val iconUrl: String? = null,
    val isSelected: Boolean = false
)

val DefaultCategories = listOf(
    Category("all", "All"),
    Category("tech", "Technology"),
    Category("music", "Music"),
    Category("gaming", "Gaming"),
    Category("sports", "Sports"),
    Category("news", "News"),
    Category("entertainment", "Entertainment"),
    Category("art", "Art")
)
