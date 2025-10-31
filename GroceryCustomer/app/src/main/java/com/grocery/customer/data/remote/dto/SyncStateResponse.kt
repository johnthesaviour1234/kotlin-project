package com.grocery.customer.data.remote.dto

import com.google.gson.annotations.SerializedName

/**
 * Response from GET /api/sync/state endpoint
 * Contains current user state with timestamps and checksums for all entities
 */
data class SyncStateResponse(
    @SerializedName("success") val success: Boolean,
    @SerializedName("data") val data: SyncStateData,
    @SerializedName("timestamp") val timestamp: String // Server timestamp
)

data class SyncStateData(
    @SerializedName("cart") val cart: SyncCartState,
    @SerializedName("orders") val orders: SyncOrdersState,
    @SerializedName("profile") val profile: SyncProfileState,
    @SerializedName("timestamp") val timestamp: String // Server timestamp
)

data class SyncCartState(
    @SerializedName("items") val items: List<CartItemApi>,
    @SerializedName("total_items") val totalItems: Int,
    @SerializedName("total_price") val totalPrice: Double,
    @SerializedName("updated_at") val updatedAt: String, // ISO 8601 timestamp
    @SerializedName("checksum") val checksum: String // MD5 hash for change detection
)

data class SyncOrdersState(
    @SerializedName("items") val items: List<OrderApiResponse>,
    @SerializedName("count") val count: Int,
    @SerializedName("updated_at") val updatedAt: String, // ISO 8601 timestamp
    @SerializedName("checksum") val checksum: String // MD5 hash for change detection
)

data class SyncProfileState(
    @SerializedName("data") val data: UserProfile,
    @SerializedName("updated_at") val updatedAt: String // ISO 8601 timestamp
)

/**
 * Order response format from sync state
 */
data class OrderApiResponse(
    @SerializedName("id") val id: String,
    @SerializedName("order_number") val orderNumber: String,
    @SerializedName("status") val status: String,
    @SerializedName("total_amount") val totalAmount: Double,
    @SerializedName("subtotal") val subtotal: Double,
    @SerializedName("tax_amount") val taxAmount: Double?,
    @SerializedName("delivery_fee") val deliveryFee: Double?,
    @SerializedName("customer_info") val customerInfo: Map<String, Any>?,
    @SerializedName("delivery_address") val deliveryAddress: Map<String, Any>?,
    @SerializedName("payment_method") val paymentMethod: String?,
    @SerializedName("payment_status") val paymentStatus: String?,
    @SerializedName("notes") val notes: String?,
    @SerializedName("estimated_delivery_time") val estimatedDeliveryTime: String?,
    @SerializedName("delivered_at") val deliveredAt: String?,
    @SerializedName("created_at") val createdAt: String,
    @SerializedName("updated_at") val updatedAt: String,
    @SerializedName("items") val items: List<OrderItemApi>?
)

/**
 * Order item from sync state
 */
data class OrderItemApi(
    @SerializedName("id") val id: String,
    @SerializedName("product_id") val productId: String,
    @SerializedName("product_name") val productName: String,
    @SerializedName("product_image_url") val productImageUrl: String?,
    @SerializedName("quantity") val quantity: Int,
    @SerializedName("unit_price") val unitPrice: Double,
    @SerializedName("total_price") val totalPrice: Double
)
