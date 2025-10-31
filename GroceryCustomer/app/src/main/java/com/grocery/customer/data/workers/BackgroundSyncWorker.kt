package com.grocery.customer.data.workers

import android.content.Context
import android.util.Log
import androidx.work.*
import com.grocery.customer.domain.repository.SyncRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.concurrent.TimeUnit
import javax.inject.Inject

/**
 * Background worker for periodic state synchronization
 * 
 * Performs full sync of cart, orders, and profile at regular intervals:
 * - 15 seconds when app is in foreground
 * - 30 seconds when app is in background
 * 
 * Features:
 * - Network-aware (only syncs when online)
 * - Battery-optimized with constraints
 * - Automatic retry on failure
 * - Comprehensive logging
 */
class BackgroundSyncWorker @Inject constructor(
    context: Context,
    params: WorkerParameters,
    private val syncRepository: SyncRepository
) : CoroutineWorker(context, params) {
    
    companion object {
        private const val TAG = "BackgroundSyncWorker"
        const val WORK_NAME_FOREGROUND = "sync_foreground"
        const val WORK_NAME_BACKGROUND = "sync_background"
        
        private const val SYNC_INTERVAL_FOREGROUND_SECONDS = 15L
        private const val SYNC_INTERVAL_BACKGROUND_SECONDS = 30L
        
        /**
         * Schedule foreground sync (15 seconds interval)
         */
        fun scheduleForegroundSync(context: Context) {
            val constraints = Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build()
            
            val syncRequest = PeriodicWorkRequestBuilder<BackgroundSyncWorker>(
                SYNC_INTERVAL_FOREGROUND_SECONDS,
                TimeUnit.SECONDS,
                5, // Flex interval of 5 seconds
                TimeUnit.SECONDS
            )
                .setConstraints(constraints)
                .setBackoffCriteria(
                    BackoffPolicy.EXPONENTIAL,
                    WorkRequest.MIN_BACKOFF_MILLIS,
                    TimeUnit.MILLISECONDS
                )
                .addTag("sync")
                .addTag("foreground")
                .build()
            
            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                WORK_NAME_FOREGROUND,
                ExistingPeriodicWorkPolicy.KEEP,
                syncRequest
            )
            
            Log.d(TAG, "Foreground sync scheduled (every $SYNC_INTERVAL_FOREGROUND_SECONDS seconds)")
        }
        
        /**
         * Schedule background sync (30 seconds interval)
         */
        fun scheduleBackgroundSync(context: Context) {
            val constraints = Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .setRequiresBatteryNotLow(true) // Battery optimization for background
                .build()
            
            val syncRequest = PeriodicWorkRequestBuilder<BackgroundSyncWorker>(
                SYNC_INTERVAL_BACKGROUND_SECONDS,
                TimeUnit.SECONDS,
                5, // Flex interval of 5 seconds
                TimeUnit.SECONDS
            )
                .setConstraints(constraints)
                .setBackoffCriteria(
                    BackoffPolicy.EXPONENTIAL,
                    WorkRequest.MIN_BACKOFF_MILLIS,
                    TimeUnit.MILLISECONDS
                )
                .addTag("sync")
                .addTag("background")
                .build()
            
            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                WORK_NAME_BACKGROUND,
                ExistingPeriodicWorkPolicy.KEEP,
                syncRequest
            )
            
            Log.d(TAG, "Background sync scheduled (every $SYNC_INTERVAL_BACKGROUND_SECONDS seconds)")
        }
        
        /**
         * Cancel foreground sync
         */
        fun cancelForegroundSync(context: Context) {
            WorkManager.getInstance(context).cancelUniqueWork(WORK_NAME_FOREGROUND)
            Log.d(TAG, "Foreground sync cancelled")
        }
        
        /**
         * Cancel background sync
         */
        fun cancelBackgroundSync(context: Context) {
            WorkManager.getInstance(context).cancelUniqueWork(WORK_NAME_BACKGROUND)
            Log.d(TAG, "Background sync cancelled")
        }
        
        /**
         * Cancel all sync work
         */
        fun cancelAllSync(context: Context) {
            WorkManager.getInstance(context).cancelAllWorkByTag("sync")
            Log.d(TAG, "All sync work cancelled")
        }
        
        /**
         * Trigger immediate one-time sync
         */
        fun triggerImmediateSync(context: Context) {
            val constraints = Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build()
            
            val syncRequest = OneTimeWorkRequestBuilder<BackgroundSyncWorker>()
                .setConstraints(constraints)
                .addTag("sync")
                .addTag("immediate")
                .build()
            
            WorkManager.getInstance(context).enqueue(syncRequest)
            Log.d(TAG, "Immediate sync triggered")
        }
    }
    
    /**
     * Perform sync work
     */
    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        try {
            Log.d(TAG, "Starting sync work (attempt ${runAttemptCount + 1})")
            
            // Perform full sync
            val syncResult = syncRepository.performFullSync()
            
            if (syncResult.isSuccess) {
                val summary = syncResult.getOrThrow()
                
                Log.d(TAG, "Sync completed successfully:")
                Log.d(TAG, "  Cart: ${summary.cartSynced} (${summary.cartAction})")
                Log.d(TAG, "  Orders: ${summary.ordersSynced} (${summary.ordersAction})")
                Log.d(TAG, "  Profile: ${summary.profileSynced} (${summary.profileAction})")
                
                if (summary.errors.isNotEmpty()) {
                    Log.w(TAG, "Sync completed with ${summary.errors.size} errors:")
                    summary.errors.forEach { error ->
                        Log.w(TAG, "  - $error")
                    }
                }
                
                // Return success even if some entities failed to sync
                Result.success()
            } else {
                val exception = syncResult.exceptionOrNull()
                Log.e(TAG, "Sync failed: ${exception?.message}", exception)
                
                // Check if we should retry
                if (exception?.message?.contains("No network connectivity") == true) {
                    // Network issue - let WorkManager retry with backoff
                    Result.retry()
                } else if (runAttemptCount < 3) {
                    // Other failures - retry up to 3 times
                    Result.retry()
                } else {
                    // Max retries reached - give up for this cycle
                    Log.e(TAG, "Max retries reached, giving up for this sync cycle")
                    Result.failure()
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Unexpected error during sync", e)
            
            // Retry on unexpected errors
            if (runAttemptCount < 3) {
                Result.retry()
            } else {
                Result.failure()
            }
        }
    }
    
}

/**
 * Custom WorkerFactory for dependency injection with Hilt
 */
class SyncWorkerFactory @Inject constructor(
    private val syncRepository: SyncRepository
) : WorkerFactory() {
    
    override fun createWorker(
        appContext: Context,
        workerClassName: String,
        workerParameters: WorkerParameters
    ): ListenableWorker? {
        return when (workerClassName) {
            BackgroundSyncWorker::class.java.name -> {
                BackgroundSyncWorker(appContext, workerParameters, syncRepository)
            }
            else -> null
        }
    }
}
