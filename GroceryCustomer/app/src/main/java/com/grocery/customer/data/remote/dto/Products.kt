package com.grocery.customer.data.remote.dto

data class ProductCategory(
    val id: String,
    val name: String,
    val description: String?,
    val parent_id: String?,
    val created_at: String?
)

data class CategoriesPayload(
    val items: List<ProductCategory>
)

data class Pagination(
    val page: Int?,
    val limit: Int?,
    val total: Int?,
    val hasMore: Boolean?
)

data class Product(
    val id: String,
    val name: String,
    val price: Double,
    val image_url: String?,
    val featured: Boolean?,
    val created_at: String?,
    val category_id: String?
)

data class ProductsListPayload(
    val items: List<Product>,
    val pagination: Pagination?
)

data class Inventory(
    val stock: Int?,
    val updated_at: String?
)

data class ProductDetail(
    val id: String,
    val name: String,
    val category_id: String?,
    val price: Double,
    val description: String?,
    val image_url: String?,
    val featured: Boolean?,
    val is_active: Boolean?,
    val created_at: String?,
    val updated_at: String?,
    val inventory: Inventory?
)
