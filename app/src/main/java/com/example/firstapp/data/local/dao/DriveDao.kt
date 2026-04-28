package com.example.firstapp.data.local.dao

import androidx.room.*
import com.example.firstapp.data.local.entity.DestinationEntity
import com.example.firstapp.data.local.entity.RideOptionEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface DriveDao {
    @Query("SELECT * FROM ride_options")
    fun getRideOptions(): Flow<List<RideOptionEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRideOptions(options: List<RideOptionEntity>)

    @Query("SELECT * FROM destinations ORDER BY id DESC")
    fun getDestinations(): Flow<List<DestinationEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDestinations(destinations: List<DestinationEntity>)

    @Query("DELETE FROM ride_options")
    suspend fun clearRideOptions()
}
