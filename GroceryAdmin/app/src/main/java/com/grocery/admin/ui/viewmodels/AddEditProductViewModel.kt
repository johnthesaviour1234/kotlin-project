package com.grocery.admin.ui.viewmodels

import android.net.Uri
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.grocery.admin.data.remote.dto.ProductDto
import com.grocery.admin.data.remote.dto.ProductCategoryDto
import com.grocery.admin.data.remote.dto.CreateProductRequest
import com.grocery.admin.data.remote.dto.UpdateProductRequest
import com.grocery.admin.domain.repository.ProductsRepository
import com.grocery.admin.util.Resource
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * ViewModel for AddEditProductFragment.
 * Handles product creation, editing, and category management.
 */
@HiltViewModel
class AddEditProductViewModel @Inject constructor(
    private val productsRepository: ProductsRepository
) : ViewModel() {

    private val _categories = MutableLiveData<Resource<List<ProductCategoryDto>>>()
    val categories: LiveData<Resource<List<ProductCategoryDto>>> = _categories

    private val _product = MutableLiveData<Resource<ProductDto>>()
    val product: LiveData<Resource<ProductDto>> = _product

    private val _saveProductResult = MutableLiveData<Resource<ProductDto>>()
    val saveProductResult: LiveData<Resource<ProductDto>> = _saveProductResult

    /**
     * Load all available categories for product assignment
     */
    fun loadCategories() {
        viewModelScope.launch {
            _categories.value = Resource.Loading()
            productsRepository.getProductCategories().collect { result ->
                _categories.value = result
            }
        }
    }

    /**
     * Load product details for editing
     */
    fun loadProduct(productId: String) {
        viewModelScope.launch {
            _product.value = Resource.Loading()
            productsRepository.getProductById(productId).collect { result ->
                _product.value = result
            }
        }
    }

    /**
     * Create a new product
     */
    fun createProduct(
        name: String,
        description: String,
        price: Double,
        categoryId: String,
        stockQuantity: Int,
        isFeatured: Boolean = false,
        isActive: Boolean = true,
        imageUri: Uri? = null
    ) {
        viewModelScope.launch {
            _saveProductResult.value = Resource.Loading()
            
            // TODO: Implement image upload when available
            // For now, create product without image URL
            val request = CreateProductRequest(
                name = name,
                description = description,
                price = price,
                categoryId = categoryId,
                featured = isFeatured,
                isActive = isActive,
                initialStock = stockQuantity
            )

            productsRepository.createProduct(request).collect { result ->
                _saveProductResult.value = result
            }
        }
    }

    /**
     * Update an existing product
     */
    fun updateProduct(
        productId: String,
        name: String,
        description: String,
        price: Double,
        categoryId: String,
        stockQuantity: Int,
        isFeatured: Boolean = false,
        isActive: Boolean = true,
        imageUri: Uri? = null
    ) {
        viewModelScope.launch {
            _saveProductResult.value = Resource.Loading()
            
            // TODO: Implement image upload when available
            // For now, update product without image URL
            val request = UpdateProductRequest(
                name = name,
                description = description,
                price = price,
                categoryId = categoryId,
                featured = isFeatured,
                isActive = isActive
            )

            productsRepository.updateProduct(productId, request).collect { result ->
                _saveProductResult.value = result
            }
        }
    }

    /**
     * Reset the save product result to clear UI state
     */
    fun resetSaveProductResult() {
        _saveProductResult.value = null
    }

    /**
     * Validate product data before saving
     */
    fun validateProduct(
        name: String,
        description: String,
        price: String,
        stock: String,
        categorySelected: Boolean
    ): ValidationResult {
        val errors = mutableListOf<String>()

        // Name validation
        if (name.trim().isEmpty()) {
            errors.add("Product name is required")
        } else if (name.trim().length < 3) {
            errors.add("Product name must be at least 3 characters")
        }

        // Description validation
        if (description.trim().isEmpty()) {
            errors.add("Product description is required")
        } else if (description.trim().length < 10) {
            errors.add("Product description must be at least 10 characters")
        }

        // Price validation
        if (price.trim().isEmpty()) {
            errors.add("Product price is required")
        } else {
            try {
                val priceValue = price.toDouble()
                if (priceValue <= 0) {
                    errors.add("Product price must be greater than 0")
                }
            } catch (e: NumberFormatException) {
                errors.add("Invalid price format")
            }
        }

        // Stock validation
        if (stock.trim().isEmpty()) {
            errors.add("Stock quantity is required")
        } else {
            try {
                val stockValue = stock.toInt()
                if (stockValue < 0) {
                    errors.add("Stock quantity cannot be negative")
                }
            } catch (e: NumberFormatException) {
                errors.add("Invalid stock quantity format")
            }
        }

        // Category validation
        if (!categorySelected) {
            errors.add("Please select a category")
        }

        return ValidationResult(
            isValid = errors.isEmpty(),
            errors = errors
        )
    }

    /**
     * Data class for validation results
     */
    data class ValidationResult(
        val isValid: Boolean,
        val errors: List<String>
    )
}