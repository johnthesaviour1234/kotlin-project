package com.grocery.admin.domain.repository

import android.content.Context
import android.net.Uri
import com.grocery.admin.data.remote.dto.*
import com.grocery.admin.util.Resource
import kotlinx.coroutines.flow.Flow

interface ProductsRepository {
    fun getProducts(page: Int = 1, limit: Int = 20): Flow<Resource<ProductsListResponse>>
    fun getProductById(productId: String): Flow<Resource<ProductDto>>
    fun createProduct(request: CreateProductRequest): Flow<Resource<ProductDto>>
    fun updateProduct(productId: String, request: UpdateProductRequest): Flow<Resource<ProductDto>>
    fun deleteProduct(productId: String): Flow<Resource<DeleteProductResponse>>
    fun getProductCategories(): Flow<Resource<List<ProductCategoryDto>>>
    fun updateInventoryStock(productId: String, stock: Int): Flow<Resource<Unit>>
    suspend fun uploadProductImage(context: Context, imageUri: Uri, productName: String): Result<String>
}
