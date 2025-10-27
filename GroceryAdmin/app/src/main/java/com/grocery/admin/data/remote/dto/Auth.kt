package com.grocery.admin.data.remote.dto

import com.google.gson.annotations.SerializedName

data class LoginRequest(
    @SerializedName("email") val email: String,
    @SerializedName("password") val password: String
)

data class LoginResponse(
    @SerializedName("user") val user: UserDto,
    @SerializedName("tokens") val tokens: AuthTokens,
    @SerializedName("session") val session: SessionData? = null
)
