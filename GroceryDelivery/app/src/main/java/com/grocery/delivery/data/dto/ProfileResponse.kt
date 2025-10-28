package com.grocery.delivery.data.dto

import com.google.gson.annotations.SerializedName

/**
 * Response from GET /api/delivery/profile
 */
data class ProfileResponse(
    @SerializedName("profile")
    val profile: DriverProfile,
    
    @SerializedName("stats")
    val stats: DeliveryStats
)

/**
 * Driver profile information
 */
data class DriverProfile(
    @SerializedName("id")
    val id: String,
    
    @SerializedName("email")
    val email: String,
    
    @SerializedName("full_name")
    val fullName: String,
    
    @SerializedName("phone")
    val phone: String,
    
    @SerializedName("user_type")
    val userType: String,
    
    @SerializedName("is_active")
    val isActive: Boolean,
    
    @SerializedName("avatar_url")
    val avatarUrl: String?,
    
    @SerializedName("preferences")
    val preferences: Map<String, Any>?,
    
    @SerializedName("created_at")
    val createdAt: String,
    
    @SerializedName("updated_at")
    val updatedAt: String
)

/**
 * Delivery statistics for driver
 */
data class DeliveryStats(
    @SerializedName("total_deliveries")
    val totalDeliveries: Int,
    
    @SerializedName("completed_deliveries")
    val completedDeliveries: Int,
    
    @SerializedName("active_deliveries")
    val activeDeliveries: Int
)

/**
 * Request for updating profile
 */
data class ProfileUpdateRequest(
    @SerializedName("full_name")
    val fullName: String? = null,
    
    @SerializedName("phone")
    val phone: String? = null,
    
    @SerializedName("avatar_url")
    val avatarUrl: String? = null,
    
    @SerializedName("preferences")
    val preferences: Map<String, Any>? = null
)

/**
 * Response after profile update
 */
data class ProfileUpdateResponse(
    @SerializedName("profile")
    val profile: DriverProfile,
    
    @SerializedName("message")
    val message: String?
)
