package com.grocery.customer.domain.usecase

import com.grocery.customer.data.remote.dto.OrderDTO
import com.grocery.customer.domain.repository.OrderRepository
import javax.inject.Inject

/**
 * Use case for retrieving specific order details.
 * This handles the business logic for fetching individual order information.
 */
class GetOrderDetailsUseCase @Inject constructor(
    private val orderRepository: OrderRepository
) {
    
    /**
     * Retrieves detailed information for a specific order.
     * 
     * @param orderId The unique identifier of the order
     * @return Result containing the order details or error information
     */
    suspend operator fun invoke(orderId: String): Result<OrderDTO> {
        
        // Validate input
        if (orderId.isBlank()) {
            return Result.failure(Exception("Order ID cannot be empty"))
        }
        
        return orderRepository.getOrder(orderId)
    }
}