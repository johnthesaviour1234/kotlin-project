package com.grocery.customer.ui.viewmodels

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.grocery.customer.data.remote.dto.Product
import com.grocery.customer.data.remote.dto.ProductCategory
import com.grocery.customer.domain.repository.ProductRepository
import com.grocery.customer.util.Resource
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject
import android.util.Log

/**
 * ViewModel for HomeFragment
 * Handles business logic and data management for the home screen
 */
@HiltViewModel
class HomeViewModel @Inject constructor(
    private val productRepository: ProductRepository
) : BaseViewModel() {

    private val _featuredProducts = MutableLiveData<Resource<List<Product>>>()
    val featuredProducts: LiveData<Resource<List<Product>>> = _featuredProducts

    private val _categories = MutableLiveData<Resource<List<ProductCategory>>>()
    val categories: LiveData<Resource<List<ProductCategory>>> = _categories

    companion object {
        private const val TAG = "HomeViewModel"
    }

    init {
        // Load data on initialization
        loadFeaturedProducts()
        loadCategories()
    }

    fun loadFeaturedProducts() {
        Log.d(TAG, "Starting to load featured products...")
        viewModelScope.launch {
            _featuredProducts.value = Resource.Loading()
            try {
                Log.d(TAG, "Calling repository.getFeaturedProducts()")
                val result = productRepository.getFeaturedProducts(limit = 10)
                result.fold(
                    onSuccess = { products ->
                        Log.d(TAG, "Repository returned ${products.size} featured products")
                        _featuredProducts.value = Resource.Success(products)
                    },
                    onFailure = { exception ->
                        Log.e(TAG, "Repository failed with exception: ${exception.message}", exception)
                        _featuredProducts.value = Resource.Error(
                            exception.message ?: "Failed to load featured products"
                        )
                    }
                )
            } catch (exception: Exception) {
                Log.e(TAG, "Exception in loadFeaturedProducts: ${exception.message}", exception)
                _featuredProducts.value = Resource.Error(
                    exception.message ?: "Unknown error occurred"
                )
            }
        }
    }

    fun loadCategories() {
        Log.d(TAG, "Starting to load categories...")
        viewModelScope.launch {
            _categories.value = Resource.Loading()
            try {
                Log.d(TAG, "Calling repository.getCategories()")
                val result = productRepository.getCategories()
                result.fold(
                    onSuccess = { categoryPayload ->
                        Log.d(TAG, "Repository returned ${categoryPayload.items.size} categories")
                        _categories.value = Resource.Success(categoryPayload.items)
                    },
                    onFailure = { exception ->
                        Log.e(TAG, "Categories loading failed with exception: ${exception.message}", exception)
                        _categories.value = Resource.Error(
                            exception.message ?: "Failed to load categories"
                        )
                    }
                )
            } catch (exception: Exception) {
                Log.e(TAG, "Exception in loadCategories: ${exception.message}", exception)
                _categories.value = Resource.Error(
                    exception.message ?: "Unknown error occurred"
                )
            }
        }
    }

    fun searchProducts(query: String) {
        Log.d(TAG, "Starting product search for: $query")
        viewModelScope.launch {
            _featuredProducts.value = Resource.Loading()
            try {
                Log.d(TAG, "Calling repository.searchProducts()")
                val result = productRepository.searchProducts(query, limit = 20)
                result.fold(
                    onSuccess = { productsPayload ->
                        Log.d(TAG, "Search returned ${productsPayload.items.size} products")
                        _featuredProducts.value = Resource.Success(productsPayload.items)
                    },
                    onFailure = { exception ->
                        Log.e(TAG, "Search failed with exception: ${exception.message}", exception)
                        _featuredProducts.value = Resource.Error(
                            exception.message ?: "Search failed"
                        )
                    }
                )
            } catch (exception: Exception) {
                Log.e(TAG, "Exception in searchProducts: ${exception.message}", exception)
                _featuredProducts.value = Resource.Error(
                    exception.message ?: "Unknown error occurred"
                )
            }
        }
    }

    fun refreshData() {
        loadFeaturedProducts()
        loadCategories()
    }
}
