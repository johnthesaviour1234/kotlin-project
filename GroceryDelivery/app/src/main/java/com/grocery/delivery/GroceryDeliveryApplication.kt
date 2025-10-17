package com.grocery.delivery

import android.app.Application
import dagger.hilt.android.HiltAndroidApp

/**
 * Main Application class for Grocery Customer app.
 * Annotated with @HiltAndroidApp to enable Hilt dependency injection.
 */
@HiltAndroidApp
class GroceryDeliveryApplication : Application() {

    override fun onCreate() {
        super.onCreate()
        
        // Initialize any global configurations here
        // Timber logging, crash reporting, etc. can be added here
    }
}
