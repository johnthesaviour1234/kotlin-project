package com.grocery.customer.data.repository

import com.grocery.customer.data.local.TokenStore
import com.grocery.customer.data.remote.ApiService
import com.grocery.customer.data.remote.dto.*
import com.grocery.customer.domain.repository.OrderRepository
import retrofit2.HttpException
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Implementation of OrderRepository that handles order operations via REST API.
 */
@Singleton
class OrderRepositoryImpl @Inject constructor(
    private val apiService: ApiService,
    private val tokenStore: TokenStore
) : OrderRepository {
    
    override suspend fun createOrder(createOrderRequest: CreateOrderRequest): Result<OrderDTO> {
        return try {
            val token = tokenStore.getAccessToken()
            if (token.isNullOrBlank()) {
                return Result.failure(Exception("Authentication token not found"))
            }
            
            val response = apiService.createOrder(
                request = createOrderRequest
            )
            
            if (response.isSuccessful) {
                response.body()?.let { createOrderResponse ->
                    Result.success(createOrderResponse.order)
                } ?: Result.failure(Exception("Empty response body"))
            } else {
                val errorMessage = when (response.code()) {
                    400 -> "Invalid order data"
                    401 -> "Authentication failed"
                    500 -> "Server error occurred"
                    else -> "Order creation failed"
                }
                Result.failure(Exception(errorMessage))
            }
            
        } catch (e: HttpException) {
            Result.failure(Exception("Network error: ${e.message()}"))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    override suspend fun getOrder(orderId: String): Result<OrderDTO> {
        return try {
            val token = tokenStore.getAccessToken()
            if (token.isNullOrBlank()) {
                return Result.failure(Exception("Authentication token not found"))
            }
            
            val response = apiService.getOrder(
                orderId = orderId
            )
            
            if (response.isSuccessful) {
                response.body()?.let { orderResponse ->
                    Result.success(orderResponse.order)
                } ?: Result.failure(Exception("Empty response body"))
            } else {
                val errorMessage = when (response.code()) {
                    404 -> "Order not found"
                    401 -> "Authentication failed"
                    403 -> "Access denied"
                    500 -> "Server error occurred"
                    else -> "Failed to fetch order details"
                }
                Result.failure(Exception(errorMessage))
            }
            
        } catch (e: HttpException) {
            Result.failure(Exception("Network error: ${e.message()}"))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    override suspend fun getOrderHistory(
        page: Int,
        limit: Int,
        status: String?
    ): Result<OrderHistoryResponse> {
        return try {
            val token = tokenStore.getAccessToken()
            if (token.isNullOrBlank()) {
                return Result.failure(Exception("Authentication token not found"))
            }
            
            val response = apiService.getOrderHistory(
                page = page,
                limit = limit,
                status = status
            )
            
            if (response.isSuccessful) {
                response.body()?.let { orderHistoryResponse ->
                    Result.success(orderHistoryResponse)
                } ?: Result.failure(Exception("Empty response body"))
            } else {
                val errorMessage = when (response.code()) {
                    401 -> "Authentication failed"
                    403 -> "Access denied"
                    500 -> "Server error occurred"
                    else -> "Failed to fetch order history"
                }
                Result.failure(Exception(errorMessage))
            }
            
        } catch (e: HttpException) {
            Result.failure(Exception("Network error: ${e.message()}"))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}