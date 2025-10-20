package com.grocery.customer.domain.repository

import com.grocery.customer.data.remote.dto.*

/**
 * Repository interface for order-related operations.
 * This interface defines the contract for order management in the domain layer.
 */
interface OrderRepository {
    
    /**
     * Creates a new order with the provided details.
     * 
     * @param createOrderRequest The order creation request containing items and delivery details
     * @return Result containing the created order or error information
     */
    suspend fun createOrder(createOrderRequest: CreateOrderRequest): Result<OrderDTO>
    
    /**
     * Retrieves a specific order by its ID.
     * 
     * @param orderId The unique identifier of the order
     * @return Result containing the order details or error information
     */
    suspend fun getOrder(orderId: String): Result<OrderDTO>
    
    /**
     * Retrieves the order history for the current user.
     * 
     * @param page The page number for pagination (default: 1)
     * @param limit The number of orders per page (default: 10)
     * @param status Optional status filter (e.g., "pending", "delivered")
     * @return Result containing the list of orders and pagination info or error information
     */
    suspend fun getOrderHistory(
        page: Int = 1, 
        limit: Int = 10, 
        status: String? = null
    ): Result<OrderHistoryResponse>
}