package com.grocery.customer

import android.app.Application
import androidx.work.Configuration
import com.grocery.customer.data.workers.BackgroundSyncWorker
import com.grocery.customer.data.workers.SyncWorkerFactory
import dagger.hilt.android.HiltAndroidApp
import javax.inject.Inject

/**
 * Main Application class for Grocery Customer app.
 * Annotated with @HiltAndroidApp to enable Hilt dependency injection.
 * Implements Configuration.Provider for custom WorkManager configuration.
 */
@HiltAndroidApp
class GroceryCustomerApplication : Application(), Configuration.Provider {

    @Inject
    lateinit var syncWorkerFactory: SyncWorkerFactory

    override fun onCreate() {
        super.onCreate()
        
        // Initialize background sync when app starts
        // This will schedule foreground sync (15 seconds interval)
        BackgroundSyncWorker.scheduleForegroundSync(this)
    }
    
    /**
     * Provide custom WorkManager configuration with dependency injection
     */
    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder()
            .setWorkerFactory(syncWorkerFactory)
            .setMinimumLoggingLevel(android.util.Log.DEBUG)
            .build()
}
