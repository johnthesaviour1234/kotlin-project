package com.grocery.customer.data.remote.dto

import com.google.gson.annotations.SerializedName

data class ApiResponse<T>(
    @SerializedName("success") val success: Boolean,
    @SerializedName("data") val data: T?,
    @SerializedName("message") val message: String? = null,
    @SerializedName("error") val error: String? = null,
    @SerializedName("validation_errors") val validationErrors: List<ValidationError>? = null,
    @SerializedName("timestamp") val timestamp: String? = null
)

data class ValidationError(
    @SerializedName("field") val field: String?,
    @SerializedName("message") val message: String
)