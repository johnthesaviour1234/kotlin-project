package com.grocery.customer.ui.viewmodels

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import com.grocery.customer.data.remote.dto.Product
import com.grocery.customer.domain.repository.ProductRepository
import com.grocery.customer.util.Resource
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * ViewModel for ProductListFragment
 * Handles product filtering by category and search functionality
 */
@HiltViewModel
class ProductListViewModel @Inject constructor(
    private val productRepository: ProductRepository,
    private val savedStateHandle: SavedStateHandle
) : BaseViewModel() {

    private val _products = MutableLiveData<Resource<List<Product>>>()
    val products: LiveData<Resource<List<Product>>> = _products

    private val _currentCategory = MutableLiveData<String?>()
    val currentCategory: LiveData<String?> = _currentCategory

    private val _searchQuery = MutableLiveData<String?>()
    val searchQuery: LiveData<String?> = _searchQuery

    fun loadProductsByCategory(categoryId: String) {
        _currentCategory.value = categoryId
        viewModelScope.launch {
            _products.value = Resource.Loading()
            try {
                val result = productRepository.getProductsByCategory(categoryId)
                result.fold(
                    onSuccess = { productsPayload ->
                        _products.value = Resource.Success(productsPayload.items)
                    },
                    onFailure = { exception ->
                        _products.value = Resource.Error(
                            exception.message ?: "Failed to load products"
                        )
                    }
                )
            } catch (exception: Exception) {
                _products.value = Resource.Error(
                    exception.message ?: "Unknown error occurred"
                )
            }
        }
    }

    fun searchProducts(query: String) {
        _searchQuery.value = query
        if (query.isBlank()) {
            // If search is cleared, reload category products
            _currentCategory.value?.let { categoryId ->
                loadProductsByCategory(categoryId)
            }
            return
        }

        viewModelScope.launch {
            _products.value = Resource.Loading()
            try {
                val result = productRepository.searchProducts(query)
                result.fold(
                    onSuccess = { productsPayload ->
                        _products.value = Resource.Success(productsPayload.items)
                    },
                    onFailure = { exception ->
                        _products.value = Resource.Error(
                            exception.message ?: "Search failed"
                        )
                    }
                )
            } catch (exception: Exception) {
                _products.value = Resource.Error(
                    exception.message ?: "Unknown error occurred"
                )
            }
        }
    }

    fun loadAllProducts() {
        viewModelScope.launch {
            _products.value = Resource.Loading()
            try {
                val result = productRepository.getAllProducts()
                result.fold(
                    onSuccess = { productsPayload ->
                        _products.value = Resource.Success(productsPayload.items)
                    },
                    onFailure = { exception ->
                        _products.value = Resource.Error(
                            exception.message ?: "Failed to load products"
                        )
                    }
                )
            } catch (exception: Exception) {
                _products.value = Resource.Error(
                    exception.message ?: "Unknown error occurred"
                )
            }
        }
    }

    fun refreshProducts() {
        _currentCategory.value?.let { categoryId ->
            loadProductsByCategory(categoryId)
        } ?: loadAllProducts()
    }
}