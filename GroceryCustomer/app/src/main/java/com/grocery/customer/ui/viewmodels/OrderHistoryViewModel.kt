package com.grocery.customer.ui.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.grocery.customer.data.remote.dto.*
import com.grocery.customer.domain.usecase.GetOrderHistoryUseCase
import com.grocery.customer.domain.usecase.GetOrderDetailsUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * ViewModel for managing order history.
 * Handles loading, filtering, and pagination of user's order history.
 */
@HiltViewModel
class OrderHistoryViewModel @Inject constructor(
    private val getOrderHistoryUseCase: GetOrderHistoryUseCase,
    private val getOrderDetailsUseCase: GetOrderDetailsUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(OrderHistoryUiState())
    val uiState: StateFlow<OrderHistoryUiState> = _uiState.asStateFlow()

    private val _selectedOrderState = MutableStateFlow<SelectedOrderState>(SelectedOrderState.None)
    val selectedOrderState: StateFlow<SelectedOrderState> = _selectedOrderState.asStateFlow()

    init {
        loadOrders()
        // EventBus subscriptions removed as part of State Sync Migration
    }

    /**
     * Load orders for the current page with optional status filter
     */
    fun loadOrders(refresh: Boolean = false) {
        val currentState = _uiState.value
        
        if (refresh) {
            _uiState.value = currentState.copy(
                isLoading = true,
                error = null,
                currentPage = 1,
                orders = emptyList()
            )
        } else if (currentState.isLoading) {
            return // Already loading
        }

        _uiState.value = _uiState.value.copy(isLoading = true, error = null)

        viewModelScope.launch {
            try {
                val result = getOrderHistoryUseCase(
                    page = _uiState.value.currentPage,
                    limit = PAGE_SIZE,
                    status = _uiState.value.selectedStatusFilter.takeIf { it != "all" }
                )

                result.fold(
                    onSuccess = { response ->
                        val newState = if (refresh || _uiState.value.currentPage == 1) {
                            // First page or refresh - replace existing orders
                            _uiState.value.copy(
                                orders = response.orders,
                                isLoading = false,
                                error = null,
                                hasNextPage = response.pagination.hasNextPage,
                                totalItems = response.pagination.totalItems
                            )
                        } else {
                            // Load more - append to existing orders
                            _uiState.value.copy(
                                orders = _uiState.value.orders + response.orders,
                                isLoading = false,
                                error = null,
                                hasNextPage = response.pagination.hasNextPage,
                                totalItems = response.pagination.totalItems
                            )
                        }
                        _uiState.value = newState
                    },
                    onFailure = { error ->
                        _uiState.value = _uiState.value.copy(
                            isLoading = false,
                            error = error.message ?: "Failed to load orders"
                        )
                    }
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = e.message ?: "Unexpected error occurred"
                )
            }
        }
    }

    /**
     * Load more orders (next page)
     */
    fun loadMoreOrders() {
        val currentState = _uiState.value
        if (currentState.isLoading || !currentState.hasNextPage) {
            return
        }

        _uiState.value = currentState.copy(currentPage = currentState.currentPage + 1)
        loadOrders()
    }

    /**
     * Filter orders by status
     */
    fun filterByStatus(status: String) {
        _uiState.value = _uiState.value.copy(
            selectedStatusFilter = status,
            currentPage = 1,
            orders = emptyList()
        )
        loadOrders()
    }

    /**
     * Get detailed information for a specific order
     */
    fun getOrderDetails(orderId: String) {
        if (_selectedOrderState.value is SelectedOrderState.Loading) {
            return // Already loading
        }

        _selectedOrderState.value = SelectedOrderState.Loading

        viewModelScope.launch {
            try {
                val result = getOrderDetailsUseCase(orderId)
                
                result.fold(
                    onSuccess = { order ->
                        _selectedOrderState.value = SelectedOrderState.Success(order)
                    },
                    onFailure = { error ->
                        _selectedOrderState.value = SelectedOrderState.Error(
                            error.message ?: "Failed to load order details"
                        )
                    }
                )
            } catch (e: Exception) {
                _selectedOrderState.value = SelectedOrderState.Error(
                    e.message ?: "Unexpected error occurred"
                )
            }
        }
    }

    /**
     * Clear selected order details
     */
    fun clearSelectedOrder() {
        _selectedOrderState.value = SelectedOrderState.None
    }

    /**
     * Update order status in the list (for real-time updates)
     */
    private fun updateOrderStatus(orderId: String, newStatus: String) {
        val updatedOrders = _uiState.value.orders.map { order ->
            if (order.id == orderId) {
                order.copy(status = newStatus)
            } else {
                order
            }
        }
        _uiState.value = _uiState.value.copy(orders = updatedOrders)
    }

    /**
     * Retry loading orders on error
     */
    fun retryLoadOrders() {
        loadOrders(refresh = true)
    }

    companion object {
        private const val PAGE_SIZE = 10
    }
}

/**
 * UI state for order history screen
 */
data class OrderHistoryUiState(
    val orders: List<OrderDTO> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null,
    val selectedStatusFilter: String = "all",
    val currentPage: Int = 1,
    val hasNextPage: Boolean = false,
    val totalItems: Int = 0
)

/**
 * Sealed class representing selected order details state
 */
sealed class SelectedOrderState {
    object None : SelectedOrderState()
    object Loading : SelectedOrderState()
    data class Success(val order: OrderDTO) : SelectedOrderState()
    data class Error(val message: String) : SelectedOrderState()
}