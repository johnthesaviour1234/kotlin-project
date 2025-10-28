package com.grocery.delivery.data.dto

import com.google.gson.annotations.SerializedName

/**
 * Request for updating delivery location
 */
data class LocationUpdateRequest(
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
    
    @SerializedName("order_id")
    val orderId: String?
)

/**
 * Response from location update
 */
data class LocationUpdateResponse(
    @SerializedName("id")
    val id: String,
    
    @SerializedName("delivery_personnel_id")
    val deliveryPersonnelId: String,
    
    @SerializedName("order_id")
    val orderId: String?,
    
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
    
    @SerializedName("created_at")
    val createdAt: String
)
