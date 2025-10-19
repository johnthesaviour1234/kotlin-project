package com.grocery.customer.data.remote.dto

import com.google.gson.annotations.SerializedName

data class ResetPasswordRequest(
    @SerializedName("token") val token: String,
    @SerializedName("new_password") val newPassword: String
)