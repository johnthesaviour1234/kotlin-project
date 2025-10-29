package com.grocery.customer.ui.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.grocery.customer.data.local.Event
import com.grocery.customer.data.local.EventBus
import com.grocery.customer.data.remote.RealtimeManager
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
    private val getOrderDetailsUseCase: GetOrderDetailsUseCase,
    private val eventBus: EventBus,
    private val realtimeManager: RealtimeManager
) : ViewModel() {

    private val _uiState = MutableStateFlow(OrderDetailUiState())
    val uiState: StateFlow<OrderDetailUiState> = _uiState.asStateFlow()

    private var currentOrderId: String? = null

    init {
        // Subscribe to order status changes
        viewModelScope.launch {
            eventBus.subscribe<Event.OrderStatusChanged>().collect { event ->
                if (event.orderId == currentOrderId) {
                    // Update order status in real-time
                    _uiState.value.order?.let { currentOrder ->
                        val updatedOrder = currentOrder.copy(status = event.newStatus)
                        _uiState.value = _uiState.value.copy(order = updatedOrder)
                    }
                }
            }
        }

        // Subscribe to order assignment events
        viewModelScope.launch {
            eventBus.subscribe<Event.OrderAssigned>().collect { event ->
                if (event.orderId == currentOrderId) {
                    // Reload order to get delivery personnel details
                    currentOrderId?.let { loadOrderDetails(it) }
                }
            }
        }

        // Subscribe to delivery status updates
        viewModelScope.launch {
            eventBus.subscribe<Event.DeliveryStatusUpdated>().collect { event ->
                if (event.orderId == currentOrderId) {
                    // Update order status based on delivery status
                    _uiState.value.order?.let { currentOrder ->
                        // Map delivery status to order status if needed
                        val updatedOrder = when (event.status) {
                            "in_transit" -> currentOrder.copy(status = "out_for_delivery")
                            "completed" -> currentOrder.copy(status = "delivered")
                            else -> currentOrder
                        }
                        _uiState.value = _uiState.value.copy(order = updatedOrder)
                    }
                }
            }
        }

        // Subscribe to driver location updates
        viewModelScope.launch {
            eventBus.subscribe<Event.LocationUpdated>().collect { event ->
                // Update driver location for tracking
                _uiState.value = _uiState.value.copy(
                    driverLocation = DriverLocation(
                        latitude = event.latitude,
                        longitude = event.longitude
                    )
                )
            }
        }
    }

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

                        // ✅ Subscribe to real-time updates for this order
                        realtimeManager.subscribeToOrder(orderId)

                        // ✅ If order has assigned driver and is out for delivery, subscribe to location
                        if (order.status == "out_for_delivery" && order.delivery_personnel_id != null) {
                            realtimeManager.subscribeToDriverLocation(orderId, order.delivery_personnel_id!!)
                        }
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
        // Unsubscribe from current order channels
        currentOrderId?.let { orderId ->
            realtimeManager.unsubscribe("order:$orderId")
            realtimeManager.unsubscribe("order:$orderId:tracking")
        }
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
