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
 * ViewModel for managing orders in the admin panel
 */
@HiltViewModel
class OrdersViewModel @Inject constructor(
    private val ordersRepository: OrdersRepository
) : BaseViewModel() {

    private val _orders = MutableLiveData<Resource<List<OrderDto>>>()
    val orders: LiveData<Resource<List<OrderDto>>> = _orders

    private val _updateStatusResult = MutableLiveData<Resource<String>>()
    val updateStatusResult: LiveData<Resource<String>> = _updateStatusResult

    private val _assignDriverResult = MutableLiveData<Resource<String>>()
    val assignDriverResult: LiveData<Resource<String>> = _assignDriverResult

    // Filter and search state
    private var currentPage = 1
    private var currentStatus: String? = null
    private var currentSearchQuery: String = ""
    private var allOrders: List<OrderDto> = emptyList()


    /**
     * Load orders from the API
     */
    fun loadOrders(page: Int = 1, status: String? = null) {
        viewModelScope.launch {
            _orders.value = Resource.Loading()
            
            ordersRepository.getOrders(
                page = page,
                limit = 20,
                status = status
            ).collect { resource ->
                when (resource) {
                    is Resource.Success -> {
                        allOrders = resource.data?.items ?: emptyList()
                        applyFiltersAndSearch()
                    }
                    is Resource.Error -> {
                        _orders.value = Resource.Error(resource.message ?: "Failed to load orders")
                    }
                    is Resource.Loading -> {
                        _orders.value = Resource.Loading()
                    }
                }
            }
        }
    }

    /**
     * Refresh orders (pull-to-refresh)
     */
    fun refreshOrders() {
        loadOrders(currentPage, currentStatus)
    }

    /**
     * Search orders by order number or customer email
     */
    fun searchOrders(query: String) {
        currentSearchQuery = query
        applyFiltersAndSearch()
    }

    /**
     * Filter orders by status
     */
    fun filterByStatus(status: String?) {
        currentStatus = status
        loadOrders(currentPage, status)
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
     * Reset update status result (after handling)
     */
    fun resetUpdateStatusResult() {
        _updateStatusResult.value = null
    }

    /**
     * Reset assign driver result (after handling)
     */
    fun resetAssignDriverResult() {
        _assignDriverResult.value = null
    }

    /**
     * Apply current filters and search query to orders list
     */
    private fun applyFiltersAndSearch() {
        val filteredOrders = allOrders.filter { order: OrderDto ->
            // Apply search filter
            if (currentSearchQuery.isNotBlank()) {
                order.id.contains(currentSearchQuery, ignoreCase = true) ||
                order.orderNumber?.contains(currentSearchQuery, ignoreCase = true) == true ||
                order.customerInfo?.email?.contains(currentSearchQuery, ignoreCase = true) == true ||
                order.customerInfo?.name?.contains(currentSearchQuery, ignoreCase = true) == true
            } else {
                true
            }
        }
        
        _orders.value = Resource.Success(filteredOrders)
    }


}
