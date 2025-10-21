package com.grocery.customer.domain.repository

import com.grocery.customer.data.remote.dto.*

interface AuthRepository {
    suspend fun login(email: String, password: String): Result<LoginResponse>
    suspend fun register(email: String, password: String, fullName: String, phone: String?, userType: String): Result<RegisterResponse>
    suspend fun logout(): Result<Unit>
    suspend fun refreshToken(): Result<AuthTokens>
    suspend fun getCurrentUser(): Result<UserProfileDto>
    suspend fun resendVerification(email: String): Result<Unit>
    
    // Profile management
    suspend fun getUserProfile(): Result<UserProfile>
    suspend fun updateUserProfile(request: ProfileUpdateRequest): Result<UserProfile>
    
    // Address management
    suspend fun getUserAddresses(): Result<AddressPayload>
    suspend fun createAddress(request: CreateAddressRequest): Result<UserAddress>
    suspend fun updateAddress(addressId: String, request: UpdateAddressRequest): Result<UserAddress>
    suspend fun deleteAddress(addressId: String): Result<Map<String, Any>>
    
    // Password management
    suspend fun changePassword(request: ChangePasswordRequest): Result<Map<String, String>>
}
