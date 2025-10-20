package com.grocery.customer.ui.viewmodels

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.grocery.customer.data.remote.dto.ProductCategory
import com.grocery.customer.domain.repository.ProductRepository
import com.grocery.customer.util.Resource
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * ViewModel for CategoriesFragment
 * Handles business logic and data management for categories screen
 */
@HiltViewModel
class CategoriesViewModel @Inject constructor(
    private val productRepository: ProductRepository
) : BaseViewModel() {

    private val _categories = MutableLiveData<Resource<List<ProductCategory>>>()
    val categories: LiveData<Resource<List<ProductCategory>>> = _categories

    init {
        loadCategories()
    }

    fun loadCategories() {
        viewModelScope.launch {
            _categories.value = Resource.Loading()
            try {
                val result = productRepository.getCategories()
                result.fold(
                    onSuccess = { categoryPayload ->
                        _categories.value = Resource.Success(categoryPayload.items)
                    },
                    onFailure = { exception ->
                        _categories.value = Resource.Error(
                            exception.message ?: "Failed to load categories"
                        )
                    }
                )
            } catch (exception: Exception) {
                _categories.value = Resource.Error(
                    exception.message ?: "Unknown error occurred"
                )
            }
        }
    }

    fun refreshCategories() {
        loadCategories()
    }
}