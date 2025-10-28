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
    suspend fun logout(
        @Header("Authorization") token: String
    ): Response<Unit>
    
    /**
     * Order management endpoints
     */
    @GET("delivery/available-orders")
    suspend fun getAvailableOrders(
        @Header("Authorization") token: String
    ): Response<DeliveryOrdersResponse>
    
    @GET("delivery/orders/active")
    suspend fun getActiveOrder(
        @Header("Authorization") token: String,
        @Query("driver_id") driverId: String
    ): Response<OrderActionResponse>
    
    @GET("delivery/orders/history")
    suspend fun getOrderHistory(
        @Header("Authorization") token: String,
        @Query("limit") limit: Int? = 50,
        @Query("offset") offset: Int? = 0
    ): Response<DeliveryOrdersResponse>
    
    @POST("delivery/accept")
    suspend fun acceptOrder(
        @Header("Authorization") token: String,
        @Body request: AcceptOrderRequest
    ): Response<OrderActionResponse>
    
    @POST("delivery/decline")
    suspend fun declineOrder(
        @Header("Authorization") token: String,
        @Body request: DeclineOrderRequest
    ): Response<OrderActionResponse>
    
    @PUT("delivery/update-status")
    suspend fun updateOrderStatus(
        @Header("Authorization") token: String,
        @Body request: UpdateOrderStatusRequest
    ): Response<OrderActionResponse>
    
    /**
     * Location tracking endpoints
     */
    @POST("delivery/update-location")
    suspend fun updateLocation(
        @Header("Authorization") token: String,
        @Body request: LocationUpdateRequest
    ): Response<ApiResponse<LocationUpdateResponse>>
    
    /**
     * Driver profile endpoints
     */
    @GET("delivery/profile")
    suspend fun getProfile(
        @Header("Authorization") token: String
    ): Response<ApiResponse<ProfileResponse>>
    
    @PUT("delivery/profile")
    suspend fun updateProfile(
        @Header("Authorization") token: String,
        @Body request: ProfileUpdateRequest
    ): Response<ApiResponse<ProfileUpdateResponse>>
    
    @PUT("delivery/profile/availability")
    suspend fun updateAvailability(
        @Header("Authorization") token: String,
        @Body availability: Map<String, Any>
    ): Response<DeliveryLoginResponse>
}
