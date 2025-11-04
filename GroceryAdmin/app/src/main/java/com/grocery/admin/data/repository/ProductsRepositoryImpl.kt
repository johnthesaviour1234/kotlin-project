package com.grocery.admin.data.repository

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import android.util.Log
import com.grocery.admin.data.remote.ApiService
import com.grocery.admin.data.remote.dto.*
import com.grocery.admin.domain.repository.ProductsRepository
import com.grocery.admin.util.Resource
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.storage.storage
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import java.io.InputStream
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ProductsRepositoryImpl @Inject constructor(
    private val apiService: ApiService,
    private val supabaseClient: SupabaseClient
) : ProductsRepository {
    
    companion object {
        private const val TAG = "ProductsRepositoryImpl"
    }
    
    override fun getProducts(page: Int, limit: Int): Flow<Resource<ProductsListResponse>> = flow {
        try {
            emit(Resource.Loading())
            Log.d(TAG, "Fetching products - page: $page, limit: $limit")
            
            val response = apiService.getProducts(page, limit)
            
            if (response.success && response.data != null) {
                Log.d(TAG, "Products fetched successfully: ${response.data.items.size} items")
                emit(Resource.Success(response.data))
            } else {
                val errorMessage = response.error ?: response.message ?: "Failed to fetch products"
                Log.e(TAG, "Failed to fetch products: $errorMessage")
                emit(Resource.Error(errorMessage))
            }
        } catch (e: Exception) {
            val errorMessage = e.localizedMessage ?: "Network error occurred"
            Log.e(TAG, "Error fetching products: $errorMessage", e)
            emit(Resource.Error(errorMessage))
        }
    }
    
    override fun getProductById(productId: String): Flow<Resource<ProductDto>> = flow {
        try {
            emit(Resource.Loading())
            Log.d(TAG, "Fetching product by ID: $productId")
            
            val response = apiService.getProductById(productId)
            
            if (response.success && response.data != null) {
                Log.d(TAG, "Product fetched successfully: ${response.data.name}")
                emit(Resource.Success(response.data))
            } else {
                val errorMessage = response.error ?: response.message ?: "Failed to fetch product"
                Log.e(TAG, "Failed to fetch product: $errorMessage")
                emit(Resource.Error(errorMessage))
            }
        } catch (e: Exception) {
            val errorMessage = e.localizedMessage ?: "Network error occurred"
            Log.e(TAG, "Error fetching product: $errorMessage", e)
            emit(Resource.Error(errorMessage))
        }
    }
    
    override fun createProduct(request: CreateProductRequest): Flow<Resource<ProductDto>> = flow {
        try {
            emit(Resource.Loading())
            Log.d(TAG, "Creating product: ${request.name}")
            
            val response = apiService.createProduct(request)
            
            if (response.success && response.data != null) {
                Log.d(TAG, "Product created successfully: ${response.data.id}")
                emit(Resource.Success(response.data))
            } else {
                val errorMessage = response.error ?: response.message ?: "Failed to create product"
                Log.e(TAG, "Failed to create product: $errorMessage")
                emit(Resource.Error(errorMessage))
            }
        } catch (e: Exception) {
            val errorMessage = e.localizedMessage ?: "Network error occurred"
            Log.e(TAG, "Error creating product: $errorMessage", e)
            emit(Resource.Error(errorMessage))
        }
    }
    
    override fun updateProduct(productId: String, request: UpdateProductRequest): Flow<Resource<ProductDto>> = flow {
        try {
            emit(Resource.Loading())
            Log.d(TAG, "Updating product: $productId")
            
            val response = apiService.updateProduct(productId, request)
            
            if (response.success && response.data != null) {
                Log.d(TAG, "Product updated successfully")
                emit(Resource.Success(response.data))
            } else {
                val errorMessage = response.error ?: response.message ?: "Failed to update product"
                Log.e(TAG, "Failed to update product: $errorMessage")
                emit(Resource.Error(errorMessage))
            }
        } catch (e: Exception) {
            val errorMessage = e.localizedMessage ?: "Network error occurred"
            Log.e(TAG, "Error updating product: $errorMessage", e)
            emit(Resource.Error(errorMessage))
        }
    }
    
    override fun deleteProduct(productId: String): Flow<Resource<DeleteProductResponse>> = flow {
        try {
            emit(Resource.Loading())
            Log.d(TAG, "Deleting product: $productId")
            
            val response = apiService.deleteProduct(productId)
            
            if (response.success && response.data != null) {
                Log.d(TAG, "Product deleted successfully")
                emit(Resource.Success(response.data))
            } else {
                val errorMessage = response.error ?: response.message ?: "Failed to delete product"
                Log.e(TAG, "Failed to delete product: $errorMessage")
                emit(Resource.Error(errorMessage))
            }
        } catch (e: Exception) {
            val errorMessage = e.localizedMessage ?: "Network error occurred"
            Log.e(TAG, "Error deleting product: $errorMessage", e)
            emit(Resource.Error(errorMessage))
        }
    }
    
    override fun getProductCategories(): Flow<Resource<List<ProductCategoryDto>>> = flow {
        try {
            emit(Resource.Loading())
            Log.d(TAG, "Fetching product categories")
            
            val response = apiService.getProductCategories()
            
            if (response.success && response.data != null) {
                val categories = response.data.items
                Log.d(TAG, "Product categories fetched successfully: ${categories.size} categories")
                emit(Resource.Success(categories))
            } else {
                val errorMessage = response.error ?: response.message ?: "Failed to fetch categories"
                Log.e(TAG, "Failed to fetch categories: $errorMessage")
                emit(Resource.Error(errorMessage))
            }
        } catch (e: Exception) {
            val errorMessage = e.localizedMessage ?: "Network error occurred"
            Log.e(TAG, "Error fetching categories: $errorMessage", e)
            emit(Resource.Error(errorMessage))
        }
    }
    
    override fun updateInventoryStock(productId: String, stock: Int): Flow<Resource<Unit>> = flow {
        try {
            emit(Resource.Loading())
            Log.d(TAG, "Updating inventory stock for product $productId to $stock")
            
            val request = UpdateInventoryRequest(
                productId = productId,
                stock = stock,
                adjustmentType = "set"
            )
            
            val response = apiService.updateInventory(request)
            
            if (response.success) {
                Log.d(TAG, "Inventory stock updated successfully")
                emit(Resource.Success(Unit))
            } else {
                val errorMessage = response.error ?: response.message ?: "Failed to update inventory"
                Log.e(TAG, "Failed to update inventory: $errorMessage")
                emit(Resource.Error(errorMessage))
            }
        } catch (e: Exception) {
            val errorMessage = e.localizedMessage ?: "Network error occurred"
            Log.e(TAG, "Error updating inventory: $errorMessage", e)
            emit(Resource.Error(errorMessage))
        }
    }
    
    /**
     * Upload product image to Supabase Storage and return the public URL
     * @param context Application context for accessing content resolver
     * @param imageUri URI of the image to upload
     * @param productName Name of the product (used for generating file name)
     * @return Result with the public URL of the uploaded image
     */
    override suspend fun uploadProductImage(
        context: Context,
        imageUri: Uri,
        productName: String
    ): Result<String> {
        return try {
            Log.d(TAG, "Starting image upload for product: $productName")
            
            // Read image data from URI
            val inputStream: InputStream? = context.contentResolver.openInputStream(imageUri)
            if (inputStream == null) {
                Log.e(TAG, "Failed to open input stream for image")
                return Result.failure(Exception("Failed to read image file"))
            }
            
            val imageBytes = inputStream.readBytes()
            inputStream.close()
            
            // Get file extension
            val extension = getFileExtension(context, imageUri) ?: "jpg"
            
            // Generate unique file name
            val fileName = "${productName.replace(" ", "_").lowercase()}_${UUID.randomUUID()}.${extension}"
            
            Log.d(TAG, "Uploading image: $fileName, size: ${imageBytes.size} bytes")
            
            // Upload to Supabase Storage
            val bucket = supabaseClient.storage.from("product-images")
            bucket.upload(fileName, imageBytes, upsert = false)
            
            // Get public URL
            val publicUrl = bucket.publicUrl(fileName)
            
            Log.d(TAG, "Image uploaded successfully: $publicUrl")
            Result.success(publicUrl)
            
        } catch (e: Exception) {
            val errorMessage = "Failed to upload image: ${e.localizedMessage}"
            Log.e(TAG, errorMessage, e)
            Result.failure(Exception(errorMessage))
        }
    }
    
    /**
     * Get file extension from URI
     */
    private fun getFileExtension(context: Context, uri: Uri): String? {
        return context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
            val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
            cursor.moveToFirst()
            val name = cursor.getString(nameIndex)
            name.substringAfterLast('.', "")
        }
    }
}
