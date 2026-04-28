package com.example.firstapp.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.firstapp.data.local.entity.AdvertEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface AdvertDao {
    @Query("SELECT * FROM adverts")
    fun getAdverts(): Flow<List<AdvertEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAdverts(adverts: List<AdvertEntity>)

    @Query("DELETE FROM adverts")
    suspend fun clearAdverts()
}
