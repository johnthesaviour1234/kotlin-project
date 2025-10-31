package com.grocery.delivery.data.api

import com.grocery.delivery.data.dto.*
import retrofit2.Response
import retrofit2.http.*

/**
 * API service interface for delivery operations
 */
interface DeliveryApiService {
    
    /**
     * Authentication endpoints
     */
    @POST("delivery/auth/login")
    suspend fun login(
        @Body request: DeliveryLoginRequest
    ): Response<DeliveryLoginResponse>
    
    @POST("delivery/auth/register")
    suspend fun register(
        @Body request: DeliveryRegisterRequest
    ): Response<DeliveryRegisterResponse>
    
    @POST("delivery/auth/logout")
    suspend fun logout(): Response<Unit>
    
    /**
     * Order management endpoints
     */
    @GET("delivery/available-orders")
    suspend fun getAvailableOrders(): Response<DeliveryOrdersResponse>
    
    @GET("delivery/orders/active")
    suspend fun getActiveOrder(
        @Query("driver_id") driverId: String
    ): Response<OrderActionResponse>
    
    @GET("delivery/orders/history")
    suspend fun getOrderHistory(
        @Query("limit") limit: Int? = 50,
        @Query("offset") offset: Int? = 0
    ): Response<DeliveryOrdersResponse>
    
    @POST("delivery/accept")
    suspend fun acceptOrder(
        @Body request: AcceptOrderRequest
    ): Response<OrderActionResponse>
    
    @POST("delivery/decline")
    suspend fun declineOrder(
        @Body request: DeclineOrderRequest
    ): Response<OrderActionResponse>
    
    @PUT("delivery/update-status")
    suspend fun updateOrderStatus(
        @Body request: UpdateOrderStatusRequest
    ): Response<OrderActionResponse>
    
    /**
     * Location tracking endpoints
     */
    @POST("delivery/update-location")
    suspend fun updateLocation(
        @Body request: LocationUpdateRequest
    ): Response<ApiResponse<LocationUpdateResponse>>
    
    /**
     * Driver profile endpoints
     */
    @GET("delivery/profile")
    suspend fun getProfile(): Response<ApiResponse<ProfileResponse>>
    
    @PUT("delivery/profile")
    suspend fun updateProfile(
        @Body request: ProfileUpdateRequest
    ): Response<ApiResponse<ProfileUpdateResponse>>
    
    @PUT("delivery/profile/availability")
    suspend fun updateAvailability(
        @Body availability: Map<String, Any>
    ): Response<DeliveryLoginResponse>
}
