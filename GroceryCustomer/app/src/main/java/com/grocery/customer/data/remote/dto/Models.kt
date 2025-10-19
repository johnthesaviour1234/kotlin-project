package com.grocery.customer.data.remote.dto

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

data class LoginResponse(
    @SerializedName("user") val user: UserDto,
    @SerializedName("tokens") val tokens: AuthTokens,
    @SerializedName("session") val session: SessionData? = null
)

data class RegistrationInfo(
    @SerializedName("confirmation_sent") val confirmationSent: Boolean = false,
    @SerializedName("requires_verification") val requiresVerification: Boolean = true
)

data class RegisterResponse(
    @SerializedName("user") val user: UserDto,
    @SerializedName("registration") val registration: RegistrationInfo,
    @SerializedName("tokens") val tokens: AuthTokens? = null,
    @SerializedName("session") val session: SessionData? = null
)

data class VerificationInfo(
    @SerializedName("status") val status: String,
    @SerializedName("verified_at") val verifiedAt: String? = null,
    @SerializedName("type") val type: String? = null
)

data class VerifyEmailResponse(
    @SerializedName("user") val user: UserDto,
    @SerializedName("verification") val verification: VerificationInfo,
    @SerializedName("tokens") val tokens: AuthTokens? = null,
    @SerializedName("session") val session: SessionData? = null
)