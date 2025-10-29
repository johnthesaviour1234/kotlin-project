package com.grocery.delivery.ui.activities

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.AlertDialog
import com.grocery.delivery.data.dto.DeliveryAssignment
import com.grocery.delivery.data.dto.OrderStatus
import com.grocery.delivery.databinding.ActivityActiveDeliveryBinding
import com.grocery.delivery.services.LocationTrackingService
import com.grocery.delivery.ui.viewmodels.ActiveDeliveryViewModel
import com.grocery.delivery.utils.LocationPermissionManager
import com.grocery.delivery.utils.Resource
import dagger.hilt.android.AndroidEntryPoint
import java.text.NumberFormat
import java.util.Locale

/**
 * Activity displaying the active delivery details and actions
 */
@AndroidEntryPoint
class ActiveDeliveryActivity : BaseActivity<ActivityActiveDeliveryBinding>() {

    private val viewModel: ActiveDeliveryViewModel by viewModels()
    private var currentAssignment: DeliveryAssignment? = null
    private var isLocationTrackingActive = false
    
    // Permission launcher for location permissions
    private val locationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val fineLocationGranted = permissions[Manifest.permission.ACCESS_FINE_LOCATION] == true
        val coarseLocationGranted = permissions[Manifest.permission.ACCESS_COARSE_LOCATION] == true
        
        if (fineLocationGranted && coarseLocationGranted) {
            // Permissions granted, check if background permission needed
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q &&
                !LocationPermissionManager.hasBackgroundLocationPermission(this)) {
                requestBackgroundLocationPermission()
            } else {
                startLocationTracking()
            }
        } else {
            showLocationPermissionDeniedDialog()
        }
    }
    
    private val backgroundLocationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            startLocationTracking()
        } else {
            // Can still track but only in foreground
            Toast.makeText(
                this,
                "Background location denied. Tracking will stop when app is closed.",
                Toast.LENGTH_LONG
            ).show()
            startLocationTracking()
        }
    }

    override fun inflateViewBinding(): ActivityActiveDeliveryBinding {
        return ActivityActiveDeliveryBinding.inflate(layoutInflater)
    }

    override fun setupUI() {
        setupToolbar()
        
        // Get assignment from intent
        @Suppress("DEPRECATION")
        val assignment = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
            intent.getParcelableExtra(EXTRA_ASSIGNMENT, DeliveryAssignment::class.java)
        } else {
            intent.getParcelableExtra(EXTRA_ASSIGNMENT)
        }
        
        if (assignment != null) {
            viewModel.setActiveDelivery(assignment)
        } else {
            Toast.makeText(this, "No delivery assignment found", Toast.LENGTH_SHORT).show()
            finish()
        }
    }

    override fun setupObservers() {
        viewModel.deliveryState.observe(this) { resource ->
            when (resource) {
                is Resource.Loading -> {
                    showLoading()
                }
                is Resource.Success -> {
                    hideLoading()
                    resource.data?.let { assignment ->
                        currentAssignment = assignment
                        displayDeliveryInfo(assignment)
                        updateUIForStatus(assignment.status)
                    }
                }
                is Resource.Error -> {
                    hideLoading()
                    showError(resource.message ?: "Failed to load delivery")
                    Toast.makeText(this, resource.message, Toast.LENGTH_SHORT).show()
                }
            }
        }
        
        viewModel.statusUpdateState.observe(this) { resource ->
            when (resource) {
                is Resource.Loading -> {
                    showLoading()
                }
                is Resource.Success -> {
                    hideLoading()
                    Toast.makeText(this, resource.data ?: "Status updated", Toast.LENGTH_SHORT).show()
                    viewModel.resetStatusUpdateState()
                    
                    // If delivery is completed, go back to available orders
                    if (viewModel.getCurrentStatus() == "completed") {
                        finish()
                    }
                }
                is Resource.Error -> {
                    hideLoading()
                    showError(resource.message ?: "Failed to update status")
                    Toast.makeText(this, resource.message, Toast.LENGTH_SHORT).show()
                    viewModel.resetStatusUpdateState()
                }
                null -> {
                    // Reset state
                }
            }
        }
    }

    private fun setupToolbar() {
        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        binding.toolbar.setNavigationOnClickListener {
            finish()
        }
    }

    private fun displayDeliveryInfo(assignment: DeliveryAssignment) {
        val order = assignment.orders ?: return
        
        // Order info
        binding.tvOrderNumber.text = order.orderNumber
        
        val currencyFormatter = NumberFormat.getCurrencyInstance(Locale.US)
        binding.tvTotalAmount.text = currencyFormatter.format(order.totalAmount)
        
        val estimatedMinutes = assignment.estimatedDeliveryMinutes ?: 30
        binding.tvEstimatedTime.text = "$estimatedMinutes mins"
        
        // Customer info
        binding.tvCustomerName.text = order.getCustomerName() ?: "N/A"
        binding.tvCustomerPhone.text = order.getCustomerPhone() ?: "N/A"
        binding.tvDeliveryAddress.text = order.getFormattedAddress()
        
        // Notes
        if (!order.notes.isNullOrBlank()) {
            binding.cardNotes.visibility = View.VISIBLE
            binding.tvNotes.text = order.notes
        } else {
            binding.cardNotes.visibility = View.GONE
        }
        
        setupActionButtons(order.getCustomerPhone(), order.getFormattedAddress())
    }

    private fun updateUIForStatus(status: String) {
        binding.tvStatus.text = when (status) {
            OrderStatus.ACCEPTED -> "Accepted"
            OrderStatus.IN_TRANSIT -> "On the Way"
            "arrived" -> "Arrived"
            "completed" -> "Completed"
            else -> status.replaceFirstChar { it.uppercase() }
        }
        
        // Show/hide buttons based on status
        when (status) {
            OrderStatus.ACCEPTED -> {
                // Status: Accepted - Show "Start Delivery" button
                binding.btnStartDelivery.visibility = View.VISIBLE
                binding.btnArrived.visibility = View.GONE
                binding.btnMarkDelivered.visibility = View.GONE
                // Stop location tracking if previously active
                stopLocationTracking()
            }
            OrderStatus.IN_TRANSIT -> {
                // Status: In Transit - Show "I've Arrived" button
                binding.btnStartDelivery.visibility = View.GONE
                binding.btnArrived.visibility = View.VISIBLE
                binding.btnMarkDelivered.visibility = View.GONE
                // Start location tracking when delivery begins
                checkAndRequestLocationPermissions()
            }
            "arrived" -> {
                // Status: Arrived - Show "Complete Delivery" button
                binding.btnStartDelivery.visibility = View.GONE
                binding.btnArrived.visibility = View.GONE
                binding.btnMarkDelivered.visibility = View.VISIBLE
                // Keep location tracking active until delivery is completed
            }
            else -> {
                // Completed or other status - Hide all buttons and stop tracking
                binding.btnStartDelivery.visibility = View.GONE
                binding.btnArrived.visibility = View.GONE
                binding.btnMarkDelivered.visibility = View.GONE
                stopLocationTracking()
            }
        }
    }

    private fun setupActionButtons(phone: String?, address: String) {
        // Call button
        binding.btnCall.setOnClickListener {
            if (!phone.isNullOrBlank()) {
                val intent = Intent(Intent.ACTION_DIAL).apply {
                    data = Uri.parse("tel:$phone")
                }
                startActivity(intent)
            } else {
                Toast.makeText(this, "Phone number not available", Toast.LENGTH_SHORT).show()
            }
        }
        
        // Navigate button
        binding.btnNavigate.setOnClickListener {
            if (address.isNotBlank() && address != "Address not available") {
                val intent = Intent(Intent.ACTION_VIEW).apply {
                    data = Uri.parse("google.navigation:q=$address")
                }
                
                if (intent.resolveActivity(packageManager) != null) {
                    startActivity(intent)
                } else {
                    // Fallback to browser maps
                    val fallbackIntent = Intent(Intent.ACTION_VIEW).apply {
                        data = Uri.parse("https://www.google.com/maps/search/?api=1&query=$address")
                    }
                    startActivity(fallbackIntent)
                }
            } else {
                Toast.makeText(this, "Address not available", Toast.LENGTH_SHORT).show()
            }
        }
        
        // Start delivery button
        binding.btnStartDelivery.setOnClickListener {
            viewModel.startDelivery()
        }
        
        // Arrived at location button
        binding.btnArrived.setOnClickListener {
            viewModel.markArrived()
        }
        
        // Mark as delivered button
        binding.btnMarkDelivered.setOnClickListener {
            // TODO: Show dialog for delivery confirmation with optional notes/photo
            viewModel.markAsDelivered()
        }
    }

    override fun showLoading() {
        binding.progressBar.visibility = View.VISIBLE
    }

    override fun hideLoading() {
        binding.progressBar.visibility = View.GONE
    }
    
    override fun onDestroy() {
        super.onDestroy()
        stopLocationTracking()
    }
    
    private fun checkAndRequestLocationPermissions() {
        if (LocationPermissionManager.hasLocationPermissions(this)) {
            // Already have permissions, check background if needed
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q &&
                !LocationPermissionManager.hasBackgroundLocationPermission(this)) {
                requestBackgroundLocationPermission()
            } else {
                startLocationTracking()
            }
        } else {
            // Check if we should show rationale
            if (LocationPermissionManager.shouldShowRationale(this)) {
                showLocationPermissionRationale()
            } else {
                requestLocationPermissions()
            }
        }
    }
    
    private fun requestLocationPermissions() {
        locationPermissionLauncher.launch(
            arrayOf(
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.ACCESS_COARSE_LOCATION
            )
        )
    }
    
    private fun requestBackgroundLocationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            if (LocationPermissionManager.shouldShowBackgroundRationale(this)) {
                showBackgroundLocationRationale()
            } else {
                backgroundLocationPermissionLauncher.launch(
                    Manifest.permission.ACCESS_BACKGROUND_LOCATION
                )
            }
        }
    }
    
    private fun showLocationPermissionRationale() {
        AlertDialog.Builder(this)
            .setTitle("Location Permission Required")
            .setMessage("This app needs your location to track deliveries and provide accurate ETA to customers.")
            .setPositiveButton("Grant") { _, _ ->
                requestLocationPermissions()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }
    
    private fun showBackgroundLocationRationale() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            AlertDialog.Builder(this)
                .setTitle("Background Location Permission")
                .setMessage("For continuous delivery tracking, please allow location access all the time in the next screen.")
                .setPositiveButton("Continue") { _, _ ->
                    backgroundLocationPermissionLauncher.launch(
                        Manifest.permission.ACCESS_BACKGROUND_LOCATION
                    )
                }
                .setNegativeButton("Skip") { _, _ ->
                    startLocationTracking()
                }
                .show()
        }
    }
    
    private fun showLocationPermissionDeniedDialog() {
        AlertDialog.Builder(this)
            .setTitle("Location Permission Denied")
            .setMessage("Location permission is required for delivery tracking. Please enable it in app settings.")
            .setPositiveButton("Open Settings") { _, _ ->
                LocationPermissionManager.openAppSettings(this)
            }
            .setNegativeButton("Cancel", null)
            .show()
    }
    
    private fun startLocationTracking() {
        if (isLocationTrackingActive) return
        
        currentAssignment?.let { assignment ->
            // Pass the actual order ID from the orders table, not the assignment ID
            val orderId = assignment.orderId ?: assignment.orders?.id
            val intent = Intent(this, LocationTrackingService::class.java).apply {
                action = LocationTrackingService.ACTION_START_TRACKING
                putExtra(LocationTrackingService.EXTRA_ORDER_ID, orderId)
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                startForegroundService(intent)
            } else {
                startService(intent)
            }
            isLocationTrackingActive = true
            updateLocationTrackingUI(true)
        }
    }
    
    private fun stopLocationTracking() {
        if (!isLocationTrackingActive) return
        
        val intent = Intent(this, LocationTrackingService::class.java).apply {
            action = LocationTrackingService.ACTION_STOP_TRACKING
        }
        startService(intent)
        isLocationTrackingActive = false
        updateLocationTrackingUI(false)
    }
    
    private fun updateLocationTrackingUI(isActive: Boolean) {
        // Update UI to show location tracking status
        if (isActive) {
            supportActionBar?.subtitle = "Location Tracking Active"
        } else {
            supportActionBar?.subtitle = null
        }
    }

    companion object {
        const val EXTRA_ASSIGNMENT_ID = "assignment_id"
        const val EXTRA_ASSIGNMENT_STATUS = "assignment_status"
        const val EXTRA_ASSIGNMENT = "assignment"
    }
}
