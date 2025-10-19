package com.grocery.customer.data.repository

import com.grocery.customer.data.local.TokenStore
import com.grocery.customer.data.remote.ApiService
import com.grocery.customer.data.remote.dto.*
import com.grocery.customer.domain.repository.AuthRepository
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import okhttp3.ResponseBody
import javax.inject.Inject

class AuthRepositoryImpl @Inject constructor(
    private val api: ApiService,
    private val tokenStore: TokenStore
) : AuthRepository {

    private val gson = Gson()

    private fun parseApiError(errorBody: ResponseBody?): String {
        if (errorBody == null) return "Request failed"
        return try {
            val type = object : TypeToken<ApiResponse<Any?>>() {}.type
            val parsed: ApiResponse<Any?> = gson.fromJson(errorBody.charStream(), type)
            buildString {
                if (!parsed.error.isNullOrBlank()) append(parsed.error)
                val details = parsed.validationErrors
                if (!details.isNullOrEmpty()) {
                    val detailMsg = details.joinToString("; ") { (it.field ?: "").let { f -> if (f.isNotBlank()) "$f: ${it.message}" else it.message } }
                    if (isNotEmpty()) append(" - ")
                    append(detailMsg)
                }
            }.ifBlank { "Bad request" }
        } catch (e: Exception) {
            "Bad request"
        }
    }

    override suspend fun login(email: String, password: String): Result<LoginResponse> {
        return try {
            val response = api.login(LoginRequest(email, password))
            if (response.isSuccessful) {
                val body = response.body()
                if (body?.success == true && body.data != null) {
                    val tokens = body.data.tokens
                    tokenStore.saveTokens(tokens.accessToken, tokens.refreshToken, tokens.expiresAt)
                    Result.success(body.data)
                } else {
                    Result.failure(Exception(body?.error ?: body?.message ?: "Login failed"))
                }
            } else {
                Result.failure(Exception(parseApiError(response.errorBody())))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun register(
        email: String,
        password: String,
        fullName: String,
        phone: String?,
        userType: String
    ): Result<RegisterResponse> {
        return try {
            val response = api.register(RegisterRequest(email, password, fullName, phone, userType))
            if (response.isSuccessful) {
                val body = response.body()
                if (body?.success == true && body.data != null) {
                    // Save tokens if present
                    val tokens = body.data.tokens
                    if (tokens != null) {
                        tokenStore.saveTokens(tokens.accessToken, tokens.refreshToken, tokens.expiresAt)
                    }
                    Result.success(body.data)
                } else {
                    Result.failure(Exception(body?.error ?: body?.message ?: "Registration failed"))
                }
            } else {
                Result.failure(Exception(parseApiError(response.errorBody())))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun logout(): Result<Unit> {
        return try {
            tokenStore.clear()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun refreshToken(): Result<AuthTokens> {
        // Placeholder until refresh endpoint is implemented on backend
        return Result.failure(UnsupportedOperationException("Refresh token not implemented"))
    }

    override suspend fun getCurrentUser(): Result<UserProfileDto> {
        // Placeholder - would call a /users/profile endpoint when available
        return Result.failure(UnsupportedOperationException("Get current user not implemented"))
    }

    override suspend fun resendVerification(email: String): Result<Unit> {
        return try {
            val response = api.resendVerification(ResendVerificationRequest(email))
            if (response.isSuccessful) {
                val body = response.body()
                if (body?.success == true) Result.success(Unit) else Result.failure(Exception(body?.error ?: body?.message ?: "Failed to resend"))
            } else {
                Result.failure(Exception(parseApiError(response.errorBody())))
            }
        } catch (e: Exception) { Result.failure(e) }
    }
}
