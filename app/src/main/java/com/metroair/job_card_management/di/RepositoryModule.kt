package com.metroair.job_card_management.di

import com.metroair.job_card_management.data.repository.JobCardRepository
import com.metroair.job_card_management.data.repository.JobCardRepositoryImpl
import com.metroair.job_card_management.data.repository.ResourceRepository
import com.metroair.job_card_management.data.repository.ResourceRepositoryImpl
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {

    @Binds
    @Singleton
    abstract fun bindJobCardRepository(
        jobCardRepositoryImpl: JobCardRepositoryImpl
    ): JobCardRepository

    @Binds
    @Singleton
    abstract fun bindResourceRepository(
        resourceRepositoryImpl: ResourceRepositoryImpl
    ): ResourceRepository
}