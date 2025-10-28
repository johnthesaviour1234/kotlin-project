package com.grocery.delivery.data.dto

import android.os.Parcelable
import com.google.gson.annotations.SerializedName
import kotlinx.parcelize.Parcelize
import kotlinx.parcelize.RawValue

/**
 * Order DTOs for delivery operations
 */

data class DeliveryOrdersResponse(
    @SerializedName("success")
    val success: Boolean,
    
    @SerializedName("data")
    val data: AvailableOrdersData?
)

data class AvailableOrdersData(
    @SerializedName("items")
    val items: List<DeliveryAssignment>?,
    
    @SerializedName("count")
    val count: Int?
)

@Parcelize
data class DeliveryAssignment(
    @SerializedName("id")
    val id: String,
    
    @SerializedName("order_id")
    val orderId: String,
    
    @SerializedName("status")
    val status: String,
    
    @SerializedName("assigned_at")
    val assignedAt: String?,
    
    @SerializedName("accepted_at")
    val acceptedAt: String?,
    
    @SerializedName("estimated_delivery_minutes")
    val estimatedDeliveryMinutes: Int?,
    
    @SerializedName("notes")
    val notes: String?,
    
    @SerializedName("created_at")
    val createdAt: String?,
    
    @SerializedName("updated_at")
    val updatedAt: String?,
    
    @SerializedName("orders")
    val orders: DeliveryOrder?
) : Parcelable

@Parcelize
data class DeliveryOrder(
    @SerializedName("id")
    val id: String,
    
    @SerializedName("order_number")
    val orderNumber: String,
    
    @SerializedName("total_amount")
    val totalAmount: Double,
    
    @SerializedName("delivery_address")
    val deliveryAddress: @RawValue Any?, // JSONB field
    
    @SerializedName("customer_info")
    val customerInfo: @RawValue Any?, // JSONB field
    
    @SerializedName("notes")
    val notes: String?,
    
    @SerializedName("created_at")
    val createdAt: String
) : Parcelable {
    fun getCustomerName(): String? {
        return try {
            val info = customerInfo as? Map<*, *>
            info?.get("name") as? String ?: info?.get("full_name") as? String
        } catch (e: Exception) {
            null
        }
    }
    
    fun getCustomerPhone(): String? {
        return try {
            val info = customerInfo as? Map<*, *>
            info?.get("phone") as? String
        } catch (e: Exception) {
            null
        }
    }
    
    fun getFormattedAddress(): String {
        return try {
            val address = deliveryAddress as? Map<*, *>
            listOfNotNull(
                address?.get("street"),
                address?.get("apartment"),
                address?.get("city"),
                address?.get("state"),
                address?.get("postal_code")
            ).joinToString(", ")
        } catch (e: Exception) {
            "Address not available"
        }
    }
}


data class AcceptOrderRequest(
    @SerializedName("assignment_id")
    val assignmentId: String,
    
    @SerializedName("notes")
    val notes: String? = null
)

data class DeclineOrderRequest(
    @SerializedName("assignment_id")
    val assignmentId: String,
    
    @SerializedName("reason")
    val reason: String? = null
)

data class UpdateOrderStatusRequest(
    @SerializedName("assignment_id")
    val assignmentId: String,
    
    @SerializedName("status")
    val status: String,
    
    @SerializedName("notes")
    val notes: String? = null,
    
    @SerializedName("proof_of_delivery")
    val proofOfDelivery: String? = null
)

data class OrderActionResponse(
    @SerializedName("success")
    val success: Boolean,
    
    @SerializedName("message")
    val message: String?,
    
    @SerializedName("data")
    val data: Map<String, Any>?
)

/**
 * Order status constants
 */
object OrderStatus {
    const val PENDING = "pending"
    const val ASSIGNED = "assigned"
    const val ACCEPTED = "accepted"
    const val IN_TRANSIT = "in_transit"
    const val DELIVERED = "delivered"
    const val CANCELLED = "cancelled"
}
