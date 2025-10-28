package com.grocery.delivery.utils

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.activity.result.ActivityResultLauncher
import androidx.core.content.ContextCompat

/**
 * Manages location permissions for the app
 */
object LocationPermissionManager {

    private const val FINE_LOCATION = Manifest.permission.ACCESS_FINE_LOCATION
    private const val COARSE_LOCATION = Manifest.permission.ACCESS_COARSE_LOCATION
    private const val BACKGROUND_LOCATION = Manifest.permission.ACCESS_BACKGROUND_LOCATION

    /**
     * Check if fine location permission is granted
     */
    fun hasFineLocationPermission(context: Context): Boolean {
        return ContextCompat.checkSelfPermission(
            context,
            FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
    }

    /**
     * Check if background location permission is granted (Android 10+)
     */
    fun hasBackgroundLocationPermission(context: Context): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            ContextCompat.checkSelfPermission(
                context,
                BACKGROUND_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
        } else {
            // Not needed for Android 9 and below
            true
        }
    }

    /**
     * Check if all required location permissions are granted
     */
    fun hasAllLocationPermissions(context: Context): Boolean {
        return hasFineLocationPermission(context) && hasBackgroundLocationPermission(context)
    }

    /**
     * Get required location permissions for current Android version
     */
    fun getRequiredPermissions(): Array<String> {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            // Android 10+: Request foreground first, background later
            arrayOf(FINE_LOCATION, COARSE_LOCATION)
        } else {
            arrayOf(FINE_LOCATION, COARSE_LOCATION)
        }
    }

    /**
     * Get background location permission (Android 10+)
     * Should be requested AFTER foreground permissions are granted
     */
    fun getBackgroundPermission(): String? {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            BACKGROUND_LOCATION
        } else {
            null
        }
    }

    /**
     * Open app settings for manual permission grant
     */
    fun openAppSettings(activity: Activity) {
        val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
            data = Uri.fromParts("package", activity.packageName, null)
        }
        activity.startActivity(intent)
    }

    /**
     * Check if should show rationale for location permission
     */
    fun shouldShowRationale(activity: Activity): Boolean {
        return activity.shouldShowRequestPermissionRationale(FINE_LOCATION)
    }
    
    /**
     * Check if should show rationale for background location permission
     */
    fun shouldShowBackgroundRationale(activity: Activity): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            activity.shouldShowRequestPermissionRationale(BACKGROUND_LOCATION)
        } else {
            false
        }
    }
    
    /**
     * Check if basic foreground location permissions are granted
     */
    fun hasLocationPermissions(context: Context): Boolean {
        return ContextCompat.checkSelfPermission(
            context,
            FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED &&
        ContextCompat.checkSelfPermission(
            context,
            COARSE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
    }
}
