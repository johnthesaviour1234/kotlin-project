package com.grocery.customer.ui.viewmodels

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import com.grocery.customer.data.local.Event
import com.grocery.customer.data.local.EventBus
import com.grocery.customer.data.remote.dto.Inventory
import com.grocery.customer.data.remote.dto.ProductDetail
import com.grocery.customer.domain.repository.CartRepository
import com.grocery.customer.domain.repository.ProductRepository
import com.grocery.customer.util.Resource
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * ViewModel for ProductDetailFragment
 * Handles individual product data and cart operations
 */
@HiltViewModel
class ProductDetailViewModel @Inject constructor(
    private val productRepository: ProductRepository,
    private val cartRepository: CartRepository,
    private val savedStateHandle: SavedStateHandle,
    private val eventBus: EventBus
) : BaseViewModel() {

    private val _productDetail = MutableLiveData<Resource<ProductDetail>>()
    val productDetail: LiveData<Resource<ProductDetail>> = _productDetail

    private val _quantity = MutableLiveData<Int>(1)
    val quantity: LiveData<Int> = _quantity

    private val _addToCartState = MutableLiveData<Resource<Unit>?>()
    val addToCartState: LiveData<Resource<Unit>?> = _addToCartState

    private var currentProductId: String? = null

    init {
        // Subscribe to product stock changes
        viewModelScope.launch {
            eventBus.subscribe<Event.ProductStockChanged>().collect { event ->
                if (event.productId == currentProductId) {
                    // Update stock in real-time
                    _productDetail.value?.data?.let { product ->
                        val updatedInventory = product.inventory?.copy(stock = event.newStock)
                            ?: Inventory(stock = event.newStock, updated_at = null)
                        val updatedProduct = product.copy(inventory = updatedInventory)
                        _productDetail.value = Resource.Success(updatedProduct)

                        // Adjust quantity if current quantity exceeds new stock
                        val currentQty = _quantity.value ?: 1
                        if (currentQty > event.newStock) {
                            _quantity.value = maxOf(1, event.newStock)
                        }
                    }
                }
            }
        }

        // Subscribe to product out of stock events
        viewModelScope.launch {
            eventBus.subscribe<Event.ProductOutOfStock>().collect { event ->
                if (event.productId == currentProductId) {
                    // Update product to show out of stock
                    _productDetail.value?.data?.let { product ->
                        val updatedInventory = product.inventory?.copy(stock = 0)
                            ?: Inventory(stock = 0, updated_at = null)
                        val updatedProduct = product.copy(inventory = updatedInventory)
                        _productDetail.value = Resource.Success(updatedProduct)
                        _quantity.value = 1 // Reset quantity
                    }
                }
            }
        }

        // Subscribe to product price changes
        viewModelScope.launch {
            eventBus.subscribe<Event.ProductPriceChanged>().collect { event ->
                if (event.productId == currentProductId) {
                    // Update price in real-time
                    _productDetail.value?.data?.let { product ->
                        val updatedProduct = product.copy(price = event.newPrice)
                        _productDetail.value = Resource.Success(updatedProduct)
                    }
                }
            }
        }

        // Subscribe to product deleted events
        viewModelScope.launch {
            eventBus.subscribe<Event.ProductDeleted>().collect { event ->
                if (event.productId == currentProductId) {
                    // Show error that product is no longer available
                    _productDetail.value = Resource.Error("This product is no longer available")
                }
            }
        }
    }

    fun loadProductDetail(productId: String) {
        currentProductId = productId
        viewModelScope.launch {
            _productDetail.value = Resource.Loading()
            try {
                val result = productRepository.getProductDetail(productId)
                result.fold(
                    onSuccess = { product ->
                        _productDetail.value = Resource.Success(product)
                    },
                    onFailure = { exception ->
                        _productDetail.value = Resource.Error(
                            exception.message ?: "Failed to load product details"
                        )
                    }
                )
            } catch (exception: Exception) {
                _productDetail.value = Resource.Error(
                    exception.message ?: "Unknown error occurred"
                )
            }
        }
    }

    fun increaseQuantity() {
        val currentQuantity = _quantity.value ?: 1
        val maxStock = _productDetail.value?.data?.inventory?.stock ?: Int.MAX_VALUE
        
        if (currentQuantity < maxStock) {
            _quantity.value = currentQuantity + 1
        }
    }

    fun decreaseQuantity() {
        val currentQuantity = _quantity.value ?: 1
        if (currentQuantity > 1) {
            _quantity.value = currentQuantity - 1
        }
    }

    fun setQuantity(quantity: Int) {
        val maxStock = _productDetail.value?.data?.inventory?.stock ?: Int.MAX_VALUE
        if (quantity in 1..maxStock) {
            _quantity.value = quantity
        }
    }

    fun addToCart() {
        viewModelScope.launch {
            _addToCartState.value = Resource.Loading()
            try {
                val product = _productDetail.value?.data
                val qty = _quantity.value ?: 1
                
                if (product != null) {
                    val result = cartRepository.addToCart(product.id, qty)
                    result.fold(
                        onSuccess = { 
                            _addToCartState.value = Resource.Success(Unit)
                            // ✅ Publish item added to cart event
                            eventBus.publish(
                                Event.ItemAddedToCart(
                                    productId = product.id,
                                    productName = product.name,
                                    quantity = qty
                                )
                            )
                        },
                        onFailure = { exception ->
                            _addToCartState.value = Resource.Error(
                                exception.message ?: "Failed to add to cart"
                            )
                        }
                    )
                } else {
                    _addToCartState.value = Resource.Error("Product not loaded")
                }
            } catch (exception: Exception) {
                _addToCartState.value = Resource.Error(
                    exception.message ?: "Failed to add to cart"
                )
            }
        }
    }

    fun resetAddToCartState() {
        _addToCartState.value = null
    }

    fun isInStock(): Boolean {
        val stock = _productDetail.value?.data?.inventory?.stock ?: 0
        return stock > 0
    }

    fun isQuantityValid(): Boolean {
        val currentQuantity = _quantity.value ?: 1
        val stock = _productDetail.value?.data?.inventory?.stock ?: 0
        return currentQuantity in 1..stock
    }
}