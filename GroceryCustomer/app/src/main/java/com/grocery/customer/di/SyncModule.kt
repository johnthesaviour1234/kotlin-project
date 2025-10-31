package com.grocery.customer.di

import android.content.Context
import androidx.work.WorkManager
import com.grocery.customer.data.local.StateManager
import com.grocery.customer.data.remote.ApiService
import com.grocery.customer.data.repository.SyncRepositoryImpl
import com.grocery.customer.data.workers.SyncWorkerFactory
import com.grocery.customer.domain.repository.SyncRepository
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * Dependency injection module for state synchronization components
 */
@Module
@InstallIn(SingletonComponent::class)
abstract class SyncModule {
    
    /**
     * Bind SyncRepository implementation
     */
    @Binds
    @Singleton
    abstract fun bindSyncRepository(
        syncRepositoryImpl: SyncRepositoryImpl
    ): SyncRepository
    
    @Module
    @InstallIn(SingletonComponent::class)
    object Providers {
    
        /**
         * Provide StateManager singleton
         */
        @Provides
        @Singleton
        fun provideStateManager(
            @ApplicationContext context: Context
        ): StateManager {
            return StateManager(context)
        }
    
        /**
         * Provide custom WorkerFactory for sync workers
         */
        @Provides
        @Singleton
        fun provideSyncWorkerFactory(
            syncRepository: SyncRepository
        ): SyncWorkerFactory {
            return SyncWorkerFactory(syncRepository)
        }
        
        /**
         * Provide WorkManager instance
         */
        @Provides
        @Singleton
        fun provideWorkManager(
            @ApplicationContext context: Context
        ): WorkManager {
            return WorkManager.getInstance(context)
        }
    }
}
