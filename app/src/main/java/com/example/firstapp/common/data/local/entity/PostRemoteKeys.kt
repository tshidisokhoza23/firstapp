package com.example.firstapp.common.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "post_remote_keys")
data class PostRemoteKeys(
    @PrimaryKey val postId: String,
    val prevKey: Int?,
    val nextKey: Int?
)
