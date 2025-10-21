package com.grocery.customer.data.repository

import android.util.Log
import com.grocery.customer.data.remote.ApiService
import com.grocery.customer.data.remote.dto.*
import com.grocery.customer.domain.repository.UserRepository
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Implementation of UserRepository
 */
@Singleton
class UserRepositoryImpl @Inject constructor(
    private val apiService: ApiService
) : UserRepository {
    
    companion object {
        private const val TAG = "UserRepositoryImpl"
    }
    
    override suspend fun getUserProfile(): Result<UserProfile> {
        return try {
            val response = apiService.getUserProfile()
            if (response.isSuccessful) {
                response.body()?.data?.let { profile ->
                    Result.success(profile)
                } ?: Result.failure(Exception("Empty profile response"))
            } else {
                Log.e(TAG, "Failed to get user profile: ${response.code()}")
                Result.failure(Exception("Failed to get user profile: ${response.code()}"))
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error getting user profile", e)
            Result.failure(e)
        }
    }
    
    override suspend fun getUserAddresses(): Result<List<UserAddress>> {
        return try {
            val response = apiService.getUserAddresses()
            if (response.isSuccessful) {
                response.body()?.data?.items?.let { addresses ->
                    Result.success(addresses)
                } ?: Result.success(emptyList())
            } else {
                Log.e(TAG, "Failed to get user addresses: ${response.code()}")
                Result.failure(Exception("Failed to get user addresses: ${response.code()}"))
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error getting user addresses", e)
            Result.failure(e)
        }
    }
    
    override suspend fun createAddress(address: UserAddress): Result<UserAddress> {
        return try {
            val request = CreateAddressRequest(
                name = "Home", // Default name since UserAddress doesn't have this field
                street_address = address.street_address,
                apartment = address.apartment,
                city = address.city,
                state = address.state,
                postal_code = address.postal_code,
                landmark = address.landmark,
                address_type = address.address_type,
                is_default = address.is_default
            )
            
            val response = apiService.createAddress(request)
            if (response.isSuccessful) {
                response.body()?.data?.let { createdAddress ->
                    Result.success(createdAddress)
                } ?: Result.failure(Exception("Empty create address response"))
            } else {
                Log.e(TAG, "Failed to create address: ${response.code()}")
                Result.failure(Exception("Failed to create address: ${response.code()}"))
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error creating address", e)
            Result.failure(e)
        }
    }
    
    override suspend fun updateAddress(addressId: String, address: UserAddress): Result<UserAddress> {
        return try {
            val request = UpdateAddressRequest(
                name = address.name,
                street_address = address.street_address,
                apartment = address.apartment,
                city = address.city,
                state = address.state,
                postal_code = address.postal_code,
                landmark = address.landmark,
                address_type = address.address_type,
                is_default = address.is_default
            )
            
            val response = apiService.updateAddress(addressId, request)
            if (response.isSuccessful) {
                response.body()?.data?.let { updatedAddress ->
                    Result.success(updatedAddress)
                } ?: Result.failure(Exception("Empty update address response"))
            } else {
                Log.e(TAG, "Failed to update address: ${response.code()}")
                Result.failure(Exception("Failed to update address: ${response.code()}"))
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error updating address", e)
            Result.failure(e)
        }
    }
    
    override suspend fun deleteAddress(addressId: String): Result<Unit> {
        return try {
            val response = apiService.deleteAddress(addressId)
            if (response.isSuccessful) {
                Result.success(Unit)
            } else {
                Log.e(TAG, "Failed to delete address: ${response.code()}")
                Result.failure(Exception("Failed to delete address: ${response.code()}"))
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error deleting address", e)
            Result.failure(e)
        }
    }
}