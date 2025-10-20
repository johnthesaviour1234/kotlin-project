package com.grocery.customer.domain.usecase

import com.grocery.customer.data.remote.dto.OrderHistoryResponse
import com.grocery.customer.domain.repository.OrderRepository
import javax.inject.Inject

/**
 * Use case for retrieving order history.
 * This handles the business logic for fetching user's past orders with optional filtering.
 */
class GetOrderHistoryUseCase @Inject constructor(
    private val orderRepository: OrderRepository
) {
    
    /**
     * Retrieves the order history for the current user.
     * 
     * @param page The page number for pagination (default: 1)
     * @param limit The number of orders per page (default: 10)
     * @param status Optional status filter (e.g., "pending", "delivered", "cancelled")
     * @return Result containing the list of orders and pagination info or error information
     */
    suspend operator fun invoke(
        page: Int = 1,
        limit: Int = 10,
        status: String? = null
    ): Result<OrderHistoryResponse> {
        
        // Validate pagination parameters
        if (page < 1) {
            return Result.failure(Exception("Page number must be greater than 0"))
        }
        
        if (limit < 1 || limit > 100) {
            return Result.failure(Exception("Limit must be between 1 and 100"))
        }
        
        // Validate status filter if provided
        status?.let { statusFilter ->
            if (!isValidOrderStatus(statusFilter)) {
                return Result.failure(Exception("Invalid order status filter"))
            }
        }
        
        return orderRepository.getOrderHistory(page, limit, status)
    }
    
    /**
     * Checks if the provided status is a valid order status.
     */
    private fun isValidOrderStatus(status: String): Boolean {
        val validStatuses = listOf(
            "pending", "confirmed", "preparing", 
            "ready", "delivered", "cancelled", "all"
        )
        return status.lowercase() in validStatuses
    }
}