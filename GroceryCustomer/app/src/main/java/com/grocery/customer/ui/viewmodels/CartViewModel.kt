package com.grocery.customer.ui.viewmodels

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.asLiveData
import androidx.lifecycle.viewModelScope
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
    private val cartRepository: CartRepository
) : BaseViewModel() {
    
    // Cart data from repository as LiveData
    val cart: LiveData<Cart> = cartRepository.getCart().asLiveData()
    
    private val _updateCartState = MutableLiveData<Resource<Unit>?>()
    val updateCartState: LiveData<Resource<Unit>?> = _updateCartState
    
    private val _removeItemState = MutableLiveData<Resource<Unit>?>()
    val removeItemState: LiveData<Resource<Unit>?> = _removeItemState
    
    private val _clearCartState = MutableLiveData<Resource<Unit>?>()
    val clearCartState: LiveData<Resource<Unit>?> = _clearCartState
    
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
}