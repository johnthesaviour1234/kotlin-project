package com.grocery.admin.data.remote

import com.grocery.admin.data.remote.dto.HealthCheckResponse
import com.grocery.admin.data.remote.dto.LoginRequest
import com.grocery.admin.data.remote.dto.LoginResponse
import com.grocery.admin.data.remote.dto.ProductCategoryResponse
import com.grocery.admin.data.remote.dto.ProductResponse
import com.grocery.admin.data.remote.dto.RegisterRequest
import com.grocery.admin.data.remote.dto.RegisterResponse
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST

/**
 * Retrofit API service interface for communicating with the Vercel backend.
 * Defines all API endpoints used by the Customer app.
 */
interface ApiService {

    // Health check endpoint
    @GET("health")
    suspend fun getHealthCheck(): Response<HealthCheckResponse>

    // Authentication endpoints
    @POST("auth/login")
    suspend fun login(@Body request: LoginRequest): Response<LoginResponse>

    @POST("auth/register")
    suspend fun register(@Body request: RegisterRequest): Response<RegisterResponse>

    // Product endpoints
    @GET("products/categories")
    suspend fun getProductCategories(): Response<List<ProductCategoryResponse>>

    @GET("products/list")
    suspend fun getProducts(): Response<List<ProductResponse>>

    // User endpoints (future implementation)
    // @GET("users/profile")
    // suspend fun getUserProfile(): Response<UserProfileResponse>
}
