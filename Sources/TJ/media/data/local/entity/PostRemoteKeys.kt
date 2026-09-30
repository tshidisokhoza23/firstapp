package com.tpsk.tj.media.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "post_remote_keys", primaryKeys = ["postId", "feedType"])
data class PostRemoteKeys(
    val postId: String,
    val feedType: String = "standard",
    val prevKey: Int?,
    val nextKey: Int?
)
