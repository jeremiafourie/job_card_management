package com.metroair.job_card_management.di

import android.content.Context
import com.metroair.job_card_management.data.local.database.JobCardDatabase
import com.metroair.job_card_management.data.local.database.dao.CurrentTechnicianDao
import com.metroair.job_card_management.data.local.database.dao.CustomerDao
import com.metroair.job_card_management.data.local.database.dao.JobCardDao
import com.metroair.job_card_management.data.local.database.dao.ResourceDao
import com.metroair.job_card_management.data.local.database.dao.ToolCheckoutDao
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
    fun provideResourceDao(database: JobCardDatabase): ResourceDao = database.resourceDao()

    @Provides
    fun provideCurrentTechnicianDao(database: JobCardDatabase): CurrentTechnicianDao = database.currentTechnicianDao()

    @Provides
    fun provideToolCheckoutDao(database: JobCardDatabase): ToolCheckoutDao = database.toolCheckoutDao()
}