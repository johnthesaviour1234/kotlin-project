package com.grocery.admin.data.repository

import android.util.Log
import com.grocery.admin.data.remote.ApiService
import com.grocery.admin.data.remote.dto.*
import com.grocery.admin.domain.repository.OrdersRepository
import com.grocery.admin.util.Resource
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class OrdersRepositoryImpl @Inject constructor(
    private val apiService: ApiService
) : OrdersRepository {
    
    companion object {
        private const val TAG = "OrdersRepositoryImpl"
    }
    
    override fun getOrders(page: Int, limit: Int, status: String?): Flow<Resource<OrdersListResponse>> = flow {
        try {
            emit(Resource.Loading())
            Log.d(TAG, "Fetching orders - page: $page, limit: $limit, status: $status")
            
            val response = apiService.getOrders(page, limit, status)
            
            if (response.success && response.data != null) {
                Log.d(TAG, "Orders fetched successfully: ${response.data.items.size} items")
                emit(Resource.Success(response.data))
            } else {
                val errorMessage = response.error ?: response.message ?: "Failed to fetch orders"
                Log.e(TAG, "Failed to fetch orders: $errorMessage")
                emit(Resource.Error(errorMessage))
            }
        } catch (e: Exception) {
            val errorMessage = e.localizedMessage ?: "Network error occurred"
            Log.e(TAG, "Error fetching orders: $errorMessage", e)
            emit(Resource.Error(errorMessage))
        }
    }
    
    override fun getOrderById(orderId: String): Flow<Resource<OrderDto>> = flow {
        try {
            emit(Resource.Loading())
            Log.d(TAG, "Fetching order by ID: $orderId")
            
            val response = apiService.getOrderById(orderId)
            
            if (response.success && response.data != null) {
                Log.d(TAG, "Order fetched successfully: ${response.data.id}")
                emit(Resource.Success(response.data))
            } else {
                val errorMessage = response.error ?: response.message ?: "Failed to fetch order"
                Log.e(TAG, "Failed to fetch order: $errorMessage")
                emit(Resource.Error(errorMessage))
            }
        } catch (e: Exception) {
            val errorMessage = e.localizedMessage ?: "Network error occurred"
            Log.e(TAG, "Error fetching order: $errorMessage", e)
            emit(Resource.Error(errorMessage))
        }
    }
    
    override fun updateOrderStatus(orderId: String, status: String, notes: String?): Flow<Resource<OrderDto>> = flow {
        try {
            emit(Resource.Loading())
            Log.d(TAG, "Updating order status - ID: $orderId, status: $status")
            
            val request = UpdateOrderStatusRequest(status, notes)
            val response = apiService.updateOrderStatus(orderId, request)
            
            if (response.success && response.data != null) {
                Log.d(TAG, "Order status updated successfully")
                emit(Resource.Success(response.data))
            } else {
                val errorMessage = response.error ?: response.message ?: "Failed to update order"
                Log.e(TAG, "Failed to update order: $errorMessage")
                emit(Resource.Error(errorMessage))
            }
        } catch (e: Exception) {
            val errorMessage = e.localizedMessage ?: "Network error occurred"
            Log.e(TAG, "Error updating order: $errorMessage", e)
            emit(Resource.Error(errorMessage))
        }
    }
    
    override fun assignOrder(orderId: String, deliveryPersonnelId: String, estimatedMinutes: Int): Flow<Resource<AssignOrderResponse>> = flow {
        try {
            emit(Resource.Loading())
            Log.d(TAG, "Assigning order - ID: $orderId, driver: $deliveryPersonnelId")
            
            val request = AssignOrderRequest(orderId, deliveryPersonnelId, estimatedMinutes)
            val response = apiService.assignOrder(request)
            
            if (response.success && response.data != null) {
                Log.d(TAG, "Order assigned successfully: ${response.data.assignmentId}")
                emit(Resource.Success(response.data))
            } else {
                val errorMessage = response.error ?: response.message ?: "Failed to assign order"
                Log.e(TAG, "Failed to assign order: $errorMessage")
                emit(Resource.Error(errorMessage))
            }
        } catch (e: Exception) {
            val errorMessage = e.localizedMessage ?: "Network error occurred"
            Log.e(TAG, "Error assigning order: $errorMessage", e)
            emit(Resource.Error(errorMessage))
        }
    }
}
