package com.grocery.admin.ui.viewmodels

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.grocery.admin.data.remote.dto.*
import com.grocery.admin.domain.repository.ProductsRepository
import com.grocery.admin.util.Resource
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ProductsViewModel @Inject constructor(
    private val productsRepository: ProductsRepository
) : BaseViewModel() {
    
    private val _products = MutableLiveData<Resource<List<ProductDto>>>()
    val products: LiveData<Resource<List<ProductDto>>> = _products
    
    private val _categories = MutableLiveData<Resource<List<ProductCategoryDto>>>()
    val categories: LiveData<Resource<List<ProductCategoryDto>>> = _categories
    
    private val _createProductResult = MutableLiveData<Resource<ProductDto>>()
    val createProductResult: LiveData<Resource<ProductDto>> = _createProductResult
    
    private val _updateProductResult = MutableLiveData<Resource<ProductDto>>()
    val updateProductResult: LiveData<Resource<ProductDto>> = _updateProductResult
    
    private val _deleteProductResult = MutableLiveData<Resource<DeleteProductResponse>>()
    val deleteProductResult: LiveData<Resource<DeleteProductResponse>> = _deleteProductResult
    
    private val _isRefreshing = MutableLiveData<Boolean>()
    val isRefreshing: LiveData<Boolean> = _isRefreshing
    
    private var currentPage = 1
    private var searchJob: Job? = null
    private var allProducts: List<ProductDto> = emptyList()
    
    init {
        loadProducts()
        loadCategories()
    }
    
    fun loadProducts(page: Int = 1) {
        currentPage = page
        
        viewModelScope.launch {
            productsRepository.getProducts(page = page, limit = 50)
                .onEach { result ->
                    when (result) {
                        is Resource.Loading -> {
                            _products.value = Resource.Loading()
                        }
                        is Resource.Success -> {
                            allProducts = result.data?.items ?: emptyList()
                            _products.value = Resource.Success(allProducts)
                            _isRefreshing.value = false
                        }
                        is Resource.Error -> {
                            _products.value = Resource.Error(result.message ?: "Unknown error")
                            _isRefreshing.value = false
                        }
                    }
                }
                .launchIn(viewModelScope)
        }
    }
    
    fun loadCategories() {
        viewModelScope.launch {
            productsRepository.getProductCategories()
                .onEach { result ->
                    _categories.value = result
                }
                .launchIn(viewModelScope)
        }
    }
    
    fun searchProducts(query: String) {
        searchJob?.cancel()
        searchJob = viewModelScope.launch {
            delay(300) // Debounce
            if (query.isEmpty()) {
                _products.value = Resource.Success(allProducts)
            } else {
                val filtered = allProducts.filter { product ->
                    product.name.contains(query, ignoreCase = true) ||
                    product.description?.contains(query, ignoreCase = true) == true
                }
                _products.value = Resource.Success(filtered)
            }
        }
    }
    
    fun filterByCategory(categoryId: String?) {
        if (categoryId == null) {
            _products.value = Resource.Success(allProducts)
        } else {
            val filtered = allProducts.filter { it.categoryId == categoryId }
            _products.value = Resource.Success(filtered)
        }
    }
    
    fun filterByActive() {
        val filtered = allProducts.filter { it.isActive }
        _products.value = Resource.Success(filtered)
    }
    
    fun filterByInactive() {
        val filtered = allProducts.filter { !it.isActive }
        _products.value = Resource.Success(filtered)
    }
    
    fun filterByFeatured() {
        val filtered = allProducts.filter { it.featured }
        _products.value = Resource.Success(filtered)
    }
    
    fun filterByLowStock(threshold: Int = 10) {
        val filtered = allProducts.filter { product ->
            product.inventory?.stock?.let { it < threshold } == true
        }
        _products.value = Resource.Success(filtered)
    }
    
    fun showAllProducts() {
        _products.value = Resource.Success(allProducts)
    }
    
    fun refreshProducts() {
        _isRefreshing.value = true
        loadProducts(page = currentPage)
    }
    
    fun createProduct(request: CreateProductRequest) {
        viewModelScope.launch {
            productsRepository.createProduct(request)
                .onEach { result ->
                    _createProductResult.value = result
                    if (result is Resource.Success) {
                        // Refresh products list
                        loadProducts()
                    }
                }
                .launchIn(viewModelScope)
        }
    }
    
    fun updateProduct(productId: String, request: UpdateProductRequest) {
        viewModelScope.launch {
            productsRepository.updateProduct(productId, request)
                .onEach { result ->
                    _updateProductResult.value = result
                    if (result is Resource.Success) {
                        // Refresh products list
                        loadProducts()
                    }
                }
                .launchIn(viewModelScope)
        }
    }
    
    fun deleteProduct(productId: String) {
        viewModelScope.launch {
            productsRepository.deleteProduct(productId)
                .onEach { result ->
                    _deleteProductResult.value = result
                    if (result is Resource.Success) {
                        // Refresh products list
                        loadProducts()
                    }
                }
                .launchIn(viewModelScope)
        }
    }
    
    fun resetCreateProductResult() {
        _createProductResult.value = null
    }
    
    fun resetUpdateProductResult() {
        _updateProductResult.value = null
    }
    
    fun resetDeleteProductResult() {
        _deleteProductResult.value = null
    }
}
