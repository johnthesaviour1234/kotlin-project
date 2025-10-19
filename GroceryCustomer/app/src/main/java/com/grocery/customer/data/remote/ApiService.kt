package com.grocery.customer.data.remote

import com.grocery.customer.data.remote.dto.ApiResponse
import com.grocery.customer.data.remote.dto.ChangePasswordRequest
import com.grocery.customer.data.remote.dto.ForgotPasswordRequest
import com.grocery.customer.data.remote.dto.LoginRequest
import com.grocery.customer.data.remote.dto.LoginResponse
import com.grocery.customer.data.remote.dto.RegisterRequest
import com.grocery.customer.data.remote.dto.RegisterResponse
import com.grocery.customer.data.remote.dto.ResetPasswordRequest
import com.grocery.customer.data.remote.dto.VerifyEmailRequest
import com.grocery.customer.data.remote.dto.VerifyEmailResponse
import com.grocery.customer.data.remote.dto.ResendVerificationRequest
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.POST
import retrofit2.http.PUT

/**
 * Retrofit API service interface for communicating with the Vercel backend.
 * Defines all authentication API endpoints used by the Customer app.
 */
interface ApiService {

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

    @PUT("auth/change-password")
    suspend fun changePassword(@Body request: ChangePasswordRequest): Response<ApiResponse<Unit>>

    @POST("auth/resend-verification")
    suspend fun resendVerification(@Body request: ResendVerificationRequest): Response<ApiResponse<Unit>>
}
