package com.metroair.job_card_management.di

import android.content.Context
import com.metroair.job_card_management.data.local.database.JobCardDatabase
import com.metroair.job_card_management.data.local.database.dao.AssetDao
import com.metroair.job_card_management.data.local.database.dao.CurrentTechnicianDao
import com.metroair.job_card_management.data.local.database.dao.CustomerDao
import com.metroair.job_card_management.data.local.database.dao.FixedDao
import com.metroair.job_card_management.data.local.database.dao.JobCardDao
import com.metroair.job_card_management.data.local.database.dao.JobFixedAssetDao
import com.metroair.job_card_management.data.local.database.dao.JobInventoryUsageDao
import com.metroair.job_card_management.data.local.database.dao.JobPurchaseDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideJobCardDatabase(@ApplicationContext context: Context): JobCardDatabase {
        return JobCardDatabase.getInstance(context)
    }

    @Provides
    fun provideJobCardDao(database: JobCardDatabase): JobCardDao = database.jobCardDao()

    @Provides
    fun provideCustomerDao(database: JobCardDatabase): CustomerDao = database.customerDao()

    @Provides
    fun provideAssetDao(database: JobCardDatabase): AssetDao = database.assetDao()

    @Provides
    fun provideFixedDao(database: JobCardDatabase): FixedDao = database.fixedDao()

    @Provides
    fun provideCurrentTechnicianDao(database: JobCardDatabase): CurrentTechnicianDao = database.currentTechnicianDao()

    @Provides
    fun provideJobInventoryUsageDao(database: JobCardDatabase): JobInventoryUsageDao = database.jobInventoryUsageDao()

    @Provides
    fun provideJobFixedAssetDao(database: JobCardDatabase): JobFixedAssetDao = database.jobFixedAssetDao()

    @Provides
    fun provideJobPurchaseDao(database: JobCardDatabase): JobPurchaseDao = database.jobPurchaseDao()
}
