package com.grocery.customer.ui.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.grocery.customer.data.remote.dto.*
import com.grocery.customer.domain.usecase.CreateOrderUseCase
import com.grocery.customer.domain.usecase.GetCurrentUserUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * ViewModel for managing checkout process.
 * Handles order creation, delivery address management, and payment processing.
 */
@HiltViewModel
class CheckoutViewModel @Inject constructor(
    private val createOrderUseCase: CreateOrderUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(CheckoutUiState())
    val uiState: StateFlow<CheckoutUiState> = _uiState.asStateFlow()

    private val _orderPlacementState = MutableStateFlow<OrderPlacementState>(OrderPlacementState.Idle)
    val orderPlacementState: StateFlow<OrderPlacementState> = _orderPlacementState.asStateFlow()

    private var cartItems: List<CartItem> = emptyList()

    /**
     * Initialize checkout with cart items
     */
    fun initializeCheckout(items: List<CartItem>) {
        cartItems = items
        val subtotal = items.sumOf { it.totalPrice }
        val taxAmount = subtotal * 0.05 // 5% tax
        val deliveryFee = if (subtotal >= 500.0) 0.0 else 50.0
        val totalAmount = subtotal + taxAmount + deliveryFee

        _uiState.value = _uiState.value.copy(
            cartItems = items,
            subtotal = subtotal,
            taxAmount = taxAmount,
            deliveryFee = deliveryFee,
            totalAmount = totalAmount
        )
    }

    /**
     * Update delivery address
     */
    fun updateDeliveryAddress(address: DeliveryAddressDTO) {
        _uiState.value = _uiState.value.copy(
            deliveryAddress = address,
            isDeliveryAddressValid = isAddressValid(address)
        )
    }

    /**
     * Update payment method
     */
    fun updatePaymentMethod(paymentMethod: String) {
        _uiState.value = _uiState.value.copy(selectedPaymentMethod = paymentMethod)
    }

    /**
     * Update order notes
     */
    fun updateOrderNotes(notes: String) {
        _uiState.value = _uiState.value.copy(orderNotes = notes)
    }

    /**
     * Validate and place the order
     */
    fun placeOrder() {
        val currentState = _uiState.value
        
        if (!validateOrderData(currentState)) {
            return
        }

        _orderPlacementState.value = OrderPlacementState.Loading

        viewModelScope.launch {
            try {
                val orderItems = currentState.cartItems.map { cartItem ->
                    CreateOrderItemRequest(
                        productId = cartItem.product.id,
                        quantity = cartItem.quantity,
                        unitPrice = cartItem.price,
                        totalPrice = cartItem.totalPrice,
                        productName = cartItem.product.name,
                        productImageUrl = cartItem.product.image_url
                    )
                }

                val result = createOrderUseCase(
                    items = orderItems,
                    deliveryAddress = currentState.deliveryAddress!!,
                    paymentMethod = currentState.selectedPaymentMethod,
                    notes = currentState.orderNotes
                )

                result.fold(
                    onSuccess = { order ->
                        _orderPlacementState.value = OrderPlacementState.Success(order)
                    },
                    onFailure = { error ->
                        _orderPlacementState.value = OrderPlacementState.Error(
                            error.message ?: "Failed to place order"
                        )
                    }
                )
            } catch (e: Exception) {
                _orderPlacementState.value = OrderPlacementState.Error(
                    e.message ?: "Unexpected error occurred"
                )
            }
        }
    }

    /**
     * Reset order placement state
     */
    fun resetOrderPlacementState() {
        _orderPlacementState.value = OrderPlacementState.Idle
    }

    /**
     * Validate order data before placing order
     */
    private fun validateOrderData(state: CheckoutUiState): Boolean {
        if (state.cartItems.isEmpty()) {
            _orderPlacementState.value = OrderPlacementState.Error("Cart is empty")
            return false
        }

        if (state.deliveryAddress == null || !isAddressValid(state.deliveryAddress)) {
            _orderPlacementState.value = OrderPlacementState.Error("Please provide a valid delivery address")
            return false
        }

        if (state.selectedPaymentMethod.isBlank()) {
            _orderPlacementState.value = OrderPlacementState.Error("Please select a payment method")
            return false
        }

        return true
    }

    /**
     * Check if delivery address is valid
     */
    private fun isAddressValid(address: DeliveryAddressDTO): Boolean {
        return address.street.isNotBlank() &&
                address.city.isNotBlank() &&
                address.state.isNotBlank() &&
                address.postal_code.isNotBlank()
    }
}

/**
 * UI state for checkout screen
 */
data class CheckoutUiState(
    val cartItems: List<CartItem> = emptyList(),
    val subtotal: Double = 0.0,
    val taxAmount: Double = 0.0,
    val deliveryFee: Double = 0.0,
    val totalAmount: Double = 0.0,
    val deliveryAddress: DeliveryAddressDTO? = null,
    val isDeliveryAddressValid: Boolean = false,
    val selectedPaymentMethod: String = "cash",
    val availablePaymentMethods: List<String> = listOf("cash", "card", "upi"),
    val orderNotes: String = ""
)

/**
 * Sealed class representing order placement states
 */
sealed class OrderPlacementState {
    object Idle : OrderPlacementState()
    object Loading : OrderPlacementState()
    data class Success(val order: OrderDTO) : OrderPlacementState()
    data class Error(val message: String) : OrderPlacementState()
}