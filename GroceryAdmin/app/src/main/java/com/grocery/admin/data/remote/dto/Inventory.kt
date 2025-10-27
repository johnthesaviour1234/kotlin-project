package com.grocery.admin.data.remote.dto

import com.google.gson.annotations.SerializedName

data class InventoryItemDto(
    @SerializedName("product_id") val productId: String,
    @SerializedName("stock") val stock: Int,
    @SerializedName("updated_at") val updatedAt: String?,
    @SerializedName("products") val products: InventoryProductDto?
)

data class InventoryProductDto(
    @SerializedName("id") val id: String,
    @SerializedName("name") val name: String,
    @SerializedName("price") val price: Double,
    @SerializedName("image_url") val imageUrl: String?,
    @SerializedName("is_active") val isActive: Boolean
)

data class InventoryListResponse(
    @SerializedName("items") val items: List<InventoryItemDto>,
    @SerializedName("low_stock_count") val lowStockCount: Int
)

data class UpdateInventoryRequest(
    @SerializedName("product_id") val productId: String,
    @SerializedName("stock") val stock: Int,
    @SerializedName("adjustment_type") val adjustmentType: String = "set" // "set", "add", or "subtract"
)

data class UpdateInventoryResponse(
    @SerializedName("product_id") val productId: String,
    @SerializedName("stock") val stock: Int,
    @SerializedName("updated_at") val updatedAt: String
)
