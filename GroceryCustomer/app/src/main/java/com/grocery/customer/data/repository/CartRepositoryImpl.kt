package com.grocery.customer.data.repository

import android.util.Log
import com.grocery.customer.data.remote.ApiService
import com.grocery.customer.data.remote.dto.*
import com.grocery.customer.domain.repository.CartRepository
import com.grocery.customer.domain.repository.ProductRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
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
    private val scope = CoroutineScope(Dispatchers.IO)
    
    init {
        // Load cart from API on initialization
        scope.launch {
            loadCartFromApi()
        }
    }
    
    override fun getCart(): Flow<Cart> = _cartState.asStateFlow()
    
    private suspend fun loadCartFromApi() {
        try {
            val response = apiService.getCart()
            if (response.isSuccessful) {
                val apiItems = response.body()?.data?.data ?: emptyList()
                val cartItems = apiItems.map { apiItem ->
                    CartItem(
                        id = apiItem.id,
                        product = Product(
                            id = apiItem.product_id,
                            name = apiItem.product_name ?: "Unknown Product",
                            price = apiItem.price,
                            image_url = apiItem.image_url ?: "",
                            featured = false, // API doesn't provide this
                            created_at = apiItem.created_at,
                            category_id = "" // API doesn't provide this
                        ),
                        quantity = apiItem.quantity,
                        price = apiItem.price,
                        addedAt = apiItem.created_at
                    )
                }
                _cartState.value = Cart(cartItems)
            } else {
                Log.e("CartRepository", "Failed to load cart: ${response.code()}")
            }
        } catch (e: Exception) {
            Log.e("CartRepository", "Error loading cart", e)
        }
    }
    
    override suspend fun addToCart(productId: String, quantity: Int): Result<Unit> {
        return try {
            // Get product details first
            val productResult = productRepository.getProductDetail(productId)
            
            productResult.fold(
                onSuccess = { productDetail ->
                    // Add to cart via API
                    val addRequest = AddToCartApiRequest(
                        product_id = productId,
                        product_name = productDetail.name,
                        price = productDetail.price,
                        image_url = productDetail.image_url ?: "",
                        quantity = quantity
                    )
                    
                    val response = apiService.addToCart(addRequest)
                    if (response.isSuccessful) {
                        // Reload cart from API to get updated state
                        loadCartFromApi()
                        Result.success(Unit)
                    } else {
                        Log.e("CartRepository", "Failed to add to cart: ${response.code()}")
                        Result.failure(Exception("Failed to add to cart: ${response.code()}"))
                    }
                },
                onFailure = { exception ->
                    Result.failure(exception)
                }
            )
        } catch (e: Exception) {
            Log.e("CartRepository", "Error adding to cart", e)
            Result.failure(e)
        }
    }
    
    override suspend fun updateCartItem(cartItemId: String, quantity: Int): Result<Unit> {
        return try {
            val currentCart = _cartState.value
            val cartItem = currentCart.items.find { it.id == cartItemId }
            
            if (cartItem == null) {
                return Result.failure(Exception("Cart item not found"))
            }
            
            if (quantity <= 0) {
                // Remove item if quantity is 0 or less
                return removeFromCart(cartItemId)
            } else {
                // Update quantity via API
                val updateRequest = UpdateCartQuantityRequest(quantity = quantity)
                val response = apiService.updateCartQuantity(cartItem.product.id, updateRequest)
                
                if (response.isSuccessful) {
                    // Reload cart from API to get updated state
                    loadCartFromApi()
                    Result.success(Unit)
                } else {
                    Log.e("CartRepository", "Failed to update cart item: ${response.code()}")
                    Result.failure(Exception("Failed to update cart item: ${response.code()}"))
                }
            }
        } catch (e: Exception) {
            Log.e("CartRepository", "Error updating cart item", e)
            Result.failure(e)
        }
    }
    
    override suspend fun removeFromCart(cartItemId: String): Result<Unit> {
        return try {
            val currentCart = _cartState.value
            val cartItem = currentCart.items.find { it.id == cartItemId }
            
            if (cartItem == null) {
                return Result.failure(Exception("Cart item not found"))
            }
            
            // Remove from cart via API
            val response = apiService.removeFromCart(cartItem.product.id)
            if (response.isSuccessful) {
                // Reload cart from API to get updated state
                loadCartFromApi()
                Result.success(Unit)
            } else {
                Log.e("CartRepository", "Failed to remove from cart: ${response.code()}")
                Result.failure(Exception("Failed to remove from cart: ${response.code()}"))
            }
        } catch (e: Exception) {
            Log.e("CartRepository", "Error removing from cart", e)
            Result.failure(e)
        }
    }
    
    override suspend fun clearCart(): Result<Unit> {
        return try {
            // Clear cart via API
            val response = apiService.clearCart()
            if (response.isSuccessful) {
                // Update local state
                _cartState.value = Cart()
                Result.success(Unit)
            } else {
                Log.e("CartRepository", "Failed to clear cart: ${response.code()}")
                Result.failure(Exception("Failed to clear cart: ${response.code()}"))
            }
        } catch (e: Exception) {
            Log.e("CartRepository", "Error clearing cart", e)
            Result.failure(e)
        }
    }
    
    override suspend fun getCartItemsCount(): Int {
        return _cartState.value.totalItems
    }
}