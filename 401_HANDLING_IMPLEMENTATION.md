# 401 Error Handling Implementation

## ✅ Completed Steps

### 1. Increased JWT Token Expiry
- **Supabase Dashboard**: JWT expiry changed from 3600s (1 hour) to 86400s (24 hours)
- **Result**: Users now stay logged in for 1 day instead of 1 hour

### 2. Reverted AuthInterceptor to Keep It Simple
All three apps now have clean AuthInterceptors that ONLY add Bearer tokens:

**Files Updated:**
- `GroceryCustomer/app/src/main/java/com/grocery/customer/data/remote/AuthInterceptor.kt` ✅
- `GroceryAdmin/app/src/main/java/com/grocery/admin/data/remote/AuthInterceptor.kt` ✅
- `GroceryDelivery/app/src/main/java/com/grocery/delivery/data/remote/AuthInterceptor.kt` ✅

**What they do now:**
- Add "Authorization: Bearer {token}" header to requests
- Log token status for debugging
- **No** token refresh logic
- **No** 401 handling (handled in repositories instead)

### 3. Created TokenExpiredException for Customer/Admin Apps
**Files Created:**
- `GroceryCustomer/app/src/main/java/com/grocery/customer/domain/exceptions/TokenExpiredException.kt` ✅
- `GroceryAdmin/app/src/main/java/com/grocery/admin/domain/exceptions/TokenExpiredException.kt` ✅

**Usage:**
```kotlin
throw TokenExpiredException("Session expired. Please login again.")
```

### 4. Enhanced Resource Class for Delivery App
**File Updated:**
- `GroceryDelivery/app/src/main/java/com/grocery/delivery/utils/Resource.kt` ✅

**What changed:**
```kotlin
class Error<T>(
    message: String, 
    data: T? = null,
    isAuthError: Boolean = false  // NEW FLAG
) : Resource<T>(data, message, isAuthError)
```

### 5. Implemented 401 Handling Pattern in Repositories

**Example for Customer/Admin Apps (Result-based):**

File: `GroceryCustomer/app/src/main/java/com/grocery/customer/data/repository/OrderRepositoryImpl.kt` ✅

```kotlin
import com.grocery.customer.data.local.TokenStore
import com.grocery.customer.domain.exceptions.TokenExpiredException

class OrderRepositoryImpl @Inject constructor(
    private val apiService: ApiService,
    private val tokenStore: TokenStore  // Added
) : OrderRepository {
    
    override suspend fun someMethod(): Result<Data> {
        return try {
            val response = apiService.someCall()
            
            if (response.isSuccessful) {
                // Handle success
                Result.success(response.body()!!.data)
            } else {
                // Handle 401 - token expired
                if (response.code() == 401) {
                    Log.e(TAG, "401 Unauthorized - Token expired, clearing tokens")
                    tokenStore.clear()
                    return Result.failure(TokenExpiredException())
                }
                
                // Handle other errors
                Result.failure(Exception("Request failed"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
```

**Example for Delivery App (Flow/Resource-based):**

File: `GroceryDelivery/app/src/main/java/com/grocery/delivery/data/repository/DeliveryRepository.kt` ✅

```kotlin
import com.grocery.delivery.data.local.PreferencesManager

class DeliveryRepository @Inject constructor(
    private val apiService: DeliveryApiService,
    private val preferencesManager: PreferencesManager
) {
    
    fun someMethod(): Flow<Resource<Data>> = flow {
        try {
            emit(Resource.Loading())
            
            val response = apiService.someCall()
            
            if (response.isSuccessful && response.body() != null) {
                val data = response.body()!!
                if (data.success) {
                    emit(Resource.Success(data))
                } else {
                    emit(Resource.Error(data.message ?: "Failed"))
                }
            } else {
                // Handle 401 - token expired
                if (response.code() == 401) {
                    Log.e(TAG, "401 Unauthorized - Token expired, clearing tokens")
                    preferencesManager.clearAll()
                    emit(Resource.Error("Session expired. Please login again.", isAuthError = true))
                } else {
                    emit(Resource.Error(response.message() ?: "Network error"))
                }
            }
        } catch (e: Exception) {
            emit(Resource.Error(e.localizedMessage ?: "An error occurred"))
        }
    }
}
```

---

## 🔄 What Needs to Be Done Next

### Apply the Same 401 Handling Pattern to All Repositories

You need to add 401 handling to ALL repository methods that make API calls. The pattern is consistent across all apps.

### Customer App Repositories
Located in: `GroceryCustomer/app/src/main/java/com/grocery/customer/data/repository/`

- [x] `OrderRepositoryImpl.kt` - ✅ DONE (3 methods updated)
- [ ] `AuthRepositoryImpl.kt` - Update `getCurrentUser()`, `getUserProfile()`, `updateUserProfile()`, `getUserAddresses()`, etc.
- [ ] `ProductRepositoryImpl.kt` - Update all methods that call APIs
- [ ] `CartRepositoryImpl.kt` - Update all methods that call APIs  
- [ ] `CategoryRepositoryImpl.kt` - Update all methods that call APIs

**Steps for each repository:**
1. Add `import com.grocery.customer.domain.exceptions.TokenExpiredException`
2. Add `import android.util.Log`
3. Inject `TokenStore` if not already injected
4. Add companion object with TAG for logging
5. In each API call, add 401 handling:
   ```kotlin
   if (response.code() == 401) {
       Log.e(TAG, "401 Unauthorized - Token expired, clearing tokens")
       tokenStore.clear()
       return Result.failure(TokenExpiredException())
   }
   ```

### Admin App Repositories
Located in: `GroceryAdmin/app/src/main/java/com/grocery/admin/data/repository/`

- [ ] `AuthRepositoryImpl.kt`
- [ ] `DashboardRepositoryImpl.kt`
- [ ] `OrdersRepositoryImpl.kt`
- [ ] `ProductsRepositoryImpl.kt`
- [ ] `InventoryRepositoryImpl.kt`

**Same steps as Customer app**, just with `com.grocery.admin` package names.

### Delivery App Repositories
Located in: `GroceryDelivery/app/src/main/java/com/grocery/delivery/data/repository/`

- [x] `DeliveryRepository.kt` - ✅ DONE (1 method updated, need to update remaining methods)
- [ ] `AuthRepository.kt`

**Steps:**
1. Add `import android.util.Log`
2. Add companion object with TAG
3. In each API call response handling, add 401 check:
   ```kotlin
   if (response.code() == 401) {
       Log.e(TAG, "401 Unauthorized - Token expired, clearing tokens")
       preferencesManager.clearAll()
       emit(Resource.Error("Session expired. Please login again.", isAuthError = true))
   }
   ```

---

## 🎨 UI Layer Implementation

### Next Step: Handle Session Expired in ViewModels and UI

#### For Customer/Admin Apps

**1. Update BaseViewModel to detect TokenExpiredException:**

File: `GroceryCustomer/app/src/main/java/com/grocery/customer/ui/viewmodels/BaseViewModel.kt`

```kotlin
import com.grocery.customer.domain.exceptions.TokenExpiredException
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow

abstract class BaseViewModel : ViewModel() {
    
    // Add this
    private val _sessionExpired = MutableSharedFlow<Unit>()
    val sessionExpired: SharedFlow<Unit> = _sessionExpired
    
    protected open fun handleError(exception: Exception): String {
        val errorMessage = when (exception) {
            is TokenExpiredException -> {
                // Emit session expired event
                viewModelScope.launch {
                    _sessionExpired.emit(Unit)
                }
                exception.message ?: "Session expired"
            }
            is java.net.UnknownHostException -> "Network error. Please check your connection."
            // ... rest of error handling
        }
        
        _error.value = errorMessage
        return errorMessage
    }
}
```

**2. Observe sessionExpired in Fragments/Activities:**

```kotlin
class SomeFragment : Fragment() {
    
    private val viewModel: SomeViewModel by viewModels()
    
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        // Add this observer
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.sessionExpired.collect {
                showSessionExpiredDialog()
            }
        }
    }
    
    private fun showSessionExpiredDialog() {
        AlertDialog.Builder(requireContext())
            .setTitle("Session Expired")
            .setMessage("Your session has expired. Please login again.")
            .setPositiveButton("Login") { _, _ ->
                navigateToLogin()
            }
            .setCancelable(false)
            .show()
    }
    
    private fun navigateToLogin() {
        val intent = Intent(requireContext(), LoginActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        activity?.finish()
    }
}
```

#### For Delivery App

**1. Update ViewModels to check isAuthError:**

```kotlin
class AvailableOrdersViewModel @Inject constructor(
    private val deliveryRepository: DeliveryRepository
) : ViewModel() {
    
    // Add this
    private val _sessionExpired = MutableLiveData<Boolean>()
    val sessionExpired: LiveData<Boolean> = _sessionExpired
    
    fun fetchOrders() {
        viewModelScope.launch {
            deliveryRepository.getAvailableOrders().collect { resource ->
                when (resource) {
                    is Resource.Success -> { /* handle success */ }
                    is Resource.Error -> {
                        if (resource.isAuthError) {
                            _sessionExpired.value = true
                        } else {
                            _error.value = resource.message
                        }
                    }
                    is Resource.Loading -> { /* show loading */ }
                }
            }
        }
    }
}
```

**2. Observe in Activity:**

```kotlin
viewModel.sessionExpired.observe(this) { expired ->
    if (expired) {
        showSessionExpiredDialog()
    }
}

private fun showSessionExpiredDialog() {
    AlertDialog.Builder(this)
        .setTitle("Session Expired")
        .setMessage("Your session has expired. Please login again.")
        .setPositiveButton("Login") { _, _ ->
            navigateToLogin()
        }
        .setCancelable(false)
        .show()
}

private fun navigateToLogin() {
    val intent = Intent(this, LoginActivity::class.java)
    intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
    startActivity(intent)
    finish()
}
```

---

## 📝 Implementation Checklist

### Backend
- [x] Increase JWT expiry to 86400 seconds (1 day) in Supabase

### Android - Infrastructure
- [x] Simplify AuthInterceptor (all 3 apps)
- [x] Create TokenExpiredException (Customer & Admin)
- [x] Add isAuthError flag to Resource (Delivery)

### Android - Repository Layer (401 Detection)
- [x] Customer: OrderRepositoryImpl (example done)
- [ ] Customer: All other repositories
- [ ] Admin: All repositories
- [x] Delivery: DeliveryRepository (example done)
- [ ] Delivery: AuthRepository

### Android - ViewModel Layer (Session Expired Events)
- [ ] Customer: Update BaseViewModel
- [ ] Customer: Update all ViewModels to emit sessionExpired
- [ ] Admin: Update BaseViewModel
- [ ] Admin: Update all ViewModels to emit sessionExpired
- [ ] Delivery: Update all ViewModels to check isAuthError

### Android - UI Layer (Session Expired Dialog)
- [ ] Customer: Add sessionExpired observer to all fragments
- [ ] Customer: Implement showSessionExpiredDialog
- [ ] Admin: Add sessionExpired observer to all fragments
- [ ] Admin: Implement showSessionExpiredDialog
- [ ] Delivery: Add sessionExpired observer to all activities
- [ ] Delivery: Implement showSessionExpiredDialog

### Testing
- [ ] Test token expiration after 24 hours
- [ ] Test 401 handling during runtime
- [ ] Test session expired dialog shows correctly
- [ ] Test redirect to login works
- [ ] Test re-login works after session expires

---

## 🎯 Expected Behavior After Full Implementation

### Scenario 1: Normal Usage
1. User logs in
2. Uses app for < 24 hours
3. ✅ Everything works normally

### Scenario 2: Token Expires During Runtime
1. User logs in
2. Leaves app open for > 24 hours
3. User tries any action (refresh, view profile, etc.)
4. API returns 401
5. Repository detects 401, clears tokens, throws TokenExpiredException
6. ViewModel catches it, emits sessionExpired event
7. Fragment/Activity observes event
8. ✅ "Session Expired" dialog shows with "Login" button
9. User clicks "Login"
10. ✅ Redirected to LoginActivity
11. User logs in again
12. ✅ Can continue using app

### Scenario 3: Token Expires While App is Closed
1. User logs in
2. Closes app
3. Waits > 24 hours
4. Opens app
5. SplashActivity checks auth (finds expired token)
6. ✅ Automatically routes to LoginActivity

---

## 📚 Quick Reference

### Files That Show the Complete Pattern

**Customer App Example (Result-based):**
- Repository: `GroceryCustomer/.../OrderRepositoryImpl.kt` ✅ IMPLEMENTED
- Exception: `GroceryCustomer/.../TokenExpiredException.kt` ✅ IMPLEMENTED

**Delivery App Example (Flow/Resource-based):**
- Repository: `GroceryDelivery/.../DeliveryRepository.kt` ✅ PARTIALLY IMPLEMENTED
- Resource: `GroceryDelivery/.../Resource.kt` ✅ IMPLEMENTED

### Copy-Paste Code Snippets

**Customer/Admin - Add to Repository:**
```kotlin
// At top
import android.util.Log
import com.grocery.customer.data.local.TokenStore
import com.grocery.customer.domain.exceptions.TokenExpiredException

// In constructor
private val tokenStore: TokenStore

// In companion object
private const val TAG = "RepositoryName"

// In response handling
if (response.code() == 401) {
    Log.e(TAG, "401 Unauthorized - Token expired, clearing tokens")
    tokenStore.clear()
    return Result.failure(TokenExpiredException())
}
```

**Delivery - Add to Repository:**
```kotlin
// At top
import android.util.Log

// In companion object
private const val TAG = "RepositoryName"

// In response handling (Flow)
if (response.code() == 401) {
    Log.e(TAG, "401 Unauthorized - Token expired, clearing tokens")
    preferencesManager.clearAll()
    emit(Resource.Error("Session expired. Please login again.", isAuthError = true))
}
```

---

## ✅ Summary

**What's Done:**
1. ✅ JWT expiry increased to 1 day
2. ✅ AuthInterceptors simplified
3. ✅ Infrastructure for 401 detection created
4. ✅ Pattern implemented in example repositories

**What's Next:**
1. Apply 401 handling to ALL repository methods (copy-paste pattern)
2. Update ViewModels to emit sessionExpired events
3. Update UI to show dialog and redirect to login

**Estimated Time:** 2-3 hours to apply the pattern to all repositories and UI

**Difficulty:** Low - it's repetitive copy-paste work following the established pattern

---

**Document Version:** 1.0  
**Date:** October 31, 2025  
**Status:** Implementation in progress
