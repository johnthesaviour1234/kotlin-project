package com.grocery.customer.data.remote.dto

data class OrderDTO(
    val id: String,
    val order_number: String,
    val status: String,
    val total_amount: Double,
    val created_at: String,
    val order_items: List<OrderItemDTO>? = null,
    // Optional fields that may not be in API response
    val customer_id: String? = null,
    val subtotal: Double? = null,
    val tax_amount: Double? = null,
    val delivery_fee: Double? = null,
    val customer_info: CustomerInfoDTO? = null,
    val delivery_address: DeliveryAddressDTO? = null,
    val payment_method: String? = null,
    val payment_status: String? = null,
    val notes: String? = null,
    val estimated_delivery_time: String? = null,
    val delivered_at: String? = null,
    val updated_at: String? = null
)

data class OrderItemDTO(
    val id: String,
    val quantity: Int,
    val product_name: String,
    val product_image_url: String? = null,
    // Optional fields that may not be in API response
    val order_id: String? = null,
    val product_id: String? = null,
    val unit_price: Double? = null,
    val total_price: Double? = null,
    val created_at: String? = null
)

data class CustomerInfoDTO(
    val name: String,
    val email: String,
    val phone: String
)

data class DeliveryAddressDTO(
    val street: String,
    val city: String,
    val state: String,
    val postal_code: String,
    val country: String = "India",
    val landmark: String? = null,
    val apartment: String? = null
)

data class CreateOrderRequest(
    val items: List<CreateOrderItemRequest>,
    val subtotal: Double,
    val taxAmount: Double = 0.0,
    val deliveryFee: Double = 0.0,
    val totalAmount: Double,
    val deliveryAddress: DeliveryAddressDTO,
    val paymentMethod: String = "cash",
    val notes: String = "",
    val estimatedDeliveryTime: String? = null
)

data class CreateOrderItemRequest(
    val productId: String,
    val quantity: Int,
    val unitPrice: Double,
    val totalPrice: Double,
    val productName: String,
    val productImageUrl: String? = null
)

data class CreateOrderResponse(
    val message: String,
    val order: OrderDTO
)

data class OrderResponse(
    val message: String,
    val order: OrderDTO
)

data class OrderHistoryResponse(
    val message: String,
    val orders: List<OrderDTO>,
    val pagination: PaginationDTO
)

data class PaginationDTO(
    val currentPage: Int,
    val totalPages: Int,
    val totalItems: Int,
    val limit: Int,
    val hasNextPage: Boolean,
    val hasPreviousPage: Boolean
)
