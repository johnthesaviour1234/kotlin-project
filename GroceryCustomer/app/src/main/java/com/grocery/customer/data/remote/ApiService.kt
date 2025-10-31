package com.grocery.customer.data.remote

import com.grocery.customer.data.remote.dto.*
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Path
import retrofit2.http.Query

/**
 * Retrofit API service interface for communicating with the Vercel backend.
 * Defines all authentication API endpoints used by the Customer app.
 */
interface ApiService {

    // Health check endpoint
    @GET("health")
    suspend fun getHealthCheck(): Response<ApiResponse<HealthData>>

    // Authentication endpoints
    @POST("auth/login")
    suspend fun login(@Body request: LoginRequest): Response<ApiResponse<LoginResponse>>

    @POST("auth/register")
    suspend fun register(@Body request: RegisterRequest): Response<ApiResponse<RegisterResponse>>

    @POST("auth/verify")
    suspend fun verifyEmail(@Body request: VerifyEmailRequest): Response<ApiResponse<VerifyEmailResponse>>

    @POST("auth/forgot-password")
    suspend fun forgotPassword(@Body request: ForgotPasswordRequest): Response<ApiResponse<Unit>>

    @POST("auth/reset-password")
    suspend fun resetPassword(@Body request: ResetPasswordRequest): Response<ApiResponse<Unit>>

    @POST("auth/resend-verification")
    suspend fun resendVerification(@Body request: ResendVerificationRequest): Response<ApiResponse<Unit>>

    // Product endpoints
    @GET("products/categories")
    suspend fun getProductCategories(): Response<ApiResponse<CategoriesPayload>>

    @GET("products/list")
    suspend fun getProducts(
        @Query("featured") featured: Boolean? = null,
        @Query("category") category: String? = null,
        @Query("q") query: String? = null,
        @Query("page") page: Int? = null,
        @Query("limit") limit: Int? = null
    ): Response<ApiResponse<ProductsListPayload>>

    @GET("products/{id}")
    suspend fun getProductDetail(@Path("id") id: String): Response<ApiResponse<ProductDetail>>

    // User endpoints
    @GET("users/profile")
    suspend fun getUserProfile(): Response<ApiResponse<UserProfile>>

    @PUT("users/profile")
    suspend fun updateUserProfile(
        @Body request: ProfileUpdateRequest
    ): Response<ApiResponse<UserProfile>>
    
    // Address endpoints
    @GET("users/addresses")
    suspend fun getUserAddresses(): Response<ApiResponse<AddressPayload>>
    
    @POST("users/addresses")
    suspend fun createAddress(
        @Body request: CreateAddressRequest
    ): Response<ApiResponse<UserAddress>>
    
    @PUT("users/addresses")
    suspend fun updateAddress(
        @Query("id") addressId: String,
        @Body request: UpdateAddressRequest
    ): Response<ApiResponse<UserAddress>>
    
    @DELETE("users/addresses")
    suspend fun deleteAddress(
        @Query("id") addressId: String
    ): Response<ApiResponse<Map<String, Any>>>
    
    // Password change endpoint
    @PUT("auth/change-password")
    suspend fun changePassword(
        @Body request: ChangePasswordRequest
    ): Response<ApiResponse<Map<String, String>>>

    // Cart endpoints
    @GET("cart")
    suspend fun getCart(): Response<ApiResponse<CartApiResponse>>
    
    @POST("cart")
    suspend fun addToCart(
        @Body request: AddToCartApiRequest
    ): Response<ApiResponse<CartItemApi>>
    
    @PUT("cart/{productId}")
    suspend fun updateCartQuantity(
        @Path("productId") productId: String,
        @Body request: UpdateCartQuantityRequest
    ): Response<ApiResponse<CartItemApi>>
    
    @DELETE("cart/{productId}")
    suspend fun removeFromCart(
        @Path("productId") productId: String
    ): Response<ApiResponse<Map<String, Any>>>
    
    @DELETE("cart")
    suspend fun clearCart(): Response<ApiResponse<Map<String, Any>>>

    // Order endpoints
    @POST("orders/create")
    suspend fun createOrder(
        @Body request: CreateOrderRequest
    ): Response<CreateOrderResponse>

    @GET("orders/{id}")
    suspend fun getOrder(
        @Path("id") orderId: String
    ): Response<OrderResponse>

    @GET("orders/history")
    suspend fun getOrderHistory(
        @Query("page") page: Int = 1,
        @Query("limit") limit: Int = 10,
        @Query("status") status: String? = null
    ): Response<OrderHistoryResponse>
    
    // Driver location tracking endpoint
    @GET("orders/{orderId}/driver-location")
    suspend fun getDriverLocation(
        @Path("orderId") orderId: String
    ): Response<ApiResponse<com.grocery.customer.data.dto.DriverLocationResponse>>

    // State sync endpoints
    @GET("sync/state")
    suspend fun getSyncState(): Response<SyncStateResponse>

    @POST("sync/resolve")
    suspend fun resolveSyncConflict(
        @Body request: SyncResolveRequest
    ): Response<SyncResolveResponse>
}
