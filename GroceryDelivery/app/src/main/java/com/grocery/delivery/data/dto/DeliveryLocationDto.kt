package com.grocery.delivery.data.dto

import com.google.gson.annotations.SerializedName

/**
 * Location DTOs for tracking delivery driver location
 */

data class UpdateLocationRequest(
    @SerializedName("latitude")
    val latitude: Double,
    
    @SerializedName("longitude")
    val longitude: Double,
    
    @SerializedName("order_id")
    val orderId: String? = null,
    
    @SerializedName("accuracy")
    val accuracy: Double? = null,
    
    @SerializedName("speed")
    val speed: Double? = null,
    
    @SerializedName("heading")
    val heading: Double? = null
)

data class UpdateLocationResponse(
    @SerializedName("success")
    val success: Boolean,
    
    @SerializedName("message")
    val message: String?
)

data class DriverLocation(
    @SerializedName("id")
    val id: String,
    
    @SerializedName("driver_id")
    val driverId: String,
    
    @SerializedName("latitude")
    val latitude: Double,
    
    @SerializedName("longitude")
    val longitude: Double,
    
    @SerializedName("order_id")
    val orderId: String?,
    
    @SerializedName("updated_at")
    val updatedAt: String
)
