package com.tpsk.tj.chat.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Tracks the last-synced server sequence for incremental delta sync.
 * One row per entity type (e.g. "messages").
 */
@Entity(tableName = "sync_cursors")
data class SyncCursorEntity(
    @PrimaryKey val entityType: String,
    val lastSyncedSequence: Long = 0,
    val lastSyncedAt: Long = 0
)
