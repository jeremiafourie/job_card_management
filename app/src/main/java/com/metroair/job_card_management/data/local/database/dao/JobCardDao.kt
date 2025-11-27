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

    // ========== MY JOBS (Assigned to current technician) ==========

    @Query("SELECT * FROM job_cards WHERE isMyJob = 1 ORDER BY CASE status " +
           "WHEN 'BUSY' THEN 0 " +
           "WHEN 'PAUSED' THEN 1 " +
           "WHEN 'EN_ROUTE' THEN 2 " +
           "WHEN 'PENDING' THEN 3 " +
           "WHEN 'COMPLETED' THEN 4 " +
           "ELSE 5 END, scheduledDate ASC, scheduledTime ASC")
    fun getMyJobs(): Flow<List<JobCardEntity>>

    @Query("SELECT * FROM job_cards WHERE isMyJob = 1 AND status IN ('BUSY', 'PAUSED') LIMIT 1")
    fun getMyCurrentActiveJob(): Flow<JobCardEntity?>

    @Query("SELECT COUNT(*) FROM job_cards WHERE isMyJob = 1 AND status IN ('BUSY', 'EN_ROUTE')")
    suspend fun getMyActiveJobCount(): Int

    @Query("SELECT * FROM job_cards WHERE isMyJob = 1 AND DATE(scheduledDate) = DATE('now') " +
           "AND status NOT IN ('COMPLETED', 'CANCELLED') " +
           "ORDER BY scheduledTime ASC")
    fun getMyTodayPendingJobs(): Flow<List<JobCardEntity>>

    @Query("SELECT * FROM job_cards WHERE isMyJob = 1 AND status = :status")
    fun getMyJobsByStatus(status: String): Flow<List<JobCardEntity>>

    @Query("SELECT COUNT(*) FROM job_cards WHERE isMyJob = 1 AND DATE(scheduledDate) = DATE('now') " +
           "AND status = 'COMPLETED'")
    suspend fun getTodayCompletedCount(): Int

    // ========== AVAILABLE JOBS (Unassigned) ==========

    @Query("SELECT * FROM job_cards WHERE isMyJob = 0 " +
           "ORDER BY scheduledDate ASC, scheduledTime ASC")
    fun getAvailableJobs(): Flow<List<JobCardEntity>>

    @Query("UPDATE job_cards SET isMyJob = 1, acceptedAt = :timestamp, status = 'PENDING', updatedAt = :timestamp " +
           "WHERE id = :jobId AND isMyJob = 0")
    suspend fun claimJob(jobId: Int, timestamp: Long): Int

    @Query("UPDATE job_cards SET acceptedAt = :timestamp, status = 'PENDING', updatedAt = :timestamp " +
           "WHERE id = :jobId AND isMyJob = 1 AND status = 'AWAITING'")
    suspend fun acceptJob(jobId: Int, timestamp: Long): Int

    // ========== JOB STATUS UPDATES ==========

    @Query("UPDATE job_cards SET status = :status, updatedAt = :timestamp, " +
           "enRouteStartTime = CASE WHEN :status = 'EN_ROUTE' THEN :timestamp ELSE enRouteStartTime END, " +
           "startTime = CASE WHEN :status = 'BUSY' THEN :startTime ELSE startTime END " +
           "WHERE id = :id AND isMyJob = 1")
    suspend fun startJob(id: Int, status: String, timestamp: Long, startTime: Long)

    @Query("UPDATE job_cards SET status = 'PAUSED', pausedTime = COALESCE(pausedTime, 0) + :pauseDuration, " +
           "pauseHistory = :pauseHistory, updatedAt = :timestamp WHERE id = :id AND isMyJob = 1")
    suspend fun pauseJob(id: Int, pauseDuration: Long, pauseHistory: String?, timestamp: Long)

    @Query("UPDATE job_cards SET status = 'BUSY', updatedAt = :timestamp " +
           "WHERE id = :id AND isMyJob = 1")
    suspend fun resumeJobToBusy(id: Int, timestamp: Long)

    @Query("UPDATE job_cards SET status = 'EN_ROUTE', updatedAt = :timestamp " +
           "WHERE id = :id AND isMyJob = 1")
    suspend fun resumeJobToEnRoute(id: Int, timestamp: Long)

    @Query("UPDATE job_cards SET status = 'COMPLETED', endTime = :endTime, " +
           "workPerformed = :workPerformed, technicianNotes = :notes, " +
           "resourcesUsed = :resourcesUsed, requiresFollowUp = :requiresFollowUp, " +
           "followUpNotes = :followUpNotes, updatedAt = :timestamp " +
           "WHERE id = :id AND isMyJob = 1")
    suspend fun completeJob(
        id: Int,
        endTime: Long,
        workPerformed: String?,
        notes: String?,
        resourcesUsed: String?,
        requiresFollowUp: Boolean,
        followUpNotes: String?,
        timestamp: Long
    )

    @Query("UPDATE job_cards SET customerSignature = :signature, updatedAt = :timestamp " +
           "WHERE id = :id AND isMyJob = 1")
    suspend fun addCustomerSignature(id: Int, signature: String, timestamp: Long)

    @Query("UPDATE job_cards SET beforePhotos = :photos, updatedAt = :timestamp " +
           "WHERE id = :id AND isMyJob = 1")
    suspend fun addBeforePhotos(id: Int, photos: String?, timestamp: Long)

    @Query("UPDATE job_cards SET afterPhotos = :photos, updatedAt = :timestamp " +
           "WHERE id = :id AND isMyJob = 1")
    suspend fun addAfterPhotos(id: Int, photos: String?, timestamp: Long)

    @Query("UPDATE job_cards SET otherPhotos = :photos, updatedAt = :timestamp " +
           "WHERE id = :id AND isMyJob = 1")
    suspend fun addOtherPhotos(id: Int, photos: String?, timestamp: Long)

    @Query("SELECT * FROM job_cards WHERE id = :id")
    fun getJobCardByIdFlow(id: Int): Flow<JobCardEntity?>

    @Query("UPDATE job_cards SET workPerformed = :workPerformed, " +
           "technicianNotes = :technicianNotes, issuesEncountered = :issuesEncountered, " +
           "customerSignature = :customerSignature, requiresFollowUp = :requiresFollowUp, " +
           "followUpNotes = :followUpNotes, updatedAt = :updatedAt " +
           "WHERE id = :jobId AND isMyJob = 1")
    suspend fun updateJobDetails(
        jobId: Int,
        workPerformed: String?,
        technicianNotes: String?,
        issuesEncountered: String?,
        customerSignature: String?,
        requiresFollowUp: Boolean,
        followUpNotes: String?,
        updatedAt: Long
    )

    @Query("SELECT * FROM job_cards WHERE " +
           "(customerName LIKE :query OR " +
           "title LIKE :query OR " +
           "serviceAddress LIKE :query OR " +
           "jobNumber LIKE :query) " +
           "ORDER BY scheduledDate DESC, scheduledTime ASC")
    fun searchJobs(query: String): Flow<List<JobCardEntity>>

    // ========== SYNC OPERATIONS ==========

    @Query("SELECT * FROM job_cards WHERE isSynced = 0")
    suspend fun getUnsyncedJobs(): List<JobCardEntity>

    @Query("UPDATE job_cards SET isSynced = 1, lastSyncedAt = :timestamp WHERE id = :id")
    suspend fun markAsSynced(id: Int, timestamp: Long)

    // ========== GENERAL OPERATIONS ==========

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertJob(jobCard: JobCardEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertJobs(jobCards: List<JobCardEntity>)

    @Update
    suspend fun updateJob(jobCard: JobCardEntity)

    @Query("SELECT * FROM job_cards WHERE id = :id")
    suspend fun getJobById(id: Int): JobCardEntity?

    @Query("DELETE FROM job_cards WHERE isMyJob = 0") // Only delete unassigned jobs
    suspend fun deleteAvailableJobs()

    @Query("DELETE FROM job_cards WHERE status = 'COMPLETED' AND DATE(endTime/1000, 'unixepoch') < DATE('now', '-30 days')")
    suspend fun deleteOldCompletedJobs()
}