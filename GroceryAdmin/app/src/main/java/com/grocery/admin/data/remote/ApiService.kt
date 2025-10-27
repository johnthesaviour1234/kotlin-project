package com.grocery.admin.data.remote

import com.grocery.admin.data.remote.dto.*
import retrofit2.http.*

/**
 * Retrofit API service interface for communicating with the Vercel backend.
 * Defines all API endpoints used by the Admin app.
 */
interface ApiService {
    
    // ===== Authentication =====
    @POST("api/admin/auth/login")
    suspend fun login(@Body request: LoginRequest): ApiResponse<LoginResponse>
    
    // ===== Dashboard =====
    @GET("api/admin/dashboard/metrics")
    suspend fun getDashboardMetrics(
        @Query("range") range: String = "7d"
    ): ApiResponse<DashboardMetrics>
    
    // ===== Orders Management =====
    @GET("api/admin/orders")
    suspend fun getOrders(
        @Query("page") page: Int = 1,
        @Query("limit") limit: Int = 20,
        @Query("status") status: String? = null
    ): ApiResponse<OrdersListResponse>
    
    @GET("api/admin/orders/{id}")
    suspend fun getOrderById(
        @Path("id") orderId: String
    ): ApiResponse<OrderDto>
    
    @PUT("api/admin/orders/{id}/status")
    suspend fun updateOrderStatus(
        @Path("id") orderId: String,
        @Body request: UpdateOrderStatusRequest
    ): ApiResponse<OrderDto>
    
    @POST("api/admin/orders/assign")
    suspend fun assignOrder(
        @Body request: AssignOrderRequest
    ): ApiResponse<AssignOrderResponse>
    
    // ===== Products Management =====
    @GET("api/admin/products")
    suspend fun getProducts(
        @Query("page") page: Int = 1,
        @Query("limit") limit: Int = 20
    ): ApiResponse<ProductsListResponse>
    
    @GET("api/admin/products/{id}")
    suspend fun getProductById(
        @Path("id") productId: String
    ): ApiResponse<ProductDto>
    
    @POST("api/admin/products")
    suspend fun createProduct(
        @Body request: CreateProductRequest
    ): ApiResponse<ProductDto>
    
    @PUT("api/admin/products/{id}")
    suspend fun updateProduct(
        @Path("id") productId: String,
        @Body request: UpdateProductRequest
    ): ApiResponse<ProductDto>
    
    @DELETE("api/admin/products/{id}")
    suspend fun deleteProduct(
        @Path("id") productId: String
    ): ApiResponse<DeleteProductResponse>
    
    // ===== Product Categories =====
    @GET("api/products/categories")
    suspend fun getProductCategories(): ApiResponse<List<ProductCategoryDto>>
    
    // ===== Inventory Management =====
    @GET("api/admin/inventory")
    suspend fun getInventory(
        @Query("low_stock") lowStock: Boolean? = null,
        @Query("threshold") threshold: Int = 10
    ): ApiResponse<InventoryListResponse>
    
    @PUT("api/admin/inventory")
    suspend fun updateInventory(
        @Body request: UpdateInventoryRequest
    ): ApiResponse<UpdateInventoryResponse>
}
