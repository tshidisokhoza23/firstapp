package com.example.firstapp.common.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.firstapp.common.data.local.entity.CallLogEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface CallLogDao {
    @Query("SELECT * FROM call_logs ORDER BY timestamp DESC")
    fun getCallLogs(): Flow<List<CallLogEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCallLogs(logs: List<CallLogEntity>)

    @Query("DELETE FROM call_logs")
    suspend fun clearCallLogs()
}
