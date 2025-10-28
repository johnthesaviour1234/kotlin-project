package com.grocery.delivery.data.repository

import com.grocery.delivery.data.api.DeliveryApiService
import com.grocery.delivery.data.dto.*
import com.grocery.delivery.data.local.PreferencesManager
import com.grocery.delivery.utils.Resource
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Repository for delivery operations (orders and location)
 */
@Singleton
class DeliveryRepository @Inject constructor(
    private val apiService: DeliveryApiService,
    private val preferencesManager: PreferencesManager
) {
    
    private fun getAuthHeader(): String {
        val token = preferencesManager.getAuthToken()
        return "Bearer $token"
    }
    
    private fun getDriverId(): String? {
        return preferencesManager.getUserId()
    }
    
    /**
     * Get available orders for delivery
     */
    fun getAvailableOrders(): Flow<Resource<List<DeliveryAssignment>>> = flow {
        try {
            emit(Resource.Loading())
            
            val response = apiService.getAvailableOrders(getAuthHeader())
            
            if (response.isSuccessful && response.body() != null) {
                val ordersResponse = response.body()!!
                
                if (ordersResponse.success && ordersResponse.data != null) {
                    emit(Resource.Success(ordersResponse.data.items ?: emptyList()))
                } else {
                    emit(Resource.Error("Failed to fetch orders"))
                }
            } else {
                emit(Resource.Error(response.message() ?: "Network error"))
            }
        } catch (e: Exception) {
            emit(Resource.Error(e.localizedMessage ?: "An unexpected error occurred"))
        }
    }
    
    /**
     * Get active order for the driver
     */
    fun getActiveOrder(): Flow<Resource<Map<String, Any>?>> = flow {
        try {
            emit(Resource.Loading())
            
            val driverId = getDriverId()
            if (driverId == null) {
                emit(Resource.Error("Driver ID not found"))
                return@flow
            }
            
            val response = apiService.getActiveOrder(getAuthHeader(), driverId)
            
            if (response.isSuccessful && response.body() != null) {
                val orderResponse = response.body()!!
                
                if (orderResponse.success) {
                    emit(Resource.Success(orderResponse.data))
                } else {
                    emit(Resource.Success(null))
                }
            } else {
                emit(Resource.Error(response.message() ?: "Network error"))
            }
        } catch (e: Exception) {
            emit(Resource.Error(e.localizedMessage ?: "An unexpected error occurred"))
        }
    }
    
    /**
     * Get order history for the driver
     */
    fun getOrderHistory(limit: Int = 50): Flow<Resource<List<DeliveryAssignment>>> = flow {
        try {
            emit(Resource.Loading())
            
            val response = apiService.getOrderHistory(getAuthHeader(), limit, 0)
            
            if (response.isSuccessful && response.body() != null) {
                val ordersResponse = response.body()!!
                
                if (ordersResponse.success && ordersResponse.data != null) {
                    emit(Resource.Success(ordersResponse.data.items ?: emptyList()))
                } else {
                    emit(Resource.Error("Failed to fetch order history"))
                }
            } else {
                emit(Resource.Error(response.message() ?: "Network error"))
            }
        } catch (e: Exception) {
            emit(Resource.Error(e.localizedMessage ?: "An unexpected error occurred"))
        }
    }
    
    /**
     * Accept an order for delivery
     */
    fun acceptOrder(assignmentId: String, notes: String? = null): Flow<Resource<String>> = flow {
        try {
            emit(Resource.Loading())
            
            val request = AcceptOrderRequest(
                assignmentId = assignmentId,
                notes = notes
            )
            
            val response = apiService.acceptOrder(getAuthHeader(), request)
            
            if (response.isSuccessful && response.body() != null) {
                val orderResponse = response.body()!!
                
                if (orderResponse.success) {
                    emit(Resource.Success(orderResponse.message ?: "Order accepted"))
                } else {
                    emit(Resource.Error(orderResponse.message ?: "Failed to accept order"))
                }
            } else {
                emit(Resource.Error(response.message() ?: "Network error"))
            }
        } catch (e: Exception) {
            emit(Resource.Error(e.localizedMessage ?: "An unexpected error occurred"))
        }
    }
    
    /**
     * Decline an order for delivery
     */
    fun declineOrder(assignmentId: String, reason: String? = null): Flow<Resource<String>> = flow {
        try {
            emit(Resource.Loading())
            
            val request = DeclineOrderRequest(
                assignmentId = assignmentId,
                reason = reason
            )
            
            val response = apiService.declineOrder(getAuthHeader(), request)
            
            if (response.isSuccessful && response.body() != null) {
                val orderResponse = response.body()!!
                
                if (orderResponse.success) {
                    emit(Resource.Success(orderResponse.message ?: "Order declined"))
                } else {
                    emit(Resource.Error(orderResponse.message ?: "Failed to decline order"))
                }
            } else {
                emit(Resource.Error(response.message() ?: "Network error"))
            }
        } catch (e: Exception) {
            emit(Resource.Error(e.localizedMessage ?: "An unexpected error occurred"))
        }
    }
    
    /**
     * Update order status
     */
    fun updateOrderStatus(
        assignmentId: String,
        status: String,
        notes: String? = null,
        proofOfDelivery: String? = null
    ): Flow<Resource<String>> = flow {
        try {
            emit(Resource.Loading())
            
            val request = UpdateOrderStatusRequest(
                assignmentId = assignmentId,
                status = status,
                notes = notes,
                proofOfDelivery = proofOfDelivery
            )
            
            val response = apiService.updateOrderStatus(getAuthHeader(), request)
            
            if (response.isSuccessful && response.body() != null) {
                val orderResponse = response.body()!!
                
                if (orderResponse.success) {
                    emit(Resource.Success(orderResponse.message ?: "Status updated"))
                } else {
                    emit(Resource.Error(orderResponse.message ?: "Failed to update order status"))
                }
            } else {
                emit(Resource.Error(response.message() ?: "Network error"))
            }
        } catch (e: Exception) {
            emit(Resource.Error(e.localizedMessage ?: "An unexpected error occurred"))
        }
    }
    
    /**
     * Update driver location
     */
    fun updateLocation(
        latitude: Double,
        longitude: Double,
        orderId: String? = null,
        accuracy: Double? = null,
        speed: Double? = null,
        heading: Double? = null
    ): Flow<Resource<Unit>> = flow {
        try {
            emit(Resource.Loading())
            
            val request = LocationUpdateRequest(
                latitude = latitude,
                longitude = longitude,
                orderId = orderId,
                accuracy = accuracy,
                speed = speed,
                heading = heading
            )
            
            val response = apiService.updateLocation(getAuthHeader(), request)
            
            if (response.isSuccessful && response.body() != null) {
                val locationResponse = response.body()!!
                
                if (locationResponse.success) {
                    emit(Resource.Success(Unit))
                } else {
                    emit(Resource.Error(locationResponse.message ?: "Failed to update location"))
                }
            } else {
                emit(Resource.Error(response.message() ?: "Network error"))
            }
        } catch (e: Exception) {
            emit(Resource.Error(e.localizedMessage ?: "Failed to update location"))
        }
    }
    
    /**
     * Get driver profile with statistics
     */
    fun getProfile(): Flow<Resource<ProfileResponse>> = flow {
        try {
            emit(Resource.Loading())
            
            val response = apiService.getProfile(getAuthHeader())
            
            if (response.isSuccessful && response.body() != null) {
                val apiResponse = response.body()!!
                
                if (apiResponse.success && apiResponse.data != null) {
                    emit(Resource.Success(apiResponse.data))
                } else {
                    emit(Resource.Error(apiResponse.message ?: "Failed to fetch profile"))
                }
            } else {
                emit(Resource.Error(response.message() ?: "Network error"))
            }
        } catch (e: Exception) {
            emit(Resource.Error(e.localizedMessage ?: "An unexpected error occurred"))
        }
    }
    
    /**
     * Update driver profile
     */
    fun updateProfile(
        fullName: String? = null,
        phone: String? = null,
        avatarUrl: String? = null,
        preferences: Map<String, Any>? = null
    ): Flow<Resource<DriverProfile>> = flow {
        try {
            emit(Resource.Loading())
            
            val request = ProfileUpdateRequest(
                fullName = fullName,
                phone = phone,
                avatarUrl = avatarUrl,
                preferences = preferences
            )
            
            val response = apiService.updateProfile(getAuthHeader(), request)
            
            if (response.isSuccessful && response.body() != null) {
                val apiResponse = response.body()!!
                
                if (apiResponse.success && apiResponse.data != null) {
                    emit(Resource.Success(apiResponse.data.profile))
                } else {
                    emit(Resource.Error(apiResponse.message ?: "Failed to update profile"))
                }
            } else {
                emit(Resource.Error(response.message() ?: "Network error"))
            }
        } catch (e: Exception) {
            emit(Resource.Error(e.localizedMessage ?: "An unexpected error occurred"))
        }
    }
}
