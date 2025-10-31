package com.grocery.delivery.services

import android.app.*
import android.content.Context
import android.content.Intent
import android.location.Location
import android.os.Build
import android.os.IBinder
import android.os.Looper
import android.util.Log
import androidx.core.app.NotificationCompat
import com.google.android.gms.location.*
import com.grocery.delivery.R
import com.grocery.delivery.data.api.DeliveryApiService
import com.grocery.delivery.data.dto.ApiResponse
import com.grocery.delivery.data.dto.LocationUpdateRequest
import com.grocery.delivery.data.dto.LocationUpdateResponse
import com.grocery.delivery.data.local.PreferencesManager
import com.grocery.delivery.ui.activities.ActiveDeliveryActivity
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.*
import javax.inject.Inject

/**
 * Foreground service for tracking delivery location
 * Sends GPS updates to backend every 15 seconds
 */
@AndroidEntryPoint
class LocationTrackingService : Service() {

    @Inject
    lateinit var apiService: DeliveryApiService

    @Inject
    lateinit var preferencesManager: PreferencesManager

    private var fusedLocationClient: FusedLocationProviderClient? = null
    private var locationCallback: LocationCallback? = null
    private val serviceScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    
    private var currentOrderId: String? = null
    private var isTracking = false

    companion object {
        private const val TAG = "LocationTrackingService"
        private const val NOTIFICATION_ID = 1001
        private const val CHANNEL_ID = "location_tracking"
        private const val CHANNEL_NAME = "Delivery Location Tracking"
        
        // Location update configuration
        private const val UPDATE_INTERVAL_MS = 15_000L // 15 seconds
        private const val FASTEST_INTERVAL_MS = 10_000L // 10 seconds
        private const val MIN_ACCURACY_METERS = 50f

        const val ACTION_START_TRACKING = "START_TRACKING"
        const val ACTION_STOP_TRACKING = "STOP_TRACKING"
        const val EXTRA_ORDER_ID = "order_id"
        const val EXTRA_DELIVERY_ID = "order_id" // Alias for consistency

        fun startTracking(context: Context, orderId: String) {
            val intent = Intent(context, LocationTrackingService::class.java).apply {
                action = ACTION_START_TRACKING
                putExtra(EXTRA_ORDER_ID, orderId)
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }

        fun stopTracking(context: Context) {
            val intent = Intent(context, LocationTrackingService::class.java).apply {
                action = ACTION_STOP_TRACKING
            }
            context.startService(intent)
        }
    }

    override fun onCreate() {
        super.onCreate()
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_START_TRACKING -> {
                currentOrderId = intent.getStringExtra(EXTRA_ORDER_ID)
                startForegroundTracking()
            }
            ACTION_STOP_TRACKING -> {
                stopTracking()
            }
        }
        return START_STICKY
    }

    private fun startForegroundTracking() {
        if (isTracking) {
            Log.d(TAG, "Already tracking location")
            return
        }

        startForeground(NOTIFICATION_ID, createNotification("Starting GPS tracking..."))
        requestLocationUpdates()
        isTracking = true
        Log.d(TAG, "Started location tracking for order: $currentOrderId")
    }

    private fun requestLocationUpdates() {
        try {
            val locationRequest = LocationRequest.Builder(
                Priority.PRIORITY_HIGH_ACCURACY,
                UPDATE_INTERVAL_MS
            ).apply {
                setMinUpdateIntervalMillis(FASTEST_INTERVAL_MS)
                setWaitForAccurateLocation(true)
            }.build()

            locationCallback = object : LocationCallback() {
                override fun onLocationResult(locationResult: LocationResult) {
                    super.onLocationResult(locationResult)
                    locationResult.lastLocation?.let { location ->
                        handleLocationUpdate(location)
                    }
                }

                override fun onLocationAvailability(availability: LocationAvailability) {
                    super.onLocationAvailability(availability)
                    if (!availability.isLocationAvailable) {
                        updateNotification("GPS signal lost. Searching...")
                    }
                }
            }

            fusedLocationClient?.requestLocationUpdates(
                locationRequest,
                locationCallback!!,
                Looper.getMainLooper()
            )
        } catch (e: SecurityException) {
            Log.e(TAG, "Location permission not granted", e)
            stopSelf()
        }
    }

    private fun handleLocationUpdate(location: Location) {
        // Filter out low accuracy locations
        if (location.accuracy > MIN_ACCURACY_METERS) {
            Log.d(TAG, "Location accuracy too low: ${location.accuracy}m")
            updateNotification("GPS accuracy: ${location.accuracy.toInt()}m (improving...)")
            return
        }

        // Update notification with current location
        updateNotification("Location: ${String.format("%.4f", location.latitude)}, ${String.format("%.4f", location.longitude)} (${location.accuracy.toInt()}m)")

        // Send to backend
        sendLocationToBackend(location)
    }

    private fun sendLocationToBackend(location: Location) {
        serviceScope.launch {
            try {
                val request = LocationUpdateRequest(
                    latitude = location.latitude,
                    longitude = location.longitude,
                    accuracy = location.accuracy.toDouble(),
                    speed = if (location.hasSpeed()) location.speed.toDouble() else null,
                    heading = if (location.hasBearing()) location.bearing.toDouble() else null,
                    orderId = currentOrderId
                )

                // AuthInterceptor will add the token automatically
                val response = apiService.updateLocation(request)

                if (response.isSuccessful) {
                    Log.d(TAG, "Location updated successfully")
                } else {
                    Log.e(TAG, "Failed to update location: ${response.code()}")
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error sending location", e)
                // Location will be retried on next update
            }
        }
    }

    private fun stopTracking() {
        isTracking = false
        locationCallback?.let {
            fusedLocationClient?.removeLocationUpdates(it)
        }
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
        Log.d(TAG, "Stopped location tracking")
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                CHANNEL_NAME,
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Shows when tracking delivery location"
                setShowBadge(false)
            }

            val notificationManager = getSystemService(NotificationManager::class.java)
            notificationManager.createNotificationChannel(channel)
        }
    }

    private fun createNotification(content: String): Notification {
        val intent = Intent(this, ActiveDeliveryActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Tracking Delivery Location")
            .setContentText(content)
            .setSmallIcon(R.drawable.ic_location)
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setCategory(NotificationCompat.CATEGORY_SERVICE)
            .build()
    }

    private fun updateNotification(content: String) {
        val notification = createNotification(content)
        val notificationManager = getSystemService(NotificationManager::class.java)
        notificationManager.notify(NOTIFICATION_ID, notification)
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        super.onDestroy()
        stopTracking()
        serviceScope.cancel()
    }
}
