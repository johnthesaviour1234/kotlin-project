package com.grocery.admin.ui.viewmodels

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.grocery.admin.data.remote.dto.OrderDto
import com.grocery.admin.domain.repository.OrdersRepository
import com.grocery.admin.util.Resource
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * ViewModel for order detail screen functionality
 */
@HiltViewModel
class OrderDetailViewModel @Inject constructor(
    private val ordersRepository: OrdersRepository
) : BaseViewModel() {

    private val _orderDetails = MutableLiveData<Resource<OrderDto>>()
    val orderDetails: LiveData<Resource<OrderDto>> = _orderDetails

    private val _updateStatusResult = MutableLiveData<Resource<String>>()
    val updateStatusResult: LiveData<Resource<String>> = _updateStatusResult

    private val _assignDriverResult = MutableLiveData<Resource<String>>()
    val assignDriverResult: LiveData<Resource<String>> = _assignDriverResult
    
    private val _deliveryPersonnel = MutableLiveData<Resource<List<com.grocery.admin.data.remote.dto.DeliveryPersonnelDto>>>()
    val deliveryPersonnel: LiveData<Resource<List<com.grocery.admin.data.remote.dto.DeliveryPersonnelDto>>> = _deliveryPersonnel

    /**
     * Load order details by ID
     */
    fun loadOrderDetails(orderId: String) {
        viewModelScope.launch {
            _orderDetails.value = Resource.Loading()
            
            ordersRepository.getOrderById(orderId).collect { resource ->
                _orderDetails.value = resource
            }
        }
    }

    /**
     * Update order status
     */
    fun updateOrderStatus(orderId: String, newStatus: String, notes: String = "") {
        viewModelScope.launch {
            _updateStatusResult.value = Resource.Loading()
            
            ordersRepository.updateOrderStatus(orderId, newStatus, notes).collect { resource ->
                _updateStatusResult.value = when (resource) {
                    is Resource.Success -> Resource.Success("Status updated successfully")
                    is Resource.Error -> Resource.Error(resource.message ?: "Failed to update status")
                    is Resource.Loading -> Resource.Loading()
                }
            }
        }
    }

    /**
     * Assign driver to order
     */
    fun assignDriverToOrder(orderId: String, driverId: String, estimatedMinutes: Int = 30) {
        viewModelScope.launch {
            _assignDriverResult.value = Resource.Loading()
            
            ordersRepository.assignOrder(orderId, driverId, estimatedMinutes).collect { resource ->
                _assignDriverResult.value = when (resource) {
                    is Resource.Success -> Resource.Success("Driver assigned successfully")
                    is Resource.Error -> Resource.Error(resource.message ?: "Failed to assign driver")
                    is Resource.Loading -> Resource.Loading()
                }
            }
        }
    }

    /**
     * Reset update status result
     */
    fun resetUpdateStatusResult() {
        _updateStatusResult.value = null
    }

    /**
     * Reset assign driver result
     */
    fun resetAssignDriverResult() {
        _assignDriverResult.value = null
    }
    
    /**
     * Load available delivery personnel
     */
    fun loadDeliveryPersonnel() {
        viewModelScope.launch {
            _deliveryPersonnel.value = Resource.Loading()
            
            ordersRepository.getDeliveryPersonnel(activeOnly = true).collect { resource ->
                _deliveryPersonnel.value = when (resource) {
                    is Resource.Success -> Resource.Success(resource.data?.items ?: emptyList())
                    is Resource.Error -> Resource.Error(resource.message ?: "Failed to load delivery personnel")
                    is Resource.Loading -> Resource.Loading()
                }
            }
        }
    }
}
