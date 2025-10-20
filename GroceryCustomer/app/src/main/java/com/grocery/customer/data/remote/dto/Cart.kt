package com.grocery.customer.data.remote.dto

/**
 * Cart item representation
 */
data class CartItem(
    val id: String,
    val product: Product,
    val quantity: Int,
    val price: Double, // Price at time of adding to cart
    val addedAt: String? = null
) {
    val totalPrice: Double
        get() = price * quantity
}

/**
 * Shopping cart representation
 */
data class Cart(
    val items: List<CartItem> = emptyList(),
    val totalItems: Int = items.sumOf { it.quantity },
    val totalPrice: Double = items.sumOf { it.totalPrice }
) {
    fun isEmpty(): Boolean = items.isEmpty()
}

/**
 * Request to add item to cart
 */
data class AddToCartRequest(
    val productId: String,
    val quantity: Int
)

/**
 * Request to update cart item quantity
 */
data class UpdateCartItemRequest(
    val cartItemId: String,
    val quantity: Int
)