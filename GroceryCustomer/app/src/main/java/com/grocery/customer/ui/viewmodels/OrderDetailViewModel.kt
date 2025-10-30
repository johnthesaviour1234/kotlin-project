package com.grocery.customer.ui.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.grocery.customer.data.remote.dto.OrderDTO
import com.grocery.customer.domain.usecase.GetOrderDetailsUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * ViewModel for managing order detail screen.
 * Handles loading and displaying detailed information for a specific order.
 */
@HiltViewModel
class OrderDetailViewModel @Inject constructor(
    private val getOrderDetailsUseCase: GetOrderDetailsUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(OrderDetailUiState())
    val uiState: StateFlow<OrderDetailUiState> = _uiState.asStateFlow()

    private var currentOrderId: String? = null

    // EventBus subscriptions removed as part of State Sync Migration
    // Order updates will be handled via polling in Phase 2

    /**
     * Load detailed information for a specific order
     */
    fun loadOrderDetails(orderId: String) {
        if (_uiState.value.isLoading) {
            return // Already loading
        }

        currentOrderId = orderId

        _uiState.value = _uiState.value.copy(
            isLoading = true,
            error = null
        )

        viewModelScope.launch {
            try {
                val result = getOrderDetailsUseCase(orderId)
                
                result.fold(
                    onSuccess = { order ->
                        _uiState.value = _uiState.value.copy(
                            isLoading = false,
                            error = null,
                            order = order
                        )
                    },
                    onFailure = { error ->
                        _uiState.value = _uiState.value.copy(
                            isLoading = false,
                            error = error.message ?: "Failed to load order details"
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
     * Retry loading order details on error
     */
    fun retryLoadOrderDetails(orderId: String) {
        loadOrderDetails(orderId)
    }

    /**
     * Clear the current order details
     */
    fun clearOrderDetails() {
        currentOrderId = null
        _uiState.value = OrderDetailUiState()
    }

    override fun onCleared() {
        super.onCleared()
        // Clean up subscriptions when ViewModel is destroyed
        clearOrderDetails()
    }
}

/**
 * UI state for order detail screen
 */
data class OrderDetailUiState(
    val order: OrderDTO? = null,
    val isLoading: Boolean = false,
    val error: String? = null,
    val driverLocation: DriverLocation? = null
)

/**
 * Driver location for real-time tracking
 */
data class DriverLocation(
    val latitude: Double,
    val longitude: Double
)
