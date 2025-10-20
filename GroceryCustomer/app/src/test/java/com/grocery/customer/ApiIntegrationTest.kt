package com.grocery.customer

import com.grocery.customer.data.remote.ApiService
import com.grocery.customer.data.repository.ProductRepositoryImpl
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

/**
 * Integration test to verify API connectivity and product loading
 */
class ApiIntegrationTest {

    private lateinit var apiService: ApiService
    private lateinit var productRepository: ProductRepositoryImpl

    @Before
    fun setup() {
        // Create a Retrofit instance with the same configuration as the app
        val retrofit = Retrofit.Builder()
            .baseUrl("https://andoid-app-kotlin.vercel.app/api/")
            .addConverterFactory(GsonConverterFactory.create())
            .build()

        apiService = retrofit.create(ApiService::class.java)
        productRepository = ProductRepositoryImpl(apiService)
    }

    @Test
    fun `test health check API endpoint`() = runTest {
        val response = apiService.getHealthCheck()
        println("Health check response: ${response.code()}")
        if (response.isSuccessful) {
            println("Health check successful: ${response.body()}")
        } else {
            println("Health check failed: ${response.errorBody()?.string()}")
        }
    }

    @Test
    fun `test get featured products`() = runTest {
        val result = productRepository.getFeaturedProducts(10)
        result.fold(
            onSuccess = { products ->
                println("Successfully loaded ${products.size} products:")
                products.forEach { product ->
                    println("- ${product.name}: $${product.price}")
                }
            },
            onFailure = { exception ->
                println("Failed to load products: ${exception.message}")
                exception.printStackTrace()
            }
        )
    }

    @Test
    fun `test get product categories`() = runTest {
        val result = productRepository.getCategories()
        result.fold(
            onSuccess = { categories ->
                println("Successfully loaded ${categories.items.size} categories:")
                categories.items.forEach { category ->
                    println("- ${category.name}")
                }
            },
            onFailure = { exception ->
                println("Failed to load categories: ${exception.message}")
                exception.printStackTrace()
            }
        )
    }

    @Test
    fun `test API call directly`() = runTest {
        val response = apiService.getProducts(featured = true, limit = 5)
        println("Direct API call response code: ${response.code()}")
        if (response.isSuccessful) {
            val body = response.body()
            println("API Response success: ${body?.success}")
            println("API Response data: ${body?.data}")
            if (body?.success == true) {
                println("Products count: ${body.data?.items?.size}")
                body.data?.items?.forEach { product ->
                    println("Product: ${product.name} - $${product.price}")
                }
            }
        } else {
            println("API call failed: ${response.errorBody()?.string()}")
        }
    }
}