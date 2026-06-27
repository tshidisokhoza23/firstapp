package com.example.firstapp.common.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.firstapp.common.data.local.entity.ShortEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ShortDao {
    @Query("SELECT * FROM shorts")
    fun getShorts(): Flow<List<ShortEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertShorts(shorts: List<ShortEntity>)

    @Query("DELETE FROM shorts")
    suspend fun clearShorts()
}
