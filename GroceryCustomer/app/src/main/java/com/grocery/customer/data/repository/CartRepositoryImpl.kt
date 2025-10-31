package com.grocery.customer.data.repository

import android.util.Log
import com.grocery.customer.data.local.TokenStore
import com.grocery.customer.data.remote.ApiService
import com.grocery.customer.data.remote.dto.*
import com.grocery.customer.domain.exceptions.TokenExpiredException
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
    private val apiService: ApiService,
    private val tokenStore: TokenStore
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
            Log.d("CartRepository", "Loading cart from API...")
            val response = apiService.getCart()
            if (response.code() == 401) {
                Log.e("CartRepository", "401 Unauthorized - Token expired")
                tokenStore.clear()
                throw TokenExpiredException("Session expired. Please login again.")
            }
            if (response.isSuccessful) {
                val apiItems = response.body()?.data?.items ?: emptyList()
                Log.d("CartRepository", "API returned ${apiItems.size} cart items")
                
                val cartItems = apiItems.map { apiItem ->
                    // If product name is missing, try to fetch it from product repository
                    val productName = if (apiItem.product_name.isNullOrBlank()) {
                        try {
                            val productResult = productRepository.getProductDetail(apiItem.product_id)
                            productResult.getOrNull()?.name ?: "Unknown Product"
                        } catch (e: Exception) {
                            Log.w("CartRepository", "Failed to fetch product name for ${apiItem.product_id}: ${e.message}")
                            "Unknown Product"
                        }
                    } else {
                        apiItem.product_name
                    }
                    
                    CartItem(
                        id = apiItem.id,
                        product = Product(
                            id = apiItem.product_id,
                            name = productName,
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
                Log.d("CartRepository", "Cart state updated with ${cartItems.size} items")
            } else {
                Log.e("CartRepository", "Failed to load cart: ${response.code()} - ${response.message()}")
                // Set empty cart on API error to ensure consistency
                _cartState.value = Cart()
            }
        } catch (e: Exception) {
            Log.e("CartRepository", "Error loading cart", e)
            // Set empty cart on exception to ensure consistency
            _cartState.value = Cart()
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
                    if (response.code() == 401) {
                        tokenStore.clear()
                        throw TokenExpiredException("Session expired. Please login again.")
                    }
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
                
                if (response.code() == 401) {
                    tokenStore.clear()
                    throw TokenExpiredException("Session expired. Please login again.")
                }
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
            
            // Try to remove using the DELETE endpoint first
            try {
                val response = apiService.removeFromCart(cartItem.product.id)
                if (response.code() == 401) {
                    tokenStore.clear()
                    throw TokenExpiredException("Session expired. Please login again.")
                }
                if (response.isSuccessful) {
                    // Reload cart from API to get updated state
                    loadCartFromApi()
                    return Result.success(Unit)
                }
                Log.w("CartRepository", "DELETE endpoint failed with ${response.code()}, trying quantity update to 0")
            } catch (deleteException: Exception) {
                Log.w("CartRepository", "DELETE endpoint threw exception: ${deleteException.message}, trying quantity update to 0")
            }
            
            // Workaround: Use update quantity endpoint to set quantity to 0
            val updateRequest = UpdateCartQuantityRequest(quantity = 0)
            val updateResponse = apiService.updateCartQuantity(cartItem.product.id, updateRequest)
            
            if (updateResponse.code() == 401) {
                tokenStore.clear()
                throw TokenExpiredException("Session expired. Please login again.")
            }
            if (updateResponse.isSuccessful) {
                // Reload cart from API to get updated state
                loadCartFromApi()
                Result.success(Unit)
            } else {
                Log.e("CartRepository", "Failed to remove from cart using both methods: DELETE=${updateResponse.code()}")
                Result.failure(Exception("Failed to remove from cart: ${updateResponse.code()}"))
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
            if (response.code() == 401) {
                tokenStore.clear()
                throw TokenExpiredException("Session expired. Please login again.")
            }
            if (response.isSuccessful) {
                // Update local state immediately
                _cartState.value = Cart()
                // Force refresh from API to ensure consistency
                loadCartFromApi()
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
    
    override suspend fun refreshCart(): Result<Unit> {
        return try {
            Log.d("CartRepository", "Force refreshing cart from backend")
            
            // Clear local state first to ensure clean slate
            _cartState.value = Cart()
            
            // Load fresh data from API
            loadCartFromApi()
            
            Log.d("CartRepository", "Cart refresh completed. Current items: ${_cartState.value.items.size}")
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e("CartRepository", "Error refreshing cart", e)
            Result.failure(e)
        }
    }
}
