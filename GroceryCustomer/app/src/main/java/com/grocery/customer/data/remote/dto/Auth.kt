package com.grocery.customer.data.remote.dto

data class LoginRequest(
    val email: String,
    val password: String
)

data class Tokens(
    val access_token: String?,
    val refresh_token: String?,
    val expires_at: Long?,
    val expires_in: Long?
)

data class SessionInfo(
    val token_type: String?
)

data class User(
    val id: String,
    val email: String,
    val email_confirmed: Boolean,
    val profile: UserProfile?
)

data class LoginData(
    val user: User,
    val tokens: Tokens?,
    val session: SessionInfo?
)

data class RegisterRequest(
    val email: String,
    val password: String,
    val full_name: String,
    val phone: String? = null,
    val user_type: String = "customer"
)

data class RegistrationInfo(
    val confirmation_sent: Boolean?,
    val requires_verification: Boolean?
)

data class RegisterData(
    val user: User,
    val registration: RegistrationInfo?,
    val tokens: Tokens? = null,
    val session: SessionInfo? = null
)
