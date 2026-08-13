package com.example.firstapp.chat.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.firstapp.chat.data.local.entity.AdvertEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface AdvertDao {
    @Query("SELECT * FROM adverts WHERE isViewed = 0")
    fun getAdverts(): Flow<List<AdvertEntity>>

    @Query("SELECT * FROM adverts WHERE isViewed = 1")
    fun getViewedAdverts(): Flow<List<AdvertEntity>>

    @Query("UPDATE adverts SET isViewed = 1 WHERE id = :advertId")
    suspend fun markAsViewed(advertId: String)

    @Query("SELECT * FROM adverts WHERE id = :advertId")
    suspend fun getAdvertById(advertId: String): AdvertEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAdverts(adverts: List<AdvertEntity>)

    @Query("DELETE FROM adverts")
    suspend fun clearAdverts()
}
