package com.tpsk.tj.chat.jobs.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.tpsk.tj.chat.jobs.data.local.entity.JobEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface JobDao {
    @Query("SELECT * FROM jobs ORDER BY postedTime DESC")
    fun getJobs(): Flow<List<JobEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertJobs(jobs: List<JobEntity>)

    @Query("UPDATE jobs SET isApplied = :isApplied WHERE id = :jobId")
    suspend fun updateAppliedStatus(jobId: String, isApplied: Boolean)

    @Query("DELETE FROM jobs")
    suspend fun clearJobs()

    @Query("SELECT * FROM jobs WHERE LOWER(title) LIKE '%' || LOWER(:query) || '%' OR LOWER(companyName) LIKE '%' || LOWER(:query) || '%' OR LOWER(description) LIKE '%' || LOWER(:query) || '%' ORDER BY postedTime DESC")
    fun searchJobs(query: String): Flow<List<JobEntity>>
}
