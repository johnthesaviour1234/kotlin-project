package com.grocery.admin.data.repository

import android.util.Log
import com.grocery.admin.data.remote.ApiService
import com.grocery.admin.data.remote.dto.*
import com.grocery.admin.domain.repository.InventoryRepository
import com.grocery.admin.util.Resource
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class InventoryRepositoryImpl @Inject constructor(
    private val apiService: ApiService
) : InventoryRepository {
    
    companion object {
        private const val TAG = "InventoryRepositoryImpl"
    }
    
    override fun getInventory(lowStock: Boolean?, threshold: Int): Flow<Resource<InventoryListResponse>> = flow {
        try {
            emit(Resource.Loading())
            Log.d(TAG, "Fetching inventory - lowStock: $lowStock, threshold: $threshold")
            
            val response = apiService.getInventory(lowStock, threshold)
            
            if (response.success && response.data != null) {
                Log.d(TAG, "Inventory fetched successfully: ${response.data.items.size} items")
                emit(Resource.Success(response.data))
            } else {
                val errorMessage = response.error ?: response.message ?: "Failed to fetch inventory"
                Log.e(TAG, "Failed to fetch inventory: $errorMessage")
                emit(Resource.Error(errorMessage))
            }
        } catch (e: Exception) {
            val errorMessage = e.localizedMessage ?: "Network error occurred"
            Log.e(TAG, "Error fetching inventory: $errorMessage", e)
            emit(Resource.Error(errorMessage))
        }
    }
    
    override fun updateInventory(request: UpdateInventoryRequest): Flow<Resource<UpdateInventoryResponse>> = flow {
        try {
            emit(Resource.Loading())
            Log.d(TAG, "Updating inventory - product: ${request.productId}, quantity: ${request.quantity}")
            
            val response = apiService.updateInventory(request)
            
            if (response.success && response.data != null) {
                Log.d(TAG, "Inventory updated successfully")
                emit(Resource.Success(response.data))
            } else {
                val errorMessage = response.error ?: response.message ?: "Failed to update inventory"
                Log.e(TAG, "Failed to update inventory: $errorMessage")
                emit(Resource.Error(errorMessage))
            }
        } catch (e: Exception) {
            val errorMessage = e.localizedMessage ?: "Network error occurred"
            Log.e(TAG, "Error updating inventory: $errorMessage", e)
            emit(Resource.Error(errorMessage))
        }
    }
}
