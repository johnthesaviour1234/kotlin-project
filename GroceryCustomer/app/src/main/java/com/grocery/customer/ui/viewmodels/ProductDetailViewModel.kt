package com.grocery.customer.ui.viewmodels

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
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
    private val savedStateHandle: SavedStateHandle
) : BaseViewModel() {

    private val _productDetail = MutableLiveData<Resource<ProductDetail>>()
    val productDetail: LiveData<Resource<ProductDetail>> = _productDetail

    private val _quantity = MutableLiveData<Int>(1)
    val quantity: LiveData<Int> = _quantity

    private val _addToCartState = MutableLiveData<Resource<Unit>?>()
    val addToCartState: LiveData<Resource<Unit>?> = _addToCartState

    fun loadProductDetail(productId: String) {
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
                        onSuccess = { _addToCartState.value = Resource.Success(Unit) },
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