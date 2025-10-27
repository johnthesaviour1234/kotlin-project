package com.grocery.admin.data.repository

import android.util.Log
import com.grocery.admin.data.remote.ApiService
import com.grocery.admin.data.remote.dto.*
import com.grocery.admin.domain.repository.ProductsRepository
import com.grocery.admin.util.Resource
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ProductsRepositoryImpl @Inject constructor(
    private val apiService: ApiService
) : ProductsRepository {
    
    companion object {
        private const val TAG = "ProductsRepositoryImpl"
    }
    
    override fun getProducts(page: Int, limit: Int): Flow<Resource<ProductsListResponse>> = flow {
        try {
            emit(Resource.Loading())
            Log.d(TAG, "Fetching products - page: $page, limit: $limit")
            
            val response = apiService.getProducts(page, limit)
            
            if (response.success && response.data != null) {
                Log.d(TAG, "Products fetched successfully: ${response.data.items.size} items")
                emit(Resource.Success(response.data))
            } else {
                val errorMessage = response.error ?: response.message ?: "Failed to fetch products"
                Log.e(TAG, "Failed to fetch products: $errorMessage")
                emit(Resource.Error(errorMessage))
            }
        } catch (e: Exception) {
            val errorMessage = e.localizedMessage ?: "Network error occurred"
            Log.e(TAG, "Error fetching products: $errorMessage", e)
            emit(Resource.Error(errorMessage))
        }
    }
    
    override fun getProductById(productId: String): Flow<Resource<ProductDto>> = flow {
        try {
            emit(Resource.Loading())
            Log.d(TAG, "Fetching product by ID: $productId")
            
            val response = apiService.getProductById(productId)
            
            if (response.success && response.data != null) {
                Log.d(TAG, "Product fetched successfully: ${response.data.name}")
                emit(Resource.Success(response.data))
            } else {
                val errorMessage = response.error ?: response.message ?: "Failed to fetch product"
                Log.e(TAG, "Failed to fetch product: $errorMessage")
                emit(Resource.Error(errorMessage))
            }
        } catch (e: Exception) {
            val errorMessage = e.localizedMessage ?: "Network error occurred"
            Log.e(TAG, "Error fetching product: $errorMessage", e)
            emit(Resource.Error(errorMessage))
        }
    }
    
    override fun createProduct(request: CreateProductRequest): Flow<Resource<ProductDto>> = flow {
        try {
            emit(Resource.Loading())
            Log.d(TAG, "Creating product: ${request.name}")
            
            val response = apiService.createProduct(request)
            
            if (response.success && response.data != null) {
                Log.d(TAG, "Product created successfully: ${response.data.id}")
                emit(Resource.Success(response.data))
            } else {
                val errorMessage = response.error ?: response.message ?: "Failed to create product"
                Log.e(TAG, "Failed to create product: $errorMessage")
                emit(Resource.Error(errorMessage))
            }
        } catch (e: Exception) {
            val errorMessage = e.localizedMessage ?: "Network error occurred"
            Log.e(TAG, "Error creating product: $errorMessage", e)
            emit(Resource.Error(errorMessage))
        }
    }
    
    override fun updateProduct(productId: String, request: UpdateProductRequest): Flow<Resource<ProductDto>> = flow {
        try {
            emit(Resource.Loading())
            Log.d(TAG, "Updating product: $productId")
            
            val response = apiService.updateProduct(productId, request)
            
            if (response.success && response.data != null) {
                Log.d(TAG, "Product updated successfully")
                emit(Resource.Success(response.data))
            } else {
                val errorMessage = response.error ?: response.message ?: "Failed to update product"
                Log.e(TAG, "Failed to update product: $errorMessage")
                emit(Resource.Error(errorMessage))
            }
        } catch (e: Exception) {
            val errorMessage = e.localizedMessage ?: "Network error occurred"
            Log.e(TAG, "Error updating product: $errorMessage", e)
            emit(Resource.Error(errorMessage))
        }
    }
    
    override fun deleteProduct(productId: String): Flow<Resource<DeleteProductResponse>> = flow {
        try {
            emit(Resource.Loading())
            Log.d(TAG, "Deleting product: $productId")
            
            val response = apiService.deleteProduct(productId)
            
            if (response.success && response.data != null) {
                Log.d(TAG, "Product deleted successfully")
                emit(Resource.Success(response.data))
            } else {
                val errorMessage = response.error ?: response.message ?: "Failed to delete product"
                Log.e(TAG, "Failed to delete product: $errorMessage")
                emit(Resource.Error(errorMessage))
            }
        } catch (e: Exception) {
            val errorMessage = e.localizedMessage ?: "Network error occurred"
            Log.e(TAG, "Error deleting product: $errorMessage", e)
            emit(Resource.Error(errorMessage))
        }
    }
    
    override fun getProductCategories(): Flow<Resource<List<ProductCategoryDto>>> = flow {
        try {
            emit(Resource.Loading())
            Log.d(TAG, "Fetching product categories")
            
            val response = apiService.getProductCategories()
            
            if (response.success && response.data != null) {
                Log.d(TAG, "Product categories fetched successfully: ${response.data.size} categories")
                emit(Resource.Success(response.data))
            } else {
                val errorMessage = response.error ?: response.message ?: "Failed to fetch categories"
                Log.e(TAG, "Failed to fetch categories: $errorMessage")
                emit(Resource.Error(errorMessage))
            }
        } catch (e: Exception) {
            val errorMessage = e.localizedMessage ?: "Network error occurred"
            Log.e(TAG, "Error fetching categories: $errorMessage", e)
            emit(Resource.Error(errorMessage))
        }
    }
}
