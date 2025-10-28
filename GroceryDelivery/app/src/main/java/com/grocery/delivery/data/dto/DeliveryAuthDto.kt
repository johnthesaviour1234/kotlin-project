package com.grocery.delivery.data.dto

import com.google.gson.annotations.SerializedName

/**
 * Authentication DTOs for delivery driver login
 */

data class DeliveryLoginRequest(
    @SerializedName("email")
    val email: String,
    
    @SerializedName("password")
    val password: String,
    
    @SerializedName("user_type")
    val userType: String = "delivery"
)

data class DeliveryLoginResponse(
    @SerializedName("success")
    val success: Boolean,
    
    @SerializedName("message")
    val message: String?,
    
    @SerializedName("data")
    val data: DeliveryAuthData?
)

data class DeliveryAuthData(
    @SerializedName("user")
    val user: DeliveryUser,
    
    @SerializedName("tokens")
    val tokens: DeliveryTokens
)

data class DeliveryTokens(
    @SerializedName("access_token")
    val accessToken: String,
    
    @SerializedName("refresh_token")
    val refreshToken: String?,
    
    @SerializedName("expires_at")
    val expiresAt: Long?,
    
    @SerializedName("expires_in")
    val expiresIn: Int?
)

data class DeliveryUser(
    @SerializedName("id")
    val id: String,
    
    @SerializedName("email")
    val email: String,
    
    @SerializedName("profile")
    val profile: DeliveryProfile?
) {
    val fullName: String?
        get() = profile?.fullName
    
    val phone: String?
        get() = profile?.phone
    
    val userType: String?
        get() = profile?.userType
}

data class DeliveryProfile(
    @SerializedName("full_name")
    val fullName: String?,
    
    @SerializedName("phone")
    val phone: String?,
    
    @SerializedName("user_type")
    val userType: String?,
    
    @SerializedName("vehicle_type")
    val vehicleType: String?,
    
    @SerializedName("vehicle_number")
    val vehicleNumber: String?,
    
    @SerializedName("is_available")
    val isAvailable: Boolean?,
    
    @SerializedName("is_active")
    val isActive: Boolean?
)

data class DeliveryRegisterRequest(
    @SerializedName("email")
    val email: String,
    
    @SerializedName("password")
    val password: String,
    
    @SerializedName("full_name")
    val fullName: String,
    
    @SerializedName("phone")
    val phone: String,
    
    @SerializedName("vehicle_type")
    val vehicleType: String,
    
    @SerializedName("vehicle_number")
    val vehicleNumber: String,
    
    @SerializedName("user_type")
    val userType: String = "delivery"
)

data class DeliveryRegisterResponse(
    @SerializedName("success")
    val success: Boolean,
    
    @SerializedName("message")
    val message: String?,
    
    @SerializedName("data")
    val data: DeliveryAuthData?
)
