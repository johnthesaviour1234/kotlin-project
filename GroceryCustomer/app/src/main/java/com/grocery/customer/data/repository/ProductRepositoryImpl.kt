package com.grocery.customer.data.repository

import com.grocery.customer.data.remote.ApiService
import com.grocery.customer.data.remote.dto.*
import com.grocery.customer.domain.repository.ProductRepository
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import okhttp3.ResponseBody
import javax.inject.Inject
import android.util.Log

/**
 * Implementation of ProductRepository
 * Handles product-related API calls and error handling
 */
class ProductRepositoryImpl @Inject constructor(
    private val api: ApiService
) : ProductRepository {

    private val gson = Gson()

    companion object {
        private const val TAG = "ProductRepositoryImpl"
    }

    private fun parseApiError(errorBody: ResponseBody?): String {
        if (errorBody == null) return "Request failed"
        return try {
            val type = object : TypeToken<ApiResponse<Any?>>() {}.type
            val parsed: ApiResponse<Any?> = gson.fromJson(errorBody.charStream(), type)
            buildString {
                if (!parsed.error.isNullOrBlank()) append(parsed.error)
                val details = parsed.validationErrors
                if (!details.isNullOrEmpty()) {
                    val detailMsg = details.joinToString("; ") { 
                        (it.field ?: "").let { f -> 
                            if (f.isNotBlank()) "$f: ${it.message}" 
                            else it.message 
                        } 
                    }
                    if (isNotEmpty()) append(" - ")
                    append(detailMsg)
                }
            }.ifBlank { "Bad request" }
        } catch (e: Exception) {
            "Bad request"
        }
    }

    override suspend fun getFeaturedProducts(limit: Int): Result<List<Product>> {
        Log.d(TAG, "getFeaturedProducts called with limit=$limit")
        return try {
            Log.d(TAG, "Making API call to getProducts(featured=true, limit=$limit)")
            val response = api.getProducts(
                featured = true,
                limit = limit
            )
            Log.d(TAG, "API response received: isSuccessful=${response.isSuccessful}, code=${response.code()}")
            if (response.isSuccessful) {
                val body = response.body()
                Log.d(TAG, "Response body: success=${body?.success}, hasData=${body?.data != null}")
                if (body?.success == true && body.data != null) {
                    Log.d(TAG, "Successfully parsed ${body.data.items.size} featured products")
                    Result.success(body.data.items)
                } else {
                    val errorMsg = body?.error ?: body?.message ?: "Failed to load featured products"
                    Log.e(TAG, "API response indicates failure: $errorMsg")
                    Result.failure(Exception(errorMsg))
                }
            } else {
                val errorMsg = parseApiError(response.errorBody())
                Log.e(TAG, "API request failed with code ${response.code()}: $errorMsg")
                Result.failure(Exception(errorMsg))
            }
        } catch (e: Exception) {
            Log.e(TAG, "Exception in getFeaturedProducts: ${e.message}", e)
            Result.failure(e)
        }
    }

    override suspend fun getCategories(): Result<CategoriesPayload> {
        return try {
            val response = api.getProductCategories()
            if (response.isSuccessful) {
                val body = response.body()
                if (body?.success == true && body.data != null) {
                    Result.success(body.data)
                } else {
                    Result.failure(Exception(body?.error ?: body?.message ?: "Failed to load categories"))
                }
            } else {
                Result.failure(Exception(parseApiError(response.errorBody())))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun getProductsByCategory(
        categoryId: String,
        page: Int,
        limit: Int
    ): Result<ProductsListPayload> {
        return try {
            val response = api.getProducts(
                category = categoryId,
                page = page,
                limit = limit
            )
            if (response.isSuccessful) {
                val body = response.body()
                if (body?.success == true && body.data != null) {
                    Result.success(body.data)
                } else {
                    Result.failure(Exception(body?.error ?: body?.message ?: "Failed to load products"))
                }
            } else {
                Result.failure(Exception(parseApiError(response.errorBody())))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun searchProducts(
        query: String,
        page: Int,
        limit: Int
    ): Result<ProductsListPayload> {
        return try {
            val response = api.getProducts(
                query = query,
                page = page,
                limit = limit
            )
            if (response.isSuccessful) {
                val body = response.body()
                if (body?.success == true && body.data != null) {
                    Result.success(body.data)
                } else {
                    Result.failure(Exception(body?.error ?: body?.message ?: "Search failed"))
                }
            } else {
                Result.failure(Exception(parseApiError(response.errorBody())))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun getProductDetail(productId: String): Result<ProductDetail> {
        return try {
            val response = api.getProductDetail(productId)
            if (response.isSuccessful) {
                val body = response.body()
                if (body?.success == true && body.data != null) {
                    Result.success(body.data)
                } else {
                    Result.failure(Exception(body?.error ?: body?.message ?: "Failed to load product details"))
                }
            } else {
                Result.failure(Exception(parseApiError(response.errorBody())))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun getAllProducts(
        featured: Boolean?,
        category: String?,
        query: String?,
        page: Int,
        limit: Int
    ): Result<ProductsListPayload> {
        return try {
            val response = api.getProducts(
                featured = featured,
                category = category,
                query = query,
                page = page,
                limit = limit
            )
            if (response.isSuccessful) {
                val body = response.body()
                if (body?.success == true && body.data != null) {
                    Result.success(body.data)
                } else {
                    Result.failure(Exception(body?.error ?: body?.message ?: "Failed to load products"))
                }
            } else {
                Result.failure(Exception(parseApiError(response.errorBody())))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}