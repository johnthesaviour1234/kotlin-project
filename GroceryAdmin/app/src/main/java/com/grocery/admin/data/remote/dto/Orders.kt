package com.grocery.admin.data.remote.dto

import android.os.Parcelable
import com.google.gson.annotations.SerializedName
import kotlinx.parcelize.Parcelize

@Parcelize
data class OrderDto(
    @SerializedName("id") val id: String,
    @SerializedName("user_id") val userId: String,
    @SerializedName("status") val status: String,
    @SerializedName("total_amount") val totalAmount: Double,
    @SerializedName("delivery_address") val deliveryAddress: DeliveryAddressDto?,
    @SerializedName("created_at") val createdAt: String,
    @SerializedName("updated_at") val updatedAt: String?,
    @SerializedName("items") val items: List<OrderItemDto>?,
    @SerializedName("user_profile") val userProfile: UserProfileDto?,
    @SerializedName("delivery_assignments") val deliveryAssignments: List<DeliveryAssignmentDto>?
) : Parcelable {
    /**
     * Check if order has an active delivery assignment
     */
    fun hasDeliveryAssignment(): Boolean {
        return !deliveryAssignments.isNullOrEmpty()
    }
}

@Parcelize
data class DeliveryAddressDto(
    @SerializedName("street") val street: String?,
    @SerializedName("apartment") val apartment: String?,
    @SerializedName("city") val city: String?,
    @SerializedName("state") val state: String?,
    @SerializedName("postal_code") val postalCode: String?,
    @SerializedName("landmark") val landmark: String?
) : Parcelable {
    fun getFormattedAddress(): String {
        val parts = mutableListOf<String>()
        street?.let { parts.add(it) }
        apartment?.let { parts.add(it) }
        city?.let { parts.add(it) }
        state?.let { parts.add(it) }
        postalCode?.let { parts.add(it) }
        return parts.joinToString(", ").ifEmpty { "No address provided" }
    }
}

@Parcelize
data class OrderItemDto(
    @SerializedName("id") val id: String,
    @SerializedName("product_id") val productId: String,
    @SerializedName("quantity") val quantity: Int,
    @SerializedName("price") val price: Double,
    @SerializedName("product") val product: ProductDto?
) : Parcelable

@Parcelize
data class ProductDto(
    @SerializedName("id") val id: String,
    @SerializedName("name") val name: String,
    @SerializedName("description") val description: String?,
    @SerializedName("price") val price: Double,
    @SerializedName("category_id") val categoryId: String?,
    @SerializedName("image_url") val imageUrl: String?,
    @SerializedName("featured") val featured: Boolean = false,
    @SerializedName("is_active") val isActive: Boolean = true,
    @SerializedName("category") val category: ProductCategorySimpleDto? = null,
    @SerializedName("inventory") val inventory: ProductInventoryDto? = null
) : Parcelable

@Parcelize
data class ProductCategorySimpleDto(
    @SerializedName("id") val id: String,
    @SerializedName("name") val name: String
) : Parcelable

@Parcelize
data class ProductInventoryDto(
    @SerializedName("quantity") val quantity: Int,
    @SerializedName("reorder_level") val reorderLevel: Int?
) : Parcelable

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

data class DeliveryPersonnelDto(
    @SerializedName("id") val id: String,
    @SerializedName("full_name") val fullName: String,
    @SerializedName("email") val email: String,
    @SerializedName("phone") val phone: String?,
    @SerializedName("is_active") val isActive: Boolean,
    @SerializedName("created_at") val createdAt: String
)

data class DeliveryPersonnelListResponse(
    @SerializedName("items") val items: List<DeliveryPersonnelDto>,
    @SerializedName("total") val total: Int
)

@Parcelize
data class DeliveryAssignmentDto(
    @SerializedName("id") val id: String,
    @SerializedName("delivery_personnel_id") val deliveryPersonnelId: String?,
    @SerializedName("status") val status: String,
    @SerializedName("assigned_at") val assignedAt: String?
) : Parcelable
