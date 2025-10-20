package com.grocery.customer.domain.repository

import com.grocery.customer.data.remote.dto.CategoriesPayload
import com.grocery.customer.data.remote.dto.Product
import com.grocery.customer.data.remote.dto.ProductDetail
import com.grocery.customer.data.remote.dto.ProductsListPayload

/**
 * Repository interface for product-related operations
 * Defines contracts for data access layer
 */
interface ProductRepository {
    
    /**
     * Get featured products for home screen
     */
    suspend fun getFeaturedProducts(limit: Int = 10): Result<List<Product>>
    
    /**
     * Get all product categories
     */
    suspend fun getCategories(): Result<CategoriesPayload>
    
    /**
     * Get products by category
     */
    suspend fun getProductsByCategory(
        categoryId: String,
        page: Int = 1,
        limit: Int = 20
    ): Result<ProductsListPayload>
    
    /**
     * Search products by query
     */
    suspend fun searchProducts(
        query: String,
        page: Int = 1,
        limit: Int = 20
    ): Result<ProductsListPayload>
    
    /**
     * Get detailed product information
     */
    suspend fun getProductDetail(productId: String): Result<ProductDetail>
    
    /**
     * Get all products with optional filtering
     */
    suspend fun getAllProducts(
        featured: Boolean? = null,
        category: String? = null,
        query: String? = null,
        page: Int = 1,
        limit: Int = 20
    ): Result<ProductsListPayload>
}