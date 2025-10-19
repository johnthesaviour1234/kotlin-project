package com.grocery.customer.data.remote.dto

import com.google.gson.annotations.SerializedName

data class ResendVerificationRequest(
    @SerializedName("email") val email: String
)