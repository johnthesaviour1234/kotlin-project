package com.grocery.delivery.data.dto

import com.google.gson.annotations.SerializedName

/**
 * Generic wrapper for API responses
 */
data class ApiResponse<T>(
    @SerializedName("success")
    val success: Boolean,
    
    @SerializedName("data")
    val data: T?,
    
    @SerializedName("message")
    val message: String?,
    
    @SerializedName("timestamp")
    val timestamp: String?
)
