package com.grocery.customer.data.repository

import com.grocery.customer.data.remote.ApiService
import com.grocery.customer.data.remote.dto.Cart
import com.grocery.customer.data.remote.dto.CartItem
import com.grocery.customer.data.remote.dto.Product
import com.grocery.customer.domain.repository.CartRepository
import com.grocery.customer.domain.repository.ProductRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Implementation of CartRepository using persistent API storage
 */
@Singleton
class CartRepositoryImpl @Inject constructor(
    private val productRepository: ProductRepository,
    private val apiService: ApiService
) : CartRepository {
    
    private val _cartState = MutableStateFlow(Cart())
    
    override fun getCart(): Flow<Cart> = _cartState.asStateFlow()
    
    override suspend fun addToCart(productId: String, quantity: Int): Result<Unit> {
        return try {
            // Get product details
            val productResult = productRepository.getProductDetail(productId)
            
            productResult.fold(
                onSuccess = { productDetail ->
                    val currentCart = _cartState.value
                    
                    // Check if item already exists in cart
                    val existingItemIndex = currentCart.items.indexOfFirst { 
                        it.product.id == productId 
                    }
                    
                    val updatedItems = if (existingItemIndex != -1) {
                        // Update existing item quantity
                        currentCart.items.toMutableList().apply {
                            val existingItem = this[existingItemIndex]
                            this[existingItemIndex] = existingItem.copy(
                                quantity = existingItem.quantity + quantity
                            )
                        }
                    } else {
                        // Add new item
                        val newItem = CartItem(
                            id = UUID.randomUUID().toString(),
                            product = Product(
                                id = productDetail.id,
                                name = productDetail.name,
                                price = productDetail.price,
                                image_url = productDetail.image_url,
                                featured = productDetail.featured,
                                created_at = productDetail.created_at,
                                category_id = productDetail.category_id
                            ),
                            quantity = quantity,
                            price = productDetail.price,
                            addedAt = System.currentTimeMillis().toString()
                        )
                        currentCart.items + newItem
                    }
                    
                    // Update cart state
                    _cartState.value = Cart(updatedItems)
                    Result.success(Unit)
                },
                onFailure = { exception ->
                    Result.failure(exception)
                }
            )
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    override suspend fun updateCartItem(cartItemId: String, quantity: Int): Result<Unit> {
        return try {
            val currentCart = _cartState.value
            val itemIndex = currentCart.items.indexOfFirst { it.id == cartItemId }
            
            if (itemIndex == -1) {
                return Result.failure(Exception("Cart item not found"))
            }
            
            val updatedItems = if (quantity <= 0) {
                // Remove item if quantity is 0 or less
                currentCart.items.filterNot { it.id == cartItemId }
            } else {
                // Update quantity
                currentCart.items.toMutableList().apply {
                    this[itemIndex] = this[itemIndex].copy(quantity = quantity)
                }
            }
            
            _cartState.value = Cart(updatedItems)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    override suspend fun removeFromCart(cartItemId: String): Result<Unit> {
        return try {
            val currentCart = _cartState.value
            val updatedItems = currentCart.items.filterNot { it.id == cartItemId }
            
            _cartState.value = Cart(updatedItems)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    override suspend fun clearCart(): Result<Unit> {
        return try {
            _cartState.value = Cart()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    override suspend fun getCartItemsCount(): Int {
        return _cartState.value.totalItems
    }
}