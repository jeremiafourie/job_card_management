package com.metroair.job_card_management.data.local.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.metroair.job_card_management.data.local.database.entities.JobCardEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface JobCardDao {

    @Query("SELECT * FROM job_cards ORDER BY scheduled_date ASC, scheduled_time ASC")
    fun getJobs(): Flow<List<JobCardEntity>>

    @Query("SELECT * FROM job_cards")
    suspend fun getJobsOnce(): List<JobCardEntity>

    @Query("SELECT * FROM job_cards WHERE DATE(scheduled_date) = DATE('now') ORDER BY scheduled_time ASC")
    fun getTodayPendingJobs(): Flow<List<JobCardEntity>>

    @Query("SELECT * FROM job_cards WHERE id = :id")
    fun getJobCardByIdFlow(id: Int): Flow<JobCardEntity?>

    @Query("SELECT * FROM job_cards WHERE " +
           "(customer_name LIKE :query OR " +
           "title LIKE :query OR " +
           "service_address LIKE :query OR " +
           "job_number LIKE :query) " +
           "ORDER BY scheduled_date DESC, scheduled_time ASC")
    fun searchJobs(query: String): Flow<List<JobCardEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertJob(jobCard: JobCardEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertJobs(jobCards: List<JobCardEntity>)

    @Update
    suspend fun updateJob(jobCard: JobCardEntity)

    @Query("SELECT * FROM job_cards WHERE id = :id")
    suspend fun getJobById(id: Int): JobCardEntity?
}
