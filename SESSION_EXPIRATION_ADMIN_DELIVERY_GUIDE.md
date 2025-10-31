# Session Expiration Handling - Admin & Delivery Apps Implementation Guide

## Overview
This guide covers the implementation of proper session expiration handling for the **Admin** and **Delivery** Android applications. It mirrors the Customer app implementation with appropriate adjustments for each app's architecture.

---

## ✅ Completed Changes

### 1. Session Expiration Dialog Handler

#### Admin App - `SessionExpiredHandler.kt`
**Location:** `GroceryAdmin/app/src/main/java/com/grocery/admin/ui/util/SessionExpiredHandler.kt`

```kotlin
package com.grocery.admin.ui.util

import android.content.Context
import android.content.Intent
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.grocery.admin.ui.activities.LoginActivity

object SessionExpiredHandler {
    
    fun showSessionExpiredDialog(context: Context) {
        if (context !is AppCompatActivity) return
        if (context.isFinishing) return
        
        AlertDialog.Builder(context)
            .setTitle("Session Expired")
            .setMessage("Your session has expired. Please login again.")
            .setPositiveButton("Login") { _, _ ->
                navigateToLogin(context)
            }
            .setCancelable(false)
            .show()
    }
    
    private fun navigateToLogin(context: Context) {
        val intent = Intent(context, LoginActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        context.startActivity(intent)
        
        if (context is AppCompatActivity) {
            context.finish()
        }
    }
}
```

#### Delivery App - `SessionExpiredHandler.kt`
**Location:** `GroceryDelivery/app/src/main/java/com/grocery/delivery/ui/util/SessionExpiredHandler.kt`

Same structure as Admin app, adjusted for delivery package naming.

---

### 2. BaseViewModel Updates

#### Admin App - `BaseViewModel.kt`

**Added:**
```kotlin
import com.grocery.admin.data.exceptions.TokenExpiredException
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow

// Session expiration event
private val _sessionExpired = MutableSharedFlow<Unit>()
val sessionExpired: SharedFlow<Unit> = _sessionExpired.asSharedFlow()
```

**Enhanced `handleError()` method:**
```kotlin
protected open fun handleError(exception: Exception): String {
    val errorMessage = when (exception) {
        is TokenExpiredException -> {
            // Emit session expired event for UI to handle
            viewModelScope.launch {
                _sessionExpired.emit(Unit)
            }
            "Session expired. Please login again."
        }
        is java.net.UnknownHostException -> "Network error. Please check your connection."
        is java.net.SocketTimeoutException -> "Request timeout. Please try again."
        is retrofit2.HttpException -> {
            when (exception.code()) {
                401 -> "Authentication error. Please login again."
                403 -> "Access denied."
                404 -> "Resource not found."
                500 -> "Server error. Please try again later."
                else -> "HTTP error: ${exception.code()}"
            }
        }
        else -> exception.message ?: "An unknown error occurred"
    }
    
    _error.value = errorMessage
    return errorMessage
}
```

#### Delivery App - `BaseViewModel.kt`

**Added:**
```kotlin
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow

// Session expiration event
private val _sessionExpired = MutableSharedFlow<Unit>()
val sessionExpired: SharedFlow<Unit> = _sessionExpired.asSharedFlow()
```

**Enhanced `executeWithResource()` method:**
```kotlin
try {
    val result = block()
    stateFlow.value = result
    
    if (result is Resource.Error) {
        _error.value = result.message
        // Check if it's an auth error and emit session expired
        if (result.isAuthError) {
            _sessionExpired.emit(Unit)
        }
    }
} catch (exception: Exception) {
    val errorMessage = handleError(exception)
    stateFlow.value = Resource.Error(errorMessage)
}
```

---

### 3. BaseActivity Updates

#### Admin App - `BaseActivity.kt`

**Added imports:**
```kotlin
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.grocery.admin.ui.util.SessionExpiredHandler
import com.grocery.admin.ui.viewmodels.BaseViewModel
import kotlinx.coroutines.launch
```

**Added method:**
```kotlin
protected open fun getViewModel(): BaseViewModel? = null
```

**Added in onCreate():**
```kotlin
observeSessionExpiration()
```

**New method:**
```kotlin
private fun observeSessionExpiration() {
    val viewModel = getViewModel() ?: return

    lifecycleScope.launch {
        repeatOnLifecycle(Lifecycle.State.STARTED) {
            viewModel.sessionExpired.collect {
                SessionExpiredHandler.showSessionExpiredDialog(this@BaseActivity)
            }
        }
    }
}
```

#### Delivery App - `BaseActivity.kt`

Same structure as Admin app, adjusted for delivery package naming.

---

## 📋 Next Steps

### Step 1: Update All Repositories (Admin App)

All repositories need to detect 401 errors and throw `TokenExpiredException`. Since Admin uses `TokenExpiredException` pattern:

**Pattern to apply:**

```kotlin
override suspend fun someRepositoryMethod(): Result<SomeData> {
    return try {
        val response = apiService.someEndpoint()
        if (response.isSuccessful && response.body() != null) {
            Result.success(response.body()!!)
        } else if (response.code() == 401) {
            // Clear token
            tokenManager.clearToken()
            // Throw auth exception
            throw TokenExpiredException("Session expired. Please login again.")
        } else {
            Result.failure(Exception(response.message()))
        }
    } catch (e: TokenExpiredException) {
        throw e  // Re-throw auth exceptions
    } catch (e: Exception) {
        Result.failure(e)
    }
}
```

**Repositories to update:**
- `ProductRepositoryImpl`
- `CategoryRepositoryImpl`
- `UserRepositoryImpl`
- Any other repository with authenticated endpoints

---

### Step 2: Update All Repositories (Delivery App)

Since Delivery app uses `Resource` with `isAuthError` flag:

**Pattern to apply:**

```kotlin
override fun getDeliveries(): Flow<Resource<List<Delivery>>> = flow {
    emit(Resource.Loading())
    try {
        val response = apiService.getDeliveries()
        if (response.isSuccessful && response.body() != null) {
            emit(Resource.Success(response.body()!!))
        } else if (response.code() == 401) {
            // Clear token
            tokenManager.clearToken()
            // Emit auth error
            emit(Resource.Error("Session expired", isAuthError = true))
        } else {
            emit(Resource.Error(response.message() ?: "Unknown error"))
        }
    } catch (e: Exception) {
        emit(Resource.Error(e.message ?: "Network error"))
    }
}
```

**Repositories to update:**
- `DeliveryRepository` (already done ✅)
- Any other delivery-related repositories

---

### Step 3: Update ViewModels to Override getViewModel()

Each Activity needs to return its ViewModel in the `getViewModel()` method:

#### Admin App Example:

```kotlin
class ProductListActivity : BaseActivity<ActivityProductListBinding>() {
    
    private val viewModel: ProductViewModel by viewModels()
    
    override fun getViewModel(): BaseViewModel = viewModel
    
    // Rest of activity code...
}
```

#### Delivery App Example:

```kotlin
class DeliveryListActivity : BaseActivity<ActivityDeliveryListBinding>() {
    
    private val viewModel: DeliveryViewModel by viewModels()
    
    override fun getViewModel(): BaseViewModel = viewModel
    
    // Rest of activity code...
}
```

**Activities to update (Admin):**
- `ProductListActivity`
- `CategoryManagementActivity`
- `OrderManagementActivity`
- All other authenticated activities

**Activities to update (Delivery):**
- `DeliveryListActivity`
- `DeliveryDetailActivity`
- All other authenticated activities

---

## 🔍 Testing Checklist

### Admin App Testing

- [ ] Open admin app and login
- [ ] Wait for token to expire (or manually expire it in backend/database)
- [ ] Trigger any API call (load products, categories, etc.)
- [ ] Verify session expired dialog appears
- [ ] Tap "Login" button
- [ ] Verify navigation to LoginActivity with cleared stack
- [ ] Login again and verify functionality restored

### Delivery App Testing

- [ ] Open delivery app and login
- [ ] Wait for token to expire (or manually expire it)
- [ ] Trigger any API call (load deliveries, update status, etc.)
- [ ] Verify session expired dialog appears
- [ ] Tap "Login" button
- [ ] Verify navigation to LoginActivity with cleared stack
- [ ] Login again and verify functionality restored

### Edge Cases to Test

- [ ] Token expiration during network request
- [ ] Multiple simultaneous 401 errors (should show only one dialog)
- [ ] Token expiration when app is in background
- [ ] Token expiration in different screens
- [ ] Back button behavior after session expiration

---

## 📁 File Summary

### Admin App Files Created/Modified

| File | Status | Purpose |
|------|--------|---------|
| `ui/util/SessionExpiredHandler.kt` | ✅ Created | Shows session expired dialog and navigates to login |
| `ui/viewmodels/BaseViewModel.kt` | ✅ Modified | Added session expiration event emission |
| `ui/activities/BaseActivity.kt` | ✅ Modified | Observes session expiration and shows dialog |
| All repository implementations | 🔲 Pending | Need to detect 401 and throw TokenExpiredException |
| All authenticated activities | 🔲 Pending | Need to override getViewModel() |

### Delivery App Files Created/Modified

| File | Status | Purpose |
|------|--------|---------|
| `ui/util/SessionExpiredHandler.kt` | ✅ Created | Shows session expired dialog and navigates to login |
| `ui/viewmodels/BaseViewModel.kt` | ✅ Modified | Added session expiration event emission |
| `ui/activities/BaseActivity.kt` | ✅ Modified | Observes session expiration and shows dialog |
| `data/repository/DeliveryRepository.kt` | ✅ Modified | Detects 401 and emits auth error |
| Other repository implementations | 🔲 Pending | Need to detect 401 and emit auth error |
| All authenticated activities | 🔲 Pending | Need to override getViewModel() |

---

## 🔗 Architecture Flow

```
┌──────────────────────────────────────────────────────────┐
│                      User Action                         │
│              (API call with expired token)               │
└────────────────────────┬─────────────────────────────────┘
                         │
                         ▼
┌──────────────────────────────────────────────────────────┐
│                   Retrofit Call                          │
│              Server returns 401 Unauthorized             │
└────────────────────────┬─────────────────────────────────┘
                         │
                         ▼
┌──────────────────────────────────────────────────────────┐
│                  Repository Layer                        │
│  • Detects 401 response                                  │
│  • Clears stored token                                   │
│  • Admin: throws TokenExpiredException                   │
│  • Delivery: emits Resource.Error(isAuthError=true)      │
└────────────────────────┬─────────────────────────────────┘
                         │
                         ▼
┌──────────────────────────────────────────────────────────┐
│                  ViewModel Layer                         │
│  • Catches exception or observes error                   │
│  • Emits _sessionExpired event                           │
└────────────────────────┬─────────────────────────────────┘
                         │
                         ▼
┌──────────────────────────────────────────────────────────┐
│                 BaseActivity Layer                       │
│  • Observes sessionExpired event                         │
│  • Calls SessionExpiredHandler                           │
└────────────────────────┬─────────────────────────────────┘
                         │
                         ▼
┌──────────────────────────────────────────────────────────┐
│             SessionExpiredHandler                        │
│  • Shows "Session Expired" dialog                        │
│  • Navigates to LoginActivity                            │
│  • Clears activity stack                                 │
└──────────────────────────────────────────────────────────┘
```

---

## 🛠️ Implementation Best Practices

1. **Consistent Error Handling**: Always check for 401 before other error cases
2. **Token Clearing**: Clear token immediately on 401 detection
3. **Single Dialog**: Use SharedFlow to prevent multiple dialogs
4. **Lifecycle Awareness**: Use repeatOnLifecycle(STARTED) to avoid leaks
5. **Clear Navigation**: Use FLAG_ACTIVITY_CLEAR_TASK to prevent back navigation
6. **Testing**: Test with real token expiration scenarios

---

## 🎯 Summary

The Admin and Delivery apps now have:
- ✅ Centralized session expiration dialog handler
- ✅ BaseViewModel with session expiration event support
- ✅ BaseActivity with automatic session expiration observation
- ✅ Repository layer foundation for 401 detection
- 🔲 Pending: Update all repositories to handle 401
- 🔲 Pending: Update all activities to return ViewModels
- 🔲 Pending: End-to-end testing

This implementation provides a consistent, user-friendly experience when tokens expire, ensuring users are properly notified and redirected to login.
