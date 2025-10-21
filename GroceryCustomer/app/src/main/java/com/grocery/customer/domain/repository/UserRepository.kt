package com.grocery.customer.domain.repository

import com.grocery.customer.data.remote.dto.UserAddress
import com.grocery.customer.data.remote.dto.UserProfile

/**
 * Repository interface for user-related operations
 */
interface UserRepository {
    
    /**
     * Get user profile information
     */
    suspend fun getUserProfile(): Result<UserProfile>
    
    /**
     * Get user addresses
     */
    suspend fun getUserAddresses(): Result<List<UserAddress>>
    
    /**
     * Create a new address
     */
    suspend fun createAddress(address: UserAddress): Result<UserAddress>
    
    /**
     * Update an existing address
     */
    suspend fun updateAddress(addressId: String, address: UserAddress): Result<UserAddress>
    
    /**
     * Delete an address
     */
    suspend fun deleteAddress(addressId: String): Result<Unit>
}