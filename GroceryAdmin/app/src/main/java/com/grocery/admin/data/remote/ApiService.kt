package com.grocery.admin.data.remote

import com.grocery.admin.data.remote.dto.*
import retrofit2.http.*

/**
 * Retrofit API service interface for communicating with the Vercel backend.
 * Defines all API endpoints used by the Admin app.
 */
interface ApiService {
    
    // ===== Authentication =====
    @POST("admin/auth/login")
    suspend fun login(@Body request: LoginRequest): ApiResponse<LoginResponse>
    
    @POST("admin/auth/register")
    suspend fun register(@Body request: RegisterRequest): ApiResponse<RegisterResponse>
    
    // ===== Dashboard =====
    @GET("admin/dashboard/metrics")
    suspend fun getDashboardMetrics(
        @Query("range") range: String = "7d"
    ): ApiResponse<DashboardMetrics>
    
    // ===== Orders Management =====
    @GET("admin/orders")
    suspend fun getOrders(
        @Query("page") page: Int = 1,
        @Query("limit") limit: Int = 20,
        @Query("status") status: String? = null
    ): ApiResponse<OrdersListResponse>
    
    @GET("admin/orders/{id}")
    suspend fun getOrderById(
        @Path("id") orderId: String
    ): ApiResponse<OrderDto>
    
    @PUT("admin/orders/{id}/status")
    suspend fun updateOrderStatus(
        @Path("id") orderId: String,
        @Body request: UpdateOrderStatusRequest
    ): ApiResponse<OrderDto>
    
    @POST("admin/orders/assign")
    suspend fun assignOrder(
        @Body request: AssignOrderRequest
    ): ApiResponse<AssignOrderResponse>
    
    // ===== Delivery Personnel =====
    @GET("admin/delivery-personnel")
    suspend fun getDeliveryPersonnel(
        @Query("active_only") activeOnly: Boolean = true
    ): ApiResponse<DeliveryPersonnelListResponse>
    
    // ===== Products Management =====
    @GET("admin/products")
    suspend fun getProducts(
        @Query("page") page: Int = 1,
        @Query("limit") limit: Int = 20
    ): ApiResponse<ProductsListResponse>
    
    @GET("admin/products/{id}")
    suspend fun getProductById(
        @Path("id") productId: String
    ): ApiResponse<ProductDto>
    
    @POST("admin/products")
    suspend fun createProduct(
        @Body request: CreateProductRequest
    ): ApiResponse<ProductDto>
    
    @PUT("admin/products/{id}")
    suspend fun updateProduct(
        @Path("id") productId: String,
        @Body request: UpdateProductRequest
    ): ApiResponse<ProductDto>
    
    @DELETE("admin/products/{id}")
    suspend fun deleteProduct(
        @Path("id") productId: String
    ): ApiResponse<DeleteProductResponse>
    
    // ===== Product Categories =====
    @GET("products/categories")
    suspend fun getProductCategories(): ApiResponse<ProductCategoriesResponse>
    
    // ===== Inventory Management =====
    @GET("admin/inventory")
    suspend fun getInventory(
        @Query("low_stock") lowStock: Boolean? = null,
        @Query("threshold") threshold: Int = 10
    ): ApiResponse<InventoryListResponse>
    
    @PUT("admin/inventory")
    suspend fun updateInventory(
        @Body request: UpdateInventoryRequest
    ): ApiResponse<UpdateInventoryResponse>
}
