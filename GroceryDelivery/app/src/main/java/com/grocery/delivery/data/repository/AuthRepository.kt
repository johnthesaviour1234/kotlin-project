package com.grocery.delivery.data.repository

import com.grocery.delivery.data.api.DeliveryApiService
import com.grocery.delivery.data.dto.DeliveryLoginRequest
import com.grocery.delivery.data.dto.DeliveryLoginResponse
import com.grocery.delivery.data.dto.DeliveryRegisterRequest
import com.grocery.delivery.data.dto.DeliveryRegisterResponse
import com.grocery.delivery.data.local.PreferencesManager
import com.grocery.delivery.utils.Resource
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Repository for authentication operations
 */
@Singleton
class AuthRepository @Inject constructor(
    private val apiService: DeliveryApiService,
    private val preferencesManager: PreferencesManager
) {
    
    /**
     * Login with email and password
     */
    fun login(email: String, password: String): Flow<Resource<DeliveryLoginResponse>> = flow {
        try {
            emit(Resource.Loading())
            
            val request = DeliveryLoginRequest(
                email = email,
                password = password,
                userType = "delivery"
            )
            
            val response = apiService.login(request)
            
            if (response.isSuccessful && response.body() != null) {
                val loginResponse = response.body()!!
                
                if (loginResponse.success && loginResponse.data != null) {
                    // Save authentication data
                    preferencesManager.saveAuthToken(loginResponse.data.tokens.accessToken)
                    loginResponse.data.tokens.refreshToken?.let {
                        preferencesManager.saveRefreshToken(it)
                    }
                    loginResponse.data.tokens.expiresAt?.let {
                        preferencesManager.saveExpiresAt(it)
                    }
                    preferencesManager.saveUserId(loginResponse.data.user.id)
                    preferencesManager.saveUserEmail(loginResponse.data.user.email)
                    loginResponse.data.user.fullName?.let {
                        preferencesManager.saveUserName(it)
                    }
                    
                    emit(Resource.Success(loginResponse))
                } else {
                    emit(Resource.Error(loginResponse.message ?: "Login failed"))
                }
            } else {
                emit(Resource.Error(response.message() ?: "Network error"))
            }
        } catch (e: Exception) {
            emit(Resource.Error(e.localizedMessage ?: "An unexpected error occurred"))
        }
    }
    
    /**
     * Register a new delivery driver
     */
    fun register(
        email: String,
        password: String,
        fullName: String,
        phone: String,
        vehicleType: String,
        vehicleNumber: String
    ): Flow<Resource<DeliveryRegisterResponse>> = flow {
        try {
            emit(Resource.Loading())
            
            val request = DeliveryRegisterRequest(
                email = email,
                password = password,
                fullName = fullName,
                phone = phone,
                vehicleType = vehicleType,
                vehicleNumber = vehicleNumber,
                userType = "delivery"
            )
            
            val response = apiService.register(request)
            
            if (response.isSuccessful && response.body() != null) {
                val registerResponse = response.body()!!
                
                if (registerResponse.success && registerResponse.data != null) {
                    // Save authentication data
                    preferencesManager.saveAuthToken(registerResponse.data.tokens.accessToken)
                    registerResponse.data.tokens.refreshToken?.let {
                        preferencesManager.saveRefreshToken(it)
                    }
                    registerResponse.data.tokens.expiresAt?.let {
                        preferencesManager.saveExpiresAt(it)
                    }
                    preferencesManager.saveUserId(registerResponse.data.user.id)
                    preferencesManager.saveUserEmail(registerResponse.data.user.email)
                    registerResponse.data.user.fullName?.let {
                        preferencesManager.saveUserName(it)
                    }
                    
                    emit(Resource.Success(registerResponse))
                } else {
                    emit(Resource.Error(registerResponse.message ?: "Registration failed"))
                }
            } else {
                emit(Resource.Error(response.message() ?: "Network error"))
            }
        } catch (e: Exception) {
            emit(Resource.Error(e.localizedMessage ?: "An unexpected error occurred"))
        }
    }
    
    /**
     * Logout the current user
     */
    fun logout(): Flow<Resource<Unit>> = flow {
        try {
            emit(Resource.Loading())
            
            try {
                // AuthInterceptor will add the token automatically
                apiService.logout()
            } catch (e: Exception) {
                // Ignore logout API errors, clear local data anyway
            }
            
            // Clear all stored data
            preferencesManager.clearAll()
            
            emit(Resource.Success(Unit))
        } catch (e: Exception) {
            emit(Resource.Error(e.localizedMessage ?: "Logout failed"))
        }
    }
    
    /**
     * Check if user is logged in
     */
    fun isLoggedIn(): Boolean {
        val token = preferencesManager.getAuthToken()
        return !token.isNullOrEmpty()
    }
    
    /**
     * Get current user ID
     */
    fun getUserId(): String? {
        return preferencesManager.getUserId()
    }
    
    /**
     * Get current auth token
     */
    fun getAuthToken(): String? {
        return preferencesManager.getAuthToken()
    }
}
