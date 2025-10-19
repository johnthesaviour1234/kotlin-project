package com.grocery.customer.domain.repository

import com.grocery.customer.data.remote.dto.AuthTokens
import com.grocery.customer.data.remote.dto.LoginResponse
import com.grocery.customer.data.remote.dto.RegisterResponse
import com.grocery.customer.data.remote.dto.UserProfileDto

interface AuthRepository {
    suspend fun login(email: String, password: String): Result<LoginResponse>
    suspend fun register(email: String, password: String, fullName: String, phone: String?, userType: String): Result<RegisterResponse>
    suspend fun logout(): Result<Unit>
    suspend fun refreshToken(): Result<AuthTokens>
    suspend fun getCurrentUser(): Result<UserProfileDto>
    suspend fun resendVerification(email: String): Result<Unit>
}
