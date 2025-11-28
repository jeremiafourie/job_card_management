package com.metroair.job_card_management.di

import com.metroair.job_card_management.data.repository.AssetRepository
import com.metroair.job_card_management.data.repository.AssetRepositoryImpl
import com.metroair.job_card_management.data.repository.FixedRepository
import com.metroair.job_card_management.data.repository.FixedRepositoryImpl
import com.metroair.job_card_management.data.repository.JobCardRepository
import com.metroair.job_card_management.data.repository.JobCardRepositoryImpl
import com.metroair.job_card_management.data.repository.PurchaseRepository
import com.metroair.job_card_management.data.repository.PurchaseRepositoryImpl
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
    abstract fun bindAssetRepository(
        assetRepositoryImpl: AssetRepositoryImpl
    ): AssetRepository

    @Binds
    @Singleton
    abstract fun bindFixedRepository(
        fixedRepositoryImpl: FixedRepositoryImpl
    ): FixedRepository

    @Binds
    @Singleton
    abstract fun bindPurchaseRepository(
        purchaseRepositoryImpl: PurchaseRepositoryImpl
    ): PurchaseRepository
}
