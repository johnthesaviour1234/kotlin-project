package com.grocery.admin.data.remote.dto

import com.google.gson.annotations.SerializedName

data class OrderDto(
    @SerializedName("id") val id: String,
    @SerializedName("user_id") val userId: String,
    @SerializedName("status") val status: String,
    @SerializedName("total_amount") val totalAmount: Double,
    @SerializedName("delivery_address") val deliveryAddress: String?,
    @SerializedName("created_at") val createdAt: String,
    @SerializedName("updated_at") val updatedAt: String?,
    @SerializedName("items") val items: List<OrderItemDto>?,
    @SerializedName("user_profile") val userProfile: UserProfileDto?
)

data class OrderItemDto(
    @SerializedName("id") val id: String,
    @SerializedName("product_id") val productId: String,
    @SerializedName("quantity") val quantity: Int,
    @SerializedName("price") val price: Double,
    @SerializedName("product") val product: ProductDto?
)

data class ProductDto(
    @SerializedName("id") val id: String,
    @SerializedName("name") val name: String,
    @SerializedName("description") val description: String?,
    @SerializedName("price") val price: Double,
    @SerializedName("category_id") val categoryId: String?,
    @SerializedName("image_url") val imageUrl: String?,
    @SerializedName("featured") val featured: Boolean = false,
    @SerializedName("is_active") val isActive: Boolean = true
)

data class OrdersListResponse(
    @SerializedName("items") val items: List<OrderDto>,
    @SerializedName("total") val total: Int,
    @SerializedName("page") val page: Int,
    @SerializedName("limit") val limit: Int
)

data class UpdateOrderStatusRequest(
    @SerializedName("status") val status: String,
    @SerializedName("notes") val notes: String? = null
)

data class AssignOrderRequest(
    @SerializedName("order_id") val orderId: String,
    @SerializedName("delivery_personnel_id") val deliveryPersonnelId: String,
    @SerializedName("estimated_delivery_minutes") val estimatedDeliveryMinutes: Int = 30
)

data class AssignOrderResponse(
    @SerializedName("assignment_id") val assignmentId: String,
    @SerializedName("order_id") val orderId: String,
    @SerializedName("delivery_personnel_id") val deliveryPersonnelId: String,
    @SerializedName("status") val status: String
)
