package com.grocery.customer.domain.repository

import com.grocery.customer.data.remote.dto.Cart
import com.grocery.customer.data.remote.dto.CartItem
import kotlinx.coroutines.flow.Flow

/**
 * Repository interface for cart operations
 */
interface CartRepository {
    
    /**
     * Get current cart items as a flow
     */
    fun getCart(): Flow<Cart>
    
    /**
     * Add product to cart
     */
    suspend fun addToCart(productId: String, quantity: Int): Result<Unit>
    
    /**
     * Update cart item quantity
     */
    suspend fun updateCartItem(cartItemId: String, quantity: Int): Result<Unit>
    
    /**
     * Remove item from cart
     */
    suspend fun removeFromCart(cartItemId: String): Result<Unit>
    
    /**
     * Clear all items from cart
     */
    suspend fun clearCart(): Result<Unit>
    
    /**
     * Get total items count in cart
     */
    suspend fun getCartItemsCount(): Int
}