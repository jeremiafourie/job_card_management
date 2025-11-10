package com.metroair.job_card_management.data.local.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.metroair.job_card_management.data.local.database.entities.ResourceEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ResourceDao {
    @Query("SELECT * FROM resources ORDER BY itemName")
    fun getAllResources(): Flow<List<ResourceEntity>>

    @Query("SELECT * FROM resources WHERE category = :category")
    fun getResourcesByCategory(category: String): Flow<List<ResourceEntity>>

    @Query("UPDATE resources SET currentStock = currentStock - :quantity WHERE id = :id")
    suspend fun useResource(id: Int, quantity: Int)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAllResources(resources: List<ResourceEntity>)
}