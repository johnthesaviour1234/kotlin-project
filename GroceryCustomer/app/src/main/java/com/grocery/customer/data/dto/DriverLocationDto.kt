package com.grocery.customer.data.dto

import com.google.gson.annotations.SerializedName

/**
 * Response from GET /api/orders/{orderId}/driver-location
 */
data class DriverLocationResponse(
    @SerializedName("has_location")
    val hasLocation: Boolean,
    
    @SerializedName("location")
    val location: DriverLocation?,
    
    @SerializedName("driver")
    val driver: DriverInfo?,
    
    @SerializedName("delivery_status")
    val deliveryStatus: String?,
    
    @SerializedName("estimated_minutes_remaining")
    val estimatedMinutesRemaining: Int?,
    
    @SerializedName("assignment_status")
    val assignmentStatus: String?,
    
    @SerializedName("message")
    val message: String?
)

data class DriverLocation(
    @SerializedName("latitude")
    val latitude: Double,
    
    @SerializedName("longitude")
    val longitude: Double,
    
    @SerializedName("accuracy")
    val accuracy: Double?,
    
    @SerializedName("speed")
    val speed: Double?,
    
    @SerializedName("heading")
    val heading: Double?,
    
    @SerializedName("timestamp")
    val timestamp: String,
    
    @SerializedName("updated_at")
    val updatedAt: String
)

data class DriverInfo(
    @SerializedName("name")
    val name: String,
    
    @SerializedName("phone")
    val phone: String?
)
