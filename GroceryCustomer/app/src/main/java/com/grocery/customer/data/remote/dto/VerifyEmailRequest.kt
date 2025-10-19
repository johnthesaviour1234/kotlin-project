package com.grocery.customer.data.remote.dto

import com.google.gson.annotations.SerializedName

data class VerifyEmailRequest(
    // Support both token (body) and token_hash (server)
    @SerializedName("token") val token: String,
    @SerializedName("email") val email: String? = null
)