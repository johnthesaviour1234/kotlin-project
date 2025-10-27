package com.grocery.admin.ui.viewmodels

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.grocery.admin.data.remote.dto.InventoryItemDto
import com.grocery.admin.data.remote.dto.InventoryListResponse
import com.grocery.admin.data.remote.dto.UpdateInventoryRequest
import com.grocery.admin.domain.repository.InventoryRepository
import com.grocery.admin.util.Resource
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class InventoryViewModel @Inject constructor(
    private val inventoryRepository: InventoryRepository
) : BaseViewModel() {
    
    companion object {
        private const val TAG = "InventoryViewModel"
    }
    
    private val _inventory = MutableLiveData<Resource<InventoryListResponse>>()
    val inventory: LiveData<Resource<InventoryListResponse>> = _inventory
    
    private val _updateResult = MutableLiveData<Resource<Boolean>>()
    val updateResult: LiveData<Resource<Boolean>> = _updateResult
    
    private var currentLowStockFilter: Boolean? = null
    private var currentThreshold: Int = 10
    
    init {
        loadInventory()
    }
    
    fun loadInventory(lowStock: Boolean? = null, threshold: Int = 10) {
        currentLowStockFilter = lowStock
        currentThreshold = threshold
        
        viewModelScope.launch {
            try {
                Log.d(TAG, "Loading inventory - lowStock: $lowStock, threshold: $threshold")
                inventoryRepository.getInventory(lowStock, threshold).collect { resource ->
                    when (resource) {
                        is Resource.Loading -> {
                            Log.d(TAG, "Loading inventory...")
                            _inventory.postValue(Resource.Loading())
                        }
                        is Resource.Success -> {
                            Log.d(TAG, "Inventory loaded: ${resource.data?.items?.size ?: 0} items")
                            _inventory.postValue(resource)
                        }
                        is Resource.Error -> {
                            Log.e(TAG, "Error loading inventory: ${resource.message}")
                            _inventory.postValue(Resource.Error(resource.message ?: "Failed to load inventory"))
                        }
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Exception loading inventory", e)
                _inventory.postValue(Resource.Error(e.localizedMessage ?: "An error occurred"))
            }
        }
    }
    
    fun refreshInventory() {
        loadInventory(currentLowStockFilter, currentThreshold)
    }
    
    fun updateStock(productId: String, stock: Int, adjustmentType: String = "set") {
        viewModelScope.launch {
            try {
                Log.d(TAG, "Updating stock - productId: $productId, stock: $stock, type: $adjustmentType")
                val request = UpdateInventoryRequest(
                    productId = productId,
                    stock = stock,
                    adjustmentType = adjustmentType
                )
                
                inventoryRepository.updateInventory(request).collect { resource ->
                    when (resource) {
                        is Resource.Loading -> {
                            Log.d(TAG, "Updating inventory...")
                            _updateResult.postValue(Resource.Loading())
                        }
                        is Resource.Success -> {
                            Log.d(TAG, "Inventory updated successfully")
                            _updateResult.postValue(Resource.Success(true))
                            // Refresh inventory after successful update
                            refreshInventory()
                        }
                        is Resource.Error -> {
                            Log.e(TAG, "Error updating inventory: ${resource.message}")
                            _updateResult.postValue(Resource.Error(resource.message ?: "Failed to update inventory"))
                        }
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Exception updating inventory", e)
                _updateResult.postValue(Resource.Error(e.localizedMessage ?: "An error occurred"))
            }
        }
    }
    
    fun filterLowStock() {
        loadInventory(lowStock = true, threshold = currentThreshold)
    }
    
    fun clearFilter() {
        loadInventory(lowStock = null, threshold = currentThreshold)
    }
    
    fun clearUpdateResult() {
        _updateResult.value = null
    }
}
