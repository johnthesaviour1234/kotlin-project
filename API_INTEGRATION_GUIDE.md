# API Integration Guide for Android Apps

## 📋 Table of Contents
1. [Overview](#overview)
2. [Base Configuration](#base-configuration)
3. [Authentication Flow](#authentication-flow)
4. [Customer App Integration](#customer-app-integration)
5. [Admin App Integration](#admin-app-integration)
6. [Delivery App Integration](#delivery-app-integration)
7. [Error Handling](#error-handling)
8. [Security Best Practices](#security-best-practices)
9. [Testing Credentials](#testing-credentials)

---

## Overview

### API Architecture
- **Base URL (Local)**: `http://localhost:3000`
- **Base URL (Production)**: `https://andoid-app-kotlin.vercel.app`
- **Authentication**: JWT Bearer Token
- **Response Format**: JSON
- **Date Format**: ISO 8601 (UTC)

### User Roles
| Role | Description | Access |
|------|-------------|--------|
| `customer` | Regular users | Customer endpoints only |
| `admin` | Administrative users | Admin endpoints only |
| `delivery_driver` | Delivery personnel | Delivery endpoints only |

**Important**: Role-based access control is strictly enforced. Cross-role API calls will return 401 Unauthorized.

---

## Base Configuration

### 1. HTTP Client Setup (Kotlin/Android)

```kotlin
// RetrofitClient.kt
object RetrofitClient {
    private const val BASE_URL = "https://andoid-app-kotlin.vercel.app/"
    
    private val okHttpClient = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .addInterceptor(AuthInterceptor()) // Add token to requests
        .addInterceptor(HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BODY
        })
        .build()
    
    val retrofit: Retrofit = Retrofit.Builder()
        .baseUrl(BASE_URL)
        .client(okHttpClient)
        .addConverterFactory(GsonConverterFactory.create())
        .build()
}

// AuthInterceptor.kt
class AuthInterceptor : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val token = TokenStore.getAccessToken() // Your token storage
        val request = if (token != null) {
            chain.request().newBuilder()
                .addHeader("Authorization", "Bearer $token")
                .build()
        } else {
            chain.request()
        }
        return chain.proceed(request)
    }
}
```

### 2. Response Models

```kotlin
// Base response wrapper
data class ApiResponse<T>(
    val success: Boolean,
    val data: T?,
    val message: String?,
    val error: String?,
    val timestamp: String,
    val validation_errors: List<ValidationError>?
)

data class ValidationError(
    val field: String,
    val message: String
)
```

---

## Authentication Flow

### Registration (All User Types)

**Endpoint**: `POST /api/auth/register`

**Request Body**:
```json
{
  "email": "user@example.com",
  "password": "Password123",
  "full_name": "John Doe",
  "phone": "1234567890",
  "user_type": "customer" // or "admin" or "delivery_driver"
}
```

**Response** (200 OK):
```json
{
  "success": true,
  "data": {
    "user": {
      "id": "uuid",
      "email": "user@example.com",
      "profile": {
        "full_name": "John Doe",
        "user_type": "customer",
        "is_active": true
      }
    },
    "tokens": {
      "access_token": "eyJhbGc...",
      "refresh_token": "xyz123",
      "expires_at": 1761496053,
      "expires_in": 3600
    }
  },
  "message": "Registration successful"
}
```

**Kotlin Implementation**:
```kotlin
interface AuthApi {
    @POST("api/auth/register")
    suspend fun register(@Body request: RegisterRequest): ApiResponse<AuthResponse>
}

data class RegisterRequest(
    val email: String,
    val password: String,
    val full_name: String,
    val phone: String,
    val user_type: String = "customer"
)

data class AuthResponse(
    val user: User,
    val tokens: Tokens
)

data class Tokens(
    val access_token: String,
    val refresh_token: String,
    val expires_at: Long,
    val expires_in: Int
)
```

### Login Flow

#### Customer Login
**Endpoint**: `POST /api/auth/login`

#### Admin Login
**Endpoint**: `POST /api/admin/auth/login`

#### Delivery Driver Login
**Endpoint**: `POST /api/delivery/auth/login`

**Request Body** (same for all):
```json
{
  "email": "user@example.com",
  "password": "Password123"
}
```

**Response**: Same as registration response

**Kotlin Implementation**:
```kotlin
// After successful login
suspend fun handleLoginSuccess(response: ApiResponse<AuthResponse>) {
    if (response.success && response.data != null) {
        // Store tokens securely
        TokenStore.saveAccessToken(response.data.tokens.access_token)
        TokenStore.saveRefreshToken(response.data.tokens.refresh_token)
        
        // Store user info
        UserStore.saveUser(response.data.user)
        
        // Navigate to main screen
        navigateToHome()
    }
}
```

---

## Customer App Integration

### Complete Customer Order Flow

#### Step 1: Browse Products
**Endpoint**: `GET /api/products/list`

**Query Parameters**:
- `page` (optional): Page number (default: 1)
- `limit` (optional): Items per page (default: 20)
- `category` (optional): Filter by category ID
- `featured` (optional): true/false
- `search` (optional): Search query

**Request**:
```kotlin
interface ProductApi {
    @GET("api/products/list")
    suspend fun getProducts(
        @Query("page") page: Int = 1,
        @Query("limit") limit: Int = 20,
        @Query("featured") featured: Boolean? = null,
        @Query("category") category: String? = null
    ): ApiResponse<ProductListResponse>
}
```

**Response**:
```json
{
  "success": true,
  "data": {
    "items": [
      {
        "id": "uuid",
        "name": "Fresh Apples",
        "description": "Organic red apples",
        "price": 4.99,
        "image_url": "https://...",
        "category": {
          "id": "uuid",
          "name": "Fruits"
        },
        "featured": true,
        "is_active": true,
        "inventory": {
          "stock": 100
        }
      }
    ],
    "pagination": {
      "page": 1,
      "limit": 20,
      "total": 50,
      "total_pages": 3,
      "has_more": true
    }
  }
}
```

#### Step 2: Add to Cart
**Endpoint**: `POST /api/cart`
**Auth Required**: ✅ Customer Token

**Request**:
```json
{
  "product_id": "uuid",
  "quantity": 2,
  "price": 4.99
}
```

**Response**:
```json
{
  "success": true,
  "data": {
    "id": "cart_item_id",
    "product_id": "uuid",
    "quantity": 2,
    "price": 4.99
  },
  "message": "Product added to cart"
}
```

#### Step 3: View Cart
**Endpoint**: `GET /api/cart`
**Auth Required**: ✅ Customer Token

**Response**:
```json
{
  "success": true,
  "data": {
    "items": [
      {
        "id": "cart_item_id",
        "product": {
          "id": "uuid",
          "name": "Fresh Apples",
          "price": 4.99,
          "image_url": "https://..."
        },
        "quantity": 2,
        "subtotal": 9.98
      }
    ],
    "summary": {
      "subtotal": 9.98,
      "item_count": 2
    }
  }
}
```

#### Step 4: Create Order
**Endpoint**: `POST /api/orders/create`
**Auth Required**: ✅ Customer Token

**Request**:
```json
{
  "items": [
    {
      "productId": "uuid",
      "productName": "Fresh Apples",
      "quantity": 2,
      "unitPrice": 4.99,
      "totalPrice": 9.98,
      "productImageUrl": "https://..."
    }
  ],
  "subtotal": 9.98,
  "taxAmount": 0.80,
  "deliveryFee": 2.99,
  "totalAmount": 13.77,
  "deliveryAddress": {
    "street": "123 Main St",
    "apartment": "Apt 4B",
    "city": "San Francisco",
    "state": "CA",
    "postal_code": "94102",
    "landmark": "Near Central Park"
  },
  "paymentMethod": "cash",
  "notes": "Please ring doorbell"
}
```

**Response**:
```json
{
  "success": true,
  "data": {
    "id": "order_uuid",
    "order_number": "ORD001026",
    "status": "pending",
    "total_amount": 13.77,
    "created_at": "2025-10-26T15:30:00Z"
  },
  "message": "Order placed successfully"
}
```

**Kotlin Implementation**:
```kotlin
data class CreateOrderRequest(
    val items: List<OrderItem>,
    val subtotal: Double,
    val taxAmount: Double,
    val deliveryFee: Double,
    val totalAmount: Double,
    val deliveryAddress: DeliveryAddress,
    val paymentMethod: String = "cash",
    val notes: String? = null
)

data class OrderItem(
    val productId: String,
    val productName: String,
    val quantity: Int,
    val unitPrice: Double,
    val totalPrice: Double,
    val productImageUrl: String?
)

data class DeliveryAddress(
    val street: String,
    val apartment: String?,
    val city: String,
    val state: String,
    val postal_code: String,
    val landmark: String?
)
```

#### Step 5: Track Order
**Endpoint**: `GET /api/orders/history`
**Auth Required**: ✅ Customer Token

**Response**:
```json
{
  "success": true,
  "data": {
    "items": [
      {
        "id": "uuid",
        "order_number": "ORD001026",
        "status": "out_for_delivery",
        "total_amount": 13.77,
        "created_at": "2025-10-26T15:30:00Z",
        "estimated_delivery_time": "2025-10-26T16:30:00Z",
        "order_items": [...]
      }
    ]
  }
}
```

**Order Status Flow**:
```
pending → confirmed → preparing → ready → out_for_delivery → delivered
```

---

## Admin App Integration

### Complete Admin Order Management Flow

#### Step 1: Admin Login
**Endpoint**: `POST /api/admin/auth/login`

```json
{
  "email": "admin@grocery.com",
  "password": "AdminPass123"
}
```

**⚠️ Important**: Store the admin token separately from customer token.

#### Step 2: View Dashboard Metrics
**Endpoint**: `GET /api/admin/dashboard/metrics`
**Auth Required**: ✅ Admin Token

**Query Parameters**:
- `range` (optional): `1d`, `7d`, `30d`, `90d` (default: `7d`)

**Response**:
```json
{
  "success": true,
  "data": {
    "summary": {
      "total_orders": 23,
      "total_revenue": 1234.56,
      "average_order_value": 53.68,
      "active_customers": 150,
      "active_delivery_personnel": 10,
      "pending_orders": 5,
      "low_stock_items": 3
    },
    "orders_by_status": {
      "pending": 5,
      "confirmed": 3,
      "preparing": 2,
      "out_for_delivery": 4,
      "delivered": 9
    },
    "recent_orders": [...],
    "time_range": "7d"
  }
}
```

#### Step 3: List Pending Orders
**Endpoint**: `GET /api/admin/orders`
**Auth Required**: ✅ Admin Token

**Query Parameters**:
- `page` (optional): Page number
- `limit` (optional): Items per page
- `status` (optional): Filter by status (e.g., "pending")
- `search` (optional): Search by order number or customer email
- `date_from` (optional): Filter from date
- `date_to` (optional): Filter to date

**Request**:
```kotlin
@GET("api/admin/orders")
suspend fun getOrders(
    @Query("page") page: Int = 1,
    @Query("limit") limit: Int = 20,
    @Query("status") status: String? = null
): ApiResponse<OrderListResponse>
```

**Response**:
```json
{
  "success": true,
  "data": {
    "items": [
      {
        "id": "uuid",
        "order_number": "ORD001026",
        "customer_id": "uuid",
        "status": "pending",
        "total_amount": 52.09,
        "customer_info": {
          "email": "customer@example.com",
          "full_name": "John Doe"
        },
        "delivery_address": {...},
        "order_items": [...],
        "created_at": "2025-10-26T15:30:00Z"
      }
    ],
    "pagination": {
      "page": 1,
      "limit": 20,
      "total": 5,
      "total_pages": 1,
      "has_more": false
    }
  }
}
```

#### Step 4: Assign Order to Delivery Driver
**Endpoint**: `POST /api/admin/orders/assign`
**Auth Required**: ✅ Admin Token

**⚠️ Critical**: This is the key step that connects orders to delivery drivers.

**Request**:
```json
{
  "order_id": "uuid",
  "delivery_personnel_id": "driver_uuid",
  "estimated_minutes": 30
}
```

**Response**:
```json
{
  "success": true,
  "data": {
    "assignment_id": "uuid",
    "order_number": "ORD001026",
    "delivery_personnel_name": "John Driver",
    "assignment_status": "pending"
  },
  "message": "Order assigned successfully"
}
```

**What Happens**:
1. ✅ Order status changes: `pending` → `confirmed`
2. ✅ Assignment record created in `delivery_assignments` table
3. ✅ Delivery driver receives notification (if implemented)
4. ✅ Driver can see order in their "Available Orders" list

**Kotlin Implementation**:
```kotlin
data class AssignOrderRequest(
    val order_id: String,
    val delivery_personnel_id: String,
    val estimated_minutes: Int = 30
)

// UI Flow
suspend fun assignOrder(orderId: String, driverId: String) {
    try {
        val request = AssignOrderRequest(orderId, driverId, 30)
        val response = adminApi.assignOrder(request)
        
        if (response.success) {
            showSuccess("Order assigned to ${response.data?.delivery_personnel_name}")
            refreshOrdersList()
        }
    } catch (e: Exception) {
        showError("Failed to assign order: ${e.message}")
    }
}
```

#### Step 5: Update Order Status (Manual Override)
**Endpoint**: `PUT /api/admin/orders/{order_id}/status`
**Auth Required**: ✅ Admin Token

**Request**:
```json
{
  "status": "preparing",
  "notes": "Order is being prepared"
}
```

**Valid Status Values**:
- `pending`
- `confirmed`
- `preparing`
- `ready`
- `out_for_delivery`
- `delivered`
- `cancelled`

#### Step 6: Product Management

##### List Products
**Endpoint**: `GET /api/admin/products`
**Auth Required**: ✅ Admin Token

##### Create Product
**Endpoint**: `POST /api/admin/products`
**Auth Required**: ✅ Admin Token

**Request**:
```json
{
  "name": "Fresh Oranges",
  "description": "Sweet Florida oranges",
  "price": 5.99,
  "category_id": "category_uuid",
  "featured": false,
  "is_active": true,
  "initial_stock": 100
}
```

##### Add Product Images
**Endpoint**: `POST /api/admin/products/{product_id}/images`
**Auth Required**: ✅ Admin Token

**Request**:
```json
{
  "image_url": "https://cdn.example.com/image.jpg",
  "is_primary": true,
  "alt_text": "Fresh Oranges"
}
```

#### Step 7: Inventory Management

##### Get Inventory Status
**Endpoint**: `GET /api/admin/inventory`
**Auth Required**: ✅ Admin Token

**Query Parameters**:
- `low_stock` (optional): `true` to filter low stock items
- `threshold` (optional): Stock threshold (default: 10)

##### Update Stock
**Endpoint**: `PUT /api/admin/inventory`
**Auth Required**: ✅ Admin Token

**Request**:
```json
{
  "product_id": "uuid",
  "stock": 50,
  "adjustment_type": "add" // or "subtract" or "set"
}
```

**Adjustment Types**:
- `set`: Set stock to exact value
- `add`: Add to current stock
- `subtract`: Remove from current stock

---

## Delivery App Integration

### Complete Delivery Driver Flow

#### Step 1: Driver Login
**Endpoint**: `POST /api/delivery/auth/login`

```json
{
  "email": "driver@grocery.com",
  "password": "DriverPass123"
}
```

**Response includes driver-specific token**. Store separately.

#### Step 2: View Available Orders
**Endpoint**: `GET /api/delivery/available-orders`
**Auth Required**: ✅ Delivery Driver Token

**⚠️ Important**: This shows only orders assigned to the logged-in driver with status "pending".

**Response**:
```json
{
  "success": true,
  "data": {
    "items": [
      {
        "id": "assignment_uuid",
        "order_id": "order_uuid",
        "status": "pending",
        "assigned_at": "2025-10-26T15:31:40Z",
        "estimated_delivery_minutes": 30,
        "notes": "Handle with care",
        "orders": {
          "id": "order_uuid",
          "order_number": "ORD001026",
          "total_amount": 52.09,
          "delivery_address": {
            "street": "123 Main St",
            "apartment": "Apt 4B",
            "city": "San Francisco",
            "state": "CA",
            "postal_code": "94102",
            "landmark": "Near Central Park"
          },
          "customer_info": {
            "full_name": "John Doe",
            "email": "john@example.com",
            "phone": "1234567890"
          },
          "notes": "Please ring doorbell",
          "created_at": "2025-10-26T15:30:00Z"
        }
      }
    ],
    "count": 1
  }
}
```

**Kotlin Model**:
```kotlin
data class AvailableOrdersResponse(
    val items: List<DeliveryAssignment>,
    val count: Int
)

data class DeliveryAssignment(
    val id: String,
    val order_id: String,
    val status: String,
    val assigned_at: String,
    val estimated_delivery_minutes: Int,
    val notes: String?,
    val orders: OrderDetails
)

data class OrderDetails(
    val id: String,
    val order_number: String,
    val total_amount: Double,
    val delivery_address: DeliveryAddress,
    val customer_info: CustomerInfo,
    val notes: String?,
    val created_at: String
)
```

#### Step 3: Accept Order
**Endpoint**: `POST /api/delivery/accept`
**Auth Required**: ✅ Delivery Driver Token

**Request**:
```json
{
  "assignment_id": "assignment_uuid",
  "notes": "On my way to pick up the order"
}
```

**Response**:
```json
{
  "success": true,
  "data": {
    "accepted": true
  },
  "message": "Order accepted successfully"
}
```

**What Happens**:
1. ✅ Assignment status: `pending` → `accepted`
2. ✅ Order status: `confirmed` → `preparing`
3. ✅ `accepted_at` timestamp recorded
4. ✅ Order disappears from "Available Orders" list

**OR Decline Order**:

**Endpoint**: `POST /api/delivery/decline`
**Auth Required**: ✅ Delivery Driver Token

**Request**:
```json
{
  "assignment_id": "assignment_uuid",
  "reason": "Too far from current location"
}
```

#### Step 4: Update Delivery Status
**Endpoint**: `PUT /api/delivery/update-status`
**Auth Required**: ✅ Delivery Driver Token

**Request for "In Transit"**:
```json
{
  "assignment_id": "assignment_uuid",
  "status": "in_transit",
  "notes": "Picked up order, heading to customer"
}
```

**What Happens**:
1. ✅ Assignment status: `accepted` → `in_transit`
2. ✅ Order status: `preparing` → `out_for_delivery`
3. ✅ Customer can now track delivery

**Request for "Completed"**:
```json
{
  "assignment_id": "assignment_uuid",
  "status": "completed",
  "notes": "Delivered successfully"
}
```

**What Happens**:
1. ✅ Assignment status: `in_transit` → `completed`
2. ✅ Order status: `out_for_delivery` → `delivered`
3. ✅ `delivered_at` timestamp recorded
4. ✅ Order moves to completed history

**Valid Status Values** (for delivery driver):
- `in_transit`
- `completed`

#### Step 5: Send GPS Location Updates
**Endpoint**: `POST /api/delivery/update-location`
**Auth Required**: ✅ Delivery Driver Token

**⚠️ Important**: Call this periodically (every 10-30 seconds) while delivery is in progress.

**Request**:
```json
{
  "latitude": 37.7749,
  "longitude": -122.4194,
  "accuracy": 10,
  "speed": 15.5,
  "heading": 180.0,
  "order_id": "order_uuid"
}
```

**Response**:
```json
{
  "success": true,
  "data": {
    "id": "location_uuid",
    "latitude": 37.7749,
    "longitude": -122.4194,
    "timestamp": "2025-10-26T15:32:00Z"
  },
  "message": "Location updated successfully"
}
```

**Kotlin Implementation**:
```kotlin
// Location tracking service
class LocationTrackingService : Service() {
    private val locationUpdateInterval = 15000L // 15 seconds
    
    private fun startTracking(orderId: String) {
        locationJob = scope.launch {
            while (isActive) {
                val location = getCurrentLocation()
                updateServerLocation(location, orderId)
                delay(locationUpdateInterval)
            }
        }
    }
    
    private suspend fun updateServerLocation(location: Location, orderId: String) {
        try {
            val request = LocationUpdateRequest(
                latitude = location.latitude,
                longitude = location.longitude,
                accuracy = location.accuracy.toDouble(),
                speed = location.speed.toDouble(),
                heading = location.bearing.toDouble(),
                order_id = orderId
            )
            deliveryApi.updateLocation(request)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to update location", e)
        }
    }
}
```

---

## Error Handling

### Standard Error Response Format

```json
{
  "success": false,
  "error": "Error message",
  "timestamp": "2025-10-26T15:30:00Z",
  "validation_errors": [
    {
      "field": "email",
      "message": "Please provide a valid email address"
    }
  ]
}
```

### HTTP Status Codes

| Code | Meaning | Action |
|------|---------|--------|
| 200 | Success | Process response data |
| 201 | Created | Resource created successfully |
| 400 | Bad Request | Check request body/parameters |
| 401 | Unauthorized | Token invalid/expired - logout user |
| 403 | Forbidden | Insufficient permissions - show error |
| 404 | Not Found | Resource doesn't exist |
| 405 | Method Not Allowed | Wrong HTTP method used |
| 500 | Server Error | Show generic error, retry later |

### Error Handling Implementation

```kotlin
sealed class ApiResult<T> {
    data class Success<T>(val data: T) : ApiResult<T>()
    data class Error<T>(val message: String, val code: Int) : ApiResult<T>()
}

suspend fun <T> safeApiCall(apiCall: suspend () -> ApiResponse<T>): ApiResult<T> {
    return try {
        val response = apiCall()
        if (response.success && response.data != null) {
            ApiResult.Success(response.data)
        } else {
            ApiResult.Error(
                response.error ?: "Unknown error",
                -1
            )
        }
    } catch (e: HttpException) {
        when (e.code()) {
            401 -> {
                // Token expired - logout user
                TokenStore.clearTokens()
                navigateToLogin()
                ApiResult.Error("Session expired. Please login again.", 401)
            }
            403 -> ApiResult.Error("Access denied", 403)
            404 -> ApiResult.Error("Resource not found", 404)
            else -> ApiResult.Error("Server error: ${e.message()}", e.code())
        }
    } catch (e: Exception) {
        ApiResult.Error("Network error: ${e.localizedMessage}", -1)
    }
}

// Usage
viewModelScope.launch {
    when (val result = safeApiCall { orderApi.createOrder(request) }) {
        is ApiResult.Success -> {
            showSuccess("Order placed successfully")
            navigateToOrderConfirmation(result.data.id)
        }
        is ApiResult.Error -> {
            showError(result.message)
        }
    }
}
```

---

## Security Best Practices

### 1. Token Storage

**✅ DO**:
```kotlin
// Use encrypted shared preferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey

object TokenStore {
    private lateinit var encryptedPrefs: SharedPreferences
    
    fun init(context: Context) {
        val masterKey = MasterKey.Builder(context)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build()
        
        encryptedPrefs = EncryptedSharedPreferences.create(
            context,
            "secure_prefs",
            masterKey,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        )
    }
    
    fun saveAccessToken(token: String) {
        encryptedPrefs.edit().putString("access_token", token).apply()
    }
    
    fun getAccessToken(): String? {
        return encryptedPrefs.getString("access_token", null)
    }
}
```

**❌ DON'T**:
- Store tokens in plain text SharedPreferences
- Log tokens to console
- Store tokens in static variables

### 2. Token Refresh

⚠️ **Note**: Token refresh endpoint not yet implemented. Current tokens expire in 1 hour.

**Recommended Implementation**:
```kotlin
class AuthInterceptor : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val token = TokenStore.getAccessToken()
        val request = chain.request().newBuilder()
            .addHeader("Authorization", "Bearer $token")
            .build()
        
        val response = chain.proceed(request)
        
        // Handle token expiration
        if (response.code == 401) {
            // TODO: Implement token refresh when endpoint is available
            // For now, redirect to login
            TokenStore.clearTokens()
        }
        
        return response
    }
}
```

### 3. Sensitive Data

**Never log**:
- Access tokens
- Refresh tokens
- User passwords
- Customer personal information

### 4. HTTPS Only

```kotlin
// Enforce HTTPS in production
private const val BASE_URL = if (BuildConfig.DEBUG) {
    "http://localhost:3000/"
} else {
    "https://andoid-app-kotlin.vercel.app/"
}
```

---

## Testing Credentials

### Test Users (Pre-configured)

| Role | Email | Password | Status |
|------|-------|----------|--------|
| Admin | admin@grocery.com | AdminPass123 | ✅ Active |
| Delivery Driver | driver@grocery.com | DriverPass123 | ✅ Active |
| Customer | abcd@gmail.com | Password123 | ✅ Active |

### Test Data Available

- **Categories**: 5 (Fruits, Vegetables, Dairy, Snacks, Beverages)
- **Products**: 8 products with inventory
- **Orders**: 23 existing orders for testing

### Testing Flow

1. **Customer App**:
   - Login with `abcd@gmail.com`
   - Browse products
   - Add to cart
   - Place order

2. **Admin App**:
   - Login with `admin@grocery.com`
   - View dashboard metrics
   - See pending orders
   - Assign order to driver

3. **Delivery App**:
   - Login with `driver@grocery.com`
   - View available orders
   - Accept order
   - Update status to in_transit
   - Send location updates
   - Mark as completed

---

## Complete Integration Checklist

### Customer App
- [ ] Implement authentication (register/login)
- [ ] Implement token storage (encrypted)
- [ ] Create product browsing UI
- [ ] Implement cart functionality
- [ ] Create order placement flow
- [ ] Implement order history view
- [ ] Add order tracking UI
- [ ] Handle all error cases
- [ ] Test complete order flow

### Admin App
- [ ] Implement admin authentication
- [ ] Create dashboard with metrics
- [ ] Implement orders list (with filters)
- [ ] Create order assignment flow
- [ ] Implement product management (CRUD)
- [ ] Create inventory management UI
- [ ] Add product image upload
- [ ] Handle all error cases
- [ ] Test admin operations

### Delivery App
- [ ] Implement driver authentication
- [ ] Create available orders list
- [ ] Implement accept/decline flow
- [ ] Create active delivery UI
- [ ] Implement status updates
- [ ] Add GPS location tracking
- [ ] Create delivery completion flow
- [ ] Implement delivery history
- [ ] Handle all error cases
- [ ] Test complete delivery flow

---

## API Sequence Diagrams

### Customer Order Flow
```
Customer App          API Server          Database
     |                    |                   |
     |-- POST /auth/login-|                   |
     |<- 200 OK (token) --|                   |
     |                    |                   |
     |-- GET /products ---|                   |
     |<- 200 OK (list) ---|                   |
     |                    |                   |
     |-- POST /cart ------|                   |
     |<- 200 OK ----------|                   |
     |                    |                   |
     |-- POST /orders -----|                   |
     |                    |-- INSERT order ---|
     |                    |<- order_id -------|
     |<- 201 Created -----|                   |
```

### Admin Assignment Flow
```
Admin App            API Server          Database
     |                   |                   |
     |-- POST /admin/auth/login-------------|
     |<- 200 OK (admin_token) --------------|
     |                   |                   |
     |-- GET /admin/orders ------------------|
     |<- 200 OK (pending orders) -----------|
     |                   |                   |
     |-- POST /admin/orders/assign ---------|
     |                   |-- CALL assign_order_to_delivery()
     |                   |-- UPDATE orders.status = 'confirmed'
     |                   |-- INSERT delivery_assignments
     |<- 200 OK (assignment) --------------|
```

### Delivery Execution Flow
```
Delivery App         API Server          Database
     |                   |                   |
     |-- POST /delivery/auth/login ---------|
     |<- 200 OK (driver_token) ------------|
     |                   |                   |
     |-- GET /delivery/available-orders ----|
     |<- 200 OK (assigned orders) ---------|
     |                   |                   |
     |-- POST /delivery/accept -------------|
     |                   |-- CALL update_delivery_assignment_status()
     |                   |-- UPDATE assignment.status = 'accepted'
     |                   |-- UPDATE orders.status = 'preparing'
     |<- 200 OK ---------------------------|
     |                   |                   |
     |-- PUT /delivery/update-status -------|
     |   (status: in_transit)               |
     |                   |-- UPDATE assignment.status
     |                   |-- UPDATE orders.status = 'out_for_delivery'
     |<- 200 OK ---------------------------|
     |                   |                   |
     |-- POST /delivery/update-location ----|
     |   (GPS coordinates)                  |
     |                   |-- INSERT delivery_locations
     |<- 201 Created ---------------------|
     |                   |                   |
     |-- PUT /delivery/update-status -------|
     |   (status: completed)                |
     |                   |-- UPDATE assignment.status = 'completed'
     |                   |-- UPDATE orders.status = 'delivered'
     |                   |-- SET orders.delivered_at = now()
     |<- 200 OK ---------------------------|
```

---

## Support & Troubleshooting

### Common Issues

1. **401 Unauthorized on all requests**
   - Check if token is being sent in Authorization header
   - Verify token format: `Bearer <token>`
   - Check if user has correct role for endpoint

2. **Order not showing in delivery app**
   - Verify order was assigned to correct driver
   - Check assignment status is "pending"
   - Ensure driver is logged in with correct account

3. **Location updates not working**
   - Check GPS permissions granted
   - Verify latitude/longitude are valid (-90 to 90, -180 to 180)
   - Ensure order_id is included in request

4. **Products not loading**
   - Check if products have `is_active: true`
   - Verify inventory stock > 0
   - Check API endpoint URL is correct

### Debug Checklist
- [ ] Verify correct base URL (local vs production)
- [ ] Check token is stored and retrieved correctly
- [ ] Verify HTTP method matches endpoint requirement
- [ ] Check request body format matches expected JSON
- [ ] Verify user role matches endpoint requirements
- [ ] Check network connectivity
- [ ] Review server logs for detailed error messages

### Contact
For backend issues or questions:
- Review: `TEST_RESULTS.md`
- Setup guide: `TESTING_SETUP.md`
- Implementation plan: `BACKEND_FIRST_IMPLEMENTATION_PLAN.md`

---

**Version**: 1.0.0  
**Last Updated**: October 26, 2025  
**API Base URL**: https://andoid-app-kotlin.vercel.app
