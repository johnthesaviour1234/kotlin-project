package com.grocery.admin.data.remote.dto

import com.google.gson.annotations.SerializedName

data class AuthTokens(
    @SerializedName("access_token") val accessToken: String?,
    @SerializedName("refresh_token") val refreshToken: String?,
    @SerializedName("expires_at") val expiresAt: Long?,
    @SerializedName("expires_in") val expiresIn: Long? = null
)

data class SessionData(
    @SerializedName("provider_token") val providerToken: String?,
    @SerializedName("provider_refresh_token") val providerRefreshToken: String?,
    @SerializedName("token_type") val tokenType: String? = "bearer"
)

data class UserProfileDto(
    @SerializedName("id") val id: String,
    @SerializedName("email") val email: String,
    @SerializedName("full_name") val fullName: String? = null,
    @SerializedName("user_type") val userType: String? = null,
    @SerializedName("phone") val phone: String? = null,
    @SerializedName("avatar_url") val avatarUrl: String? = null
)

data class UserDto(
    @SerializedName("id") val id: String,
    @SerializedName("email") val email: String,
    @SerializedName("email_confirmed") val emailConfirmed: Boolean = false,
    @SerializedName("profile") val profile: UserProfileDto? = null
)
