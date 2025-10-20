package com.grocery.customer.domain.usecase

import com.grocery.customer.data.remote.dto.*
import com.grocery.customer.domain.repository.OrderRepository
import javax.inject.Inject

/**
 * Use case for creating a new order.
 * This handles the business logic for order creation including validation.
 */
class CreateOrderUseCase @Inject constructor(
    private val orderRepository: OrderRepository
) {
    
    /**
     * Creates a new order with the provided details.
     * 
     * @param items List of cart items to be converted to order items
     * @param deliveryAddress Customer's delivery address
     * @param paymentMethod Payment method selected by customer (default: "cash")
     * @param notes Optional notes for the order
     * @return Result containing the created order or error information
     */
    suspend operator fun invoke(
        items: List<CreateOrderItemRequest>,
        deliveryAddress: DeliveryAddressDTO,
        paymentMethod: String = "cash",
        notes: String = ""
    ): Result<OrderDTO> {
        
        // Validate input
        if (items.isEmpty()) {
            return Result.failure(Exception("Order must contain at least one item"))
        }
        
        if (deliveryAddress.street.isBlank() || deliveryAddress.city.isBlank()) {
            return Result.failure(Exception("Complete delivery address is required"))
        }
        
        // Calculate totals
        val subtotal = items.sumOf { it.totalPrice }
        val taxAmount = calculateTax(subtotal)
        val deliveryFee = calculateDeliveryFee(subtotal)
        val totalAmount = subtotal + taxAmount + deliveryFee
        
        // Validate calculated amounts
        if (totalAmount <= 0) {
            return Result.failure(Exception("Invalid order total"))
        }
        
        val createOrderRequest = CreateOrderRequest(
            items = items,
            subtotal = subtotal,
            taxAmount = taxAmount,
            deliveryFee = deliveryFee,
            totalAmount = totalAmount,
            deliveryAddress = deliveryAddress,
            paymentMethod = paymentMethod,
            notes = notes
        )
        
        return orderRepository.createOrder(createOrderRequest)
    }
    
    /**
     * Calculates tax amount based on subtotal.
     * For now, using a simple 5% tax rate.
     */
    private fun calculateTax(subtotal: Double): Double {
        return subtotal * 0.05 // 5% tax
    }
    
    /**
     * Calculates delivery fee based on order value.
     * Free delivery for orders above ₹500, otherwise ₹50.
     */
    private fun calculateDeliveryFee(subtotal: Double): Double {
        return if (subtotal >= 500.0) 0.0 else 50.0
    }
}