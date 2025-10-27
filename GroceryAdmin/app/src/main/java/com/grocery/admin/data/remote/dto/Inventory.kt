package com.grocery.admin.data.remote.dto

import com.google.gson.annotations.SerializedName

data class InventoryItemDto(
    @SerializedName("product_id") val productId: String,
    @SerializedName("quantity") val quantity: Int,
    @SerializedName("reorder_level") val reorderLevel: Int?,
    @SerializedName("last_restocked") val lastRestocked: String?,
    @SerializedName("product") val product: ProductDto?
)

data class InventoryListResponse(
    @SerializedName("items") val items: List<InventoryItemDto>,
    @SerializedName("total") val total: Int
)

data class UpdateInventoryRequest(
    @SerializedName("product_id") val productId: String,
    @SerializedName("quantity") val quantity: Int,
    @SerializedName("reorder_level") val reorderLevel: Int? = null
)

data class UpdateInventoryResponse(
    @SerializedName("product_id") val productId: String,
    @SerializedName("quantity") val quantity: Int,
    @SerializedName("previous_quantity") val previousQuantity: Int
)
