package com.grocery.customer.data.remote.dto

data class ApiResponse<T>(
    val success: Boolean,
    val data: T?,
    val message: String? = null,
    val error: String? = null,
    val timestamp: String? = null
)
