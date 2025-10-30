package com.grocery.admin.data.repository

import android.util.Log
import com.grocery.admin.data.local.TokenStore
import com.grocery.admin.data.remote.ApiService
import com.grocery.admin.data.remote.dto.LoginRequest
import com.grocery.admin.data.remote.dto.LoginResponse
import com.grocery.admin.data.remote.dto.RegisterRequest
import com.grocery.admin.data.remote.dto.RegisterResponse
import com.grocery.admin.domain.repository.AuthRepository
import com.grocery.admin.util.Resource
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AuthRepositoryImpl @Inject constructor(
    private val apiService: ApiService,
    private val tokenStore: TokenStore
) : AuthRepository {
    
    companion object {
        private const val TAG = "AuthRepositoryImpl"
    }
    
    override fun login(email: String, password: String): Flow<Resource<LoginResponse>> = flow {
        try {
            emit(Resource.Loading())
            Log.d(TAG, "Attempting login for email: $email")
            
            val response = apiService.login(LoginRequest(email, password))
            
            if (response.success && response.data != null) {
                Log.d(TAG, "Login successful")
                val tokens = response.data.tokens
                tokenStore.saveTokens(
                    accessToken = tokens.accessToken,
                    refreshToken = tokens.refreshToken,
                    expiresAt = tokens.expiresAt
                )
                
                emit(Resource.Success(response.data))
            } else {
                val errorMessage = response.error ?: response.message ?: "Login failed"
                Log.e(TAG, "Login failed: $errorMessage")
                emit(Resource.Error(errorMessage))
            }
        } catch (e: Exception) {
            val errorMessage = e.localizedMessage ?: "Network error occurred"
            Log.e(TAG, "Login error: $errorMessage", e)
            emit(Resource.Error(errorMessage))
        }
    }
    
    override fun register(
        email: String,
        password: String,
        fullName: String,
        phone: String?
    ): Flow<Resource<RegisterResponse>> = flow {
        try {
            emit(Resource.Loading())
            Log.d(TAG, "Attempting admin registration for email: $email")
            
            val response = apiService.register(
                RegisterRequest(
                    email = email,
                    password = password,
                    fullName = fullName,
                    phone = phone
                )
            )
            
            if (response.success && response.data != null) {
                Log.d(TAG, "Registration successful")
                // Save tokens if provided (auto sign-in)
                response.data.tokens?.let { tokens ->
                    tokenStore.saveTokens(
                        accessToken = tokens.accessToken,
                        refreshToken = tokens.refreshToken,
                        expiresAt = tokens.expiresAt
                    )
                }
                emit(Resource.Success(response.data))
            } else {
                val errorMessage = response.error ?: response.message ?: "Registration failed"
                Log.e(TAG, "Registration failed: $errorMessage")
                emit(Resource.Error(errorMessage))
            }
        } catch (e: Exception) {
            val errorMessage = e.localizedMessage ?: "Network error occurred"
            Log.e(TAG, "Registration error: $errorMessage", e)
            emit(Resource.Error(errorMessage))
        }
    }
    
    override suspend fun logout() {
        Log.d(TAG, "Logging out user")
        tokenStore.clear()
    }
    
    override suspend fun isLoggedIn(): Boolean {
        val token = tokenStore.getAccessToken()
        val isLoggedIn = !token.isNullOrBlank()
        Log.d(TAG, "isLoggedIn: $isLoggedIn")
        return isLoggedIn
    }
}
