package com.grocery.admin.data.remote.dto

import com.google.gson.annotations.SerializedName

data class ProductsListResponse(
    @SerializedName("items") val items: List<ProductDto>,
    @SerializedName("total") val total: Int,
    @SerializedName("page") val page: Int,
    @SerializedName("limit") val limit: Int
)

data class CreateProductRequest(
    @SerializedName("name") val name: String,
    @SerializedName("description") val description: String?,
    @SerializedName("price") val price: Double,
    @SerializedName("category_id") val categoryId: String,
    @SerializedName("image_url") val imageUrl: String? = null,
    @SerializedName("featured") val featured: Boolean = false,
    @SerializedName("is_active") val isActive: Boolean = true,
    @SerializedName("initial_stock") val initialStock: Int? = null
)

data class UpdateProductRequest(
    @SerializedName("name") val name: String?,
    @SerializedName("description") val description: String?,
    @SerializedName("price") val price: Double?,
    @SerializedName("category_id") val categoryId: String?,
    @SerializedName("image_url") val imageUrl: String? = null,
    @SerializedName("featured") val featured: Boolean?,
    @SerializedName("is_active") val isActive: Boolean?
)

data class ProductCategoryDto(
    @SerializedName("id") val id: String,
    @SerializedName("name") val name: String,
    @SerializedName("description") val description: String?,
    @SerializedName("parent_id") val parentId: String? = null,
    @SerializedName("created_at") val createdAt: String? = null
)

data class ProductCategoriesResponse(
    @SerializedName("items") val items: List<ProductCategoryDto>
)

data class DeleteProductResponse(
    @SerializedName("deleted") val deleted: Boolean,
    @SerializedName("product_id") val productId: String
)
