package com.tpsk.tj.chat.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.tpsk.tj.chat.data.local.entity.CallLogEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface CallLogDao {
    @Query("SELECT * FROM call_logs ORDER BY timestamp DESC")
    fun getCallLogs(): Flow<List<CallLogEntity>>

    @Query("SELECT * FROM call_logs ORDER BY timestamp DESC")
    suspend fun getCallLogsOnce(): List<CallLogEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCallLogs(logs: List<CallLogEntity>)

    @Query("DELETE FROM call_logs")
    suspend fun clearCallLogs()
}
