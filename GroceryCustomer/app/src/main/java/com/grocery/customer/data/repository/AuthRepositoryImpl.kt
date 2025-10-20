package com.grocery.customer.data.repository

import com.grocery.customer.data.local.TokenStore
import com.grocery.customer.data.remote.ApiService
import com.grocery.customer.data.remote.dto.*
import com.grocery.customer.domain.repository.AuthRepository
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import okhttp3.ResponseBody
import android.util.Log
import javax.inject.Inject

class AuthRepositoryImpl @Inject constructor(
    private val api: ApiService,
    private val tokenStore: TokenStore
) : AuthRepository {

    companion object {
        private const val TAG = "AuthRepositoryImpl"
    }

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
        Log.d(TAG, "Starting login for email: $email")
        return try {
            val response = api.login(LoginRequest(email, password))
            Log.d(TAG, "Login API response: ${response.code()} - ${response.message()}")
            
            if (response.isSuccessful) {
                val body = response.body()
                Log.d(TAG, "Login response body success: ${body?.success}, hasData: ${body?.data != null}")
                
                if (body?.success == true && body.data != null) {
                    val tokens = body.data.tokens
                    Log.d(TAG, "Tokens received - Access: ${tokens.accessToken != null}, Refresh: ${tokens.refreshToken != null}")
                    
                    tokenStore.saveTokens(tokens.accessToken, tokens.refreshToken, tokens.expiresAt)
                    Log.d(TAG, "Login successful")
                    Result.success(body.data)
                } else {
                    val errorMsg = body?.error ?: body?.message ?: "Login failed"
                    Log.e(TAG, "Login failed: $errorMsg")
                    Result.failure(Exception(errorMsg))
                }
            } else {
                val errorMsg = parseApiError(response.errorBody())
                Log.e(TAG, "Login request failed: $errorMsg")
                Result.failure(Exception(errorMsg))
            }
        } catch (e: Exception) {
            Log.e(TAG, "Exception during login: ${e.message}", e)
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
        return try {
            val token = tokenStore.getAccessToken()
            if (token.isNullOrBlank()) {
                return Result.failure(Exception("No authentication token found"))
            }
            
            // AuthInterceptor automatically adds the Bearer token
            val response = api.getUserProfile()
            if (response.isSuccessful) {
                val body = response.body()
                if (body?.success == true && body.data != null) {
                    val profile = body.data
                    // Convert UserProfile to UserProfileDto
                    val userProfileDto = UserProfileDto(
                        id = profile.id,
                        email = profile.email ?: "",
                        fullName = profile.full_name,
                        phone = profile.phone,
                        userType = profile.user_type,
                        avatarUrl = profile.avatar_url
                    )
                    Result.success(userProfileDto)
                } else {
                    Result.failure(Exception(body?.error ?: body?.message ?: "Failed to get user profile"))
                }
            } else {
                Result.failure(Exception(parseApiError(response.errorBody())))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
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
