package com.example.firstapp.common.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.firstapp.common.data.local.entity.JobApplicationEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface JobApplicationDao {
    @Query("SELECT * FROM job_applications ORDER BY appliedDate DESC")
    fun getApplications(): Flow<List<JobApplicationEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertApplications(applications: List<JobApplicationEntity>)

    @Query("DELETE FROM job_applications")
    suspend fun clearApplications()

    @Query("DELETE FROM job_applications WHERE id = :applicationId")
    suspend fun deleteApplication(applicationId: String)
}
