package com.grocery.customer.ui.viewmodels

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.asLiveData
import androidx.lifecycle.viewModelScope
import com.grocery.customer.data.local.Event
import com.grocery.customer.data.local.EventBus
import com.grocery.customer.data.remote.dto.Cart
import com.grocery.customer.domain.repository.CartRepository
import com.grocery.customer.util.Resource
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * ViewModel for CartFragment
 * Manages cart state and cart operations
 */
@HiltViewModel
class CartViewModel @Inject constructor(
    private val cartRepository: CartRepository,
    private val eventBus: EventBus
) : BaseViewModel() {
    
    private val TAG = "CartViewModel"
    
    // Cart data from repository as LiveData
    val cart: LiveData<Cart> = cartRepository.getCart().asLiveData()
    
    private val _updateCartState = MutableLiveData<Resource<Unit>?>()
    val updateCartState: LiveData<Resource<Unit>?> = _updateCartState
    
    private val _removeItemState = MutableLiveData<Resource<Unit>?>()
    val removeItemState: LiveData<Resource<Unit>?> = _removeItemState
    
    private val _clearCartState = MutableLiveData<Resource<Unit>?>()
    val clearCartState: LiveData<Resource<Unit>?> = _clearCartState

    init {
        // Subscribe to product stock changes to update cart availability
        viewModelScope.launch {
            eventBus.subscribe<Event.ProductStockChanged>().collect { event ->
                // Refresh cart to get updated availability
                refreshCart()
            }
        }

        // Subscribe to product out of stock events
        viewModelScope.launch {
            eventBus.subscribe<Event.ProductOutOfStock>().collect { event ->
                // Refresh cart immediately when product goes out of stock
                refreshCart()
            }
        }

        // Subscribe to realtime cart update events from other devices
        viewModelScope.launch {
            eventBus.subscribe<Event.CartUpdated>().collect {
                // Refresh cart when updated from another device
                refreshCart()
            }
        }

        viewModelScope.launch {
            eventBus.subscribe<Event.CartItemAdded>().collect { event ->
                Log.d(TAG, "CartItemAdded event received: productId=${event.productId}, quantity=${event.quantity}")
                // Refresh cart when item added from another device
                refreshCart()
            }
        }

        viewModelScope.launch {
            eventBus.subscribe<Event.CartItemQuantityChanged>().collect { event ->
                // Refresh cart when quantity changed from another device
                refreshCart()
            }
        }

        viewModelScope.launch {
            eventBus.subscribe<Event.CartItemRemoved>().collect { event ->
                // Refresh cart when item removed from another device
                refreshCart()
            }
        }

        // Subscribe to cart cleared events
        viewModelScope.launch {
            eventBus.subscribe<Event.CartCleared>().collect {
                // Refresh cart when cleared from another device/session
                refreshCart()
            }
        }
    }
    
    /**
     * Update quantity of a cart item
     */
    fun updateCartItemQuantity(cartItemId: String, quantity: Int) {
        viewModelScope.launch {
            _updateCartState.value = Resource.Loading()
            try {
                val result = cartRepository.updateCartItem(cartItemId, quantity)
                result.fold(
                    onSuccess = { 
                        _updateCartState.value = Resource.Success(Unit)
                        // Cart will be updated automatically via repository Flow
                    },
                    onFailure = { exception ->
                        _updateCartState.value = Resource.Error(
                            exception.message ?: "Failed to update cart item"
                        )
                    }
                )
            } catch (exception: Exception) {
                _updateCartState.value = Resource.Error(
                    exception.message ?: "Unknown error occurred"
                )
            }
        }
    }
    
    /**
     * Remove item from cart
     */
    fun removeCartItem(cartItemId: String) {
        viewModelScope.launch {
            _removeItemState.value = Resource.Loading()
            try {
                val result = cartRepository.removeFromCart(cartItemId)
                result.fold(
                    onSuccess = { 
                        _removeItemState.value = Resource.Success(Unit)
                        // ✅ Publish item removed event
                        eventBus.publish(Event.ItemRemovedFromCart(cartItemId))
                    },
                    onFailure = { exception ->
                        _removeItemState.value = Resource.Error(
                            exception.message ?: "Failed to remove item from cart"
                        )
                    }
                )
            } catch (exception: Exception) {
                _removeItemState.value = Resource.Error(
                    exception.message ?: "Unknown error occurred"
                )
            }
        }
    }
    
    /**
     * Clear all items from cart
     */
    fun clearCart() {
        viewModelScope.launch {
            _clearCartState.value = Resource.Loading()
            try {
                val result = cartRepository.clearCart()
                result.fold(
                    onSuccess = { 
                        _clearCartState.value = Resource.Success(Unit)
                        // Cart will be updated automatically via repository Flow
                    },
                    onFailure = { exception ->
                        _clearCartState.value = Resource.Error(
                            exception.message ?: "Failed to clear cart"
                        )
                    }
                )
            } catch (exception: Exception) {
                _clearCartState.value = Resource.Error(
                    exception.message ?: "Unknown error occurred"
                )
            }
        }
    }
    
    /**
     * Reset update cart state
     */
    fun resetUpdateCartState() {
        _updateCartState.value = null
    }
    
    /**
     * Reset remove item state
     */
    fun resetRemoveItemState() {
        _removeItemState.value = null
    }
    
    /**
     * Reset clear cart state
     */
    fun resetClearCartState() {
        _clearCartState.value = null
    }
    
    /**
     * Get total items count
     */
    fun getTotalItemsCount(): Int {
        return cart.value?.totalItems ?: 0
    }
    
    /**
     * Get total price
     */
    fun getTotalPrice(): Double {
        return cart.value?.totalPrice ?: 0.0
    }
    
    /**
     * Check if cart is empty
     */
    fun isCartEmpty(): Boolean {
        return cart.value?.isEmpty() ?: true
    }
    
    /**
     * Refresh cart from server
     */
    fun refreshCart() {
        viewModelScope.launch {
            try {
                Log.d(TAG, "Refreshing cart from server...")
                cartRepository.refreshCart()
                Log.d(TAG, "Cart refreshed successfully")
            } catch (exception: Exception) {
                Log.e(TAG, "Error refreshing cart: ${exception.message}", exception)
                // Log error but don't show to user as this is a background operation
                // The cart LiveData will still reflect the current state
            }
        }
    }
}
