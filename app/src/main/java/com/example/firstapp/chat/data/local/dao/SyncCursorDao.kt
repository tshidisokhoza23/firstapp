package com.example.firstapp.chat.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.firstapp.chat.data.local.entity.SyncCursorEntity

@Dao
interface SyncCursorDao {
    @Query("SELECT * FROM sync_cursors WHERE entityType = :entityType LIMIT 1")
    suspend fun getCursor(entityType: String): SyncCursorEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertCursor(cursor: SyncCursorEntity)
}
