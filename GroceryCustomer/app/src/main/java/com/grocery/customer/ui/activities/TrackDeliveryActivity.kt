package com.grocery.customer.ui.activities

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.preference.PreferenceManager
import android.view.View
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import com.grocery.customer.R
import com.grocery.customer.data.dto.DriverLocationResponse
import com.grocery.customer.data.remote.ApiService
import com.grocery.customer.data.remote.dto.ApiResponse
import com.grocery.customer.databinding.ActivityTrackDeliveryBinding
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import org.osmdroid.config.Configuration
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker
import retrofit2.Response
import javax.inject.Inject

@AndroidEntryPoint
class TrackDeliveryActivity : AppCompatActivity() {

    private lateinit var binding: ActivityTrackDeliveryBinding
    private var mapView: MapView? = null
    private var driverMarker: Marker? = null
    private var orderId: String? = null
    private var orderNumber: String? = null
    private var driverPhone: String? = null
    
    @Inject
    lateinit var apiService: ApiService
    
    private val handler = Handler(Looper.getMainLooper())
    private val locationUpdateInterval = 15000L // 15 seconds
    private var isTracking = false

    companion object {
        const val EXTRA_ORDER_ID = "order_id"
        const val EXTRA_ORDER_NUMBER = "order_number"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Configure osmdroid
        Configuration.getInstance().load(
            this,
            PreferenceManager.getDefaultSharedPreferences(this)
        )
        
        binding = ActivityTrackDeliveryBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Get order info from intent
        orderId = intent.getStringExtra(EXTRA_ORDER_ID)
        orderNumber = intent.getStringExtra(EXTRA_ORDER_NUMBER)

        if (orderId == null) {
            Toast.makeText(this, "Order ID not provided", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        setupUI()
        setupMap()
        startLocationTracking()
    }

    private fun setupUI() {
        binding.toolbar.setNavigationOnClickListener {
            finish()
        }

        binding.textViewOrderNumber.text = orderNumber ?: "Order"

        binding.buttonCallDriver.setOnClickListener {
            driverPhone?.let { phone ->
                val intent = Intent(Intent.ACTION_DIAL).apply {
                    data = Uri.parse("tel:$phone")
                }
                startActivity(intent)
            } ?: run {
                Toast.makeText(this, "Driver phone not available", Toast.LENGTH_SHORT).show()
            }
        }

        binding.buttonRetry.setOnClickListener {
            fetchDriverLocation()
        }
    }

    private fun setupMap() {
        mapView = binding.mapView
        
        // Configure map
        mapView?.apply {
            setTileSource(TileSourceFactory.MAPNIK)
            setMultiTouchControls(true)
            controller.setZoom(15.0)
        }
    }

    private fun startLocationTracking() {
        isTracking = true
        showLoading()
        fetchDriverLocation()
    }

    private fun fetchDriverLocation() {
        lifecycleScope.launch {
            try {
                val response: Response<ApiResponse<DriverLocationResponse>> = 
                    apiService.getDriverLocation(orderId!!)

                if (response.isSuccessful && response.body()?.success == true) {
                    val data = response.body()?.data
                    
                    if (data?.hasLocation == true && data.location != null) {
                        // Update map with driver location
                        updateDriverLocation(
                            lat = data.location.latitude,
                            lng = data.location.longitude
                        )
                        
                        // Update UI
                        updateDeliveryInfo(data)
                        showContent()
                        
                        // Schedule next update
                        scheduleNextUpdate()
                    } else {
                        // Check if delivery is completed/not active based on status or message
                        val message = data?.message ?: ""
                        val deliveryStatus = data?.deliveryStatus ?: ""
                        
                        if (deliveryStatus == "completed" || deliveryStatus == "delivered" ||
                            message.contains("completed", ignoreCase = true) ||
                            message.contains("delivered", ignoreCase = true)) {
                            // Delivery completed, navigate back to order details
                            navigateToOrderDetails()
                        } else {
                            // Location not yet available - delivery hasn't started
                            showError(
                                message = "Driver location not yet available",
                                details = message.ifEmpty { "Delivery hasn't started yet" }
                            )
                        }
                    }
                } else {
                    // Check if delivery is completed/not in progress
                    val errorMsg = response.body()?.error ?: ""
                    val errorData = response.body()?.data
                    
                    // Check if error response contains delivery status indicating completion
                    val deliveryStatus = when (errorData) {
                        is com.grocery.customer.data.dto.DriverLocationResponse -> errorData.deliveryStatus
                        else -> null
                    }
                    
                    if (response.code() == 400 && errorMsg.contains("not currently in progress", ignoreCase = true)) {
                        // Delivery is completed, redirect to order details
                        navigateToOrderDetails()
                    } else if (deliveryStatus == "completed" || deliveryStatus == "delivered") {
                        // Delivery status indicates completion, redirect to order details
                        navigateToOrderDetails()
                    } else if (response.code() == 404 && errorMsg.contains("No delivery assignment", ignoreCase = true)) {
                        // No delivery assignment found, redirect to order details
                        navigateToOrderDetails()
                    } else {
                        showError(
                            message = "Unable to fetch location",
                            details = errorMsg.ifEmpty { response.message() ?: "Unknown error" }
                        )
                    }
                }
            } catch (e: Exception) {
                showError(
                    message = "Network error",
                    details = e.localizedMessage ?: "Please check your connection"
                )
            }
        }
    }
    
    private fun navigateToOrderDetails() {
        Toast.makeText(this, "Delivery completed! Check your order details.", Toast.LENGTH_LONG).show()
        
        // Go back to order details (previous screen)
        finish()
    }

    private fun updateDriverLocation(lat: Double, lng: Double) {
        val driverPosition = GeoPoint(lat, lng)

        // Remove old marker if exists
        driverMarker?.let {
            mapView?.overlays?.remove(it)
        }

        // Create new marker
        driverMarker = Marker(mapView).apply {
            position = driverPosition
            title = "Delivery Driver"
            snippet = "On the way to you"
            setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
            icon = ContextCompat.getDrawable(this@TrackDeliveryActivity, R.drawable.ic_arrow_forward)
        }
        
        mapView?.overlays?.add(driverMarker)

        // Move camera to driver location
        mapView?.controller?.animateTo(driverPosition)
        mapView?.invalidate()
    }

    private fun updateDeliveryInfo(data: DriverLocationResponse) {
        // Driver name
        binding.textViewDriverName.text = data.driver?.name ?: "Driver"
        driverPhone = data.driver?.phone

        // Delivery status
        binding.textViewDeliveryStatus.text = when (data.deliveryStatus) {
            "accepted" -> "Preparing for delivery"
            "in_transit" -> "On the way"
            "arrived" -> "Arrived at location"
            else -> data.deliveryStatus?.replaceFirstChar { it.uppercase() } ?: "In Progress"
        }

        // ETA
        val eta = data.estimatedMinutesRemaining
        if (eta != null && eta > 0) {
            binding.textViewETA.text = "$eta minutes"
        } else {
            binding.textViewETA.text = "Arriving soon"
        }
    }

    private fun scheduleNextUpdate() {
        if (isTracking) {
            handler.postDelayed({
                fetchDriverLocation()
            }, locationUpdateInterval)
        }
    }

    private fun showLoading() {
        binding.loadingOverlay.visibility = View.VISIBLE
        binding.cardDeliveryInfo.visibility = View.GONE
        binding.errorLayout.visibility = View.GONE
    }

    private fun showContent() {
        binding.loadingOverlay.visibility = View.GONE
        binding.cardDeliveryInfo.visibility = View.VISIBLE
        binding.errorLayout.visibility = View.GONE
    }

    private fun showError(message: String, details: String) {
        binding.loadingOverlay.visibility = View.GONE
        binding.cardDeliveryInfo.visibility = View.GONE
        binding.errorLayout.visibility = View.VISIBLE
        binding.textViewError.text = message
        binding.textViewErrorDetails.text = details
    }

    override fun onDestroy() {
        super.onDestroy()
        isTracking = false
        handler.removeCallbacksAndMessages(null)
        mapView?.onDetach()
    }

    override fun onPause() {
        super.onPause()
        isTracking = false
        handler.removeCallbacksAndMessages(null)
        mapView?.onPause()
    }

    override fun onResume() {
        super.onResume()
        mapView?.onResume()
        if (!isTracking) {
            startLocationTracking()
        }
    }
}
