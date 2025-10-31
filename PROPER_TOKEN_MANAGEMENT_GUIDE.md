# Proper Token Management & JWT Expiry Configuration

## Problem Analysis

The 401 errors after leaving apps idle are caused by:
1. **Short JWT token expiry** (default 1 hour in Supabase)
2. Apps continuing to use expired tokens without detecting expiration
3. No graceful handling of 401 errors to redirect users to login

## Solution Overview

### Two-Part Solution:

#### Part 1: Increase JWT Token Expiry (Backend - Supabase Configuration)
**Goal**: Extend token lifetime from 1 hour to 1 day (86400 seconds)

#### Part 2: Proper 401 Error Handling (Android Apps)
**Goal**: When tokens expire, automatically clear auth state and redirect to login

---

## Part 1: Increase JWT Token Expiry in Supabase

### Steps to Change JWT Expiry:

1. **Login to Supabase Dashboard**
   - URL: https://supabase.com/dashboard/project/hfxdxxpmcemdjsvhsdcf

2. **Navigate to Authentication Settings**
   - Click on "Authentication" in the left sidebar
   - Click on "Configuration" tab
   - Scroll to "JWT Settings" section

3. **Change JWT Expiry Time**
   - Find setting: **"JWT expiry limit"**
   - Current value: `3600` seconds (1 hour)
   - **Change to**: `86400` seconds (24 hours / 1 day)
   - Click **"Save"**

4. **Verify Configuration**
   - After saving, test login from any app
   - Check the `expires_in` value in login response
   - Should now be `86400` instead of `3600`

### Why 1 Day?
- **Balance**: Long enough for normal usage, short enough for security
- **User Experience**: Users won't need to re-login multiple times per day
- **Security**: Still expires eventually, limiting exposure if token is compromised
- **No refresh needed**: With 1-day expiry, no need for complex refresh token logic

---

## Part 2: Proper 401 Error Handling in Android Apps

### Current Architecture (All Three Apps)

All apps already have the proper structure:
```
SplashActivity → Checks auth → Routes to Login or Main
MainActivity   → User's main screen
LoginActivity  → Handles authentication
```

### What's Already Working

#### ✅ Customer App
- Uses DataStore for token storage (`TokenStore.kt`)
- `AuthInterceptor` adds Bearer token to requests
- `SplashActivity` checks authentication on app start
- Proper login/logout flow

#### ✅ Admin App  
- Same architecture as Customer app
- Uses DataStore for token storage
- Proper authentication flow

#### ✅ Delivery App
- Uses SharedPreferences for token storage (`PreferencesManager.kt`)
- Has `AuthInterceptor` that adds Bearer token
- `SplashActivity` checks authentication
- Proper logout with confirmation dialog

### What Needs to Be Fixed

The apps don't handle 401 errors gracefully during runtime. When a token expires WHILE the app is running:
- API calls fail with 401
- User sees generic error message
- App doesn't automatically redirect to login
- Expired tokens aren't cleared

### Solution: Handle 401 at the Repository Layer

Instead of modifying `AuthInterceptor` (which shouldn't have app navigation logic), we handle 401 errors in repositories and ViewModels.

#### Approach:

**1. Repository Layer**: Detect 401 errors
**2. Clear tokens**: Remove expired auth data
**3. Emit special error**: Signal that re-authentication is needed
**4. ViewModel Layer**: Handle the special error
**5. Activity/Fragment**: Show "Session expired" dialog and redirect to login

### Implementation Pattern

#### Customer/Admin Apps (Using TokenStore):

```kotlin
// In Repository
override suspend fun someApiCall(): Result<Data> {
    return try {
        val response = apiService.someEndpoint()
        if (response.isSuccessful) {
            Result.success(response.body()!!.data)
        } else {
            // Check for 401 - token expired
            if (response.code() == 401) {
                // Clear expired tokens
                tokenStore.clear()
                Result.failure(TokenExpiredException("Session expired. Please login again."))
            } else {
                Result.failure(Exception(parseApiError(response.errorBody())))
            }
        }
    } catch (e: Exception) {
        Result.failure(e)
    }
}

// Special exception class
class TokenExpiredException(message: String) : Exception(message)
```

#### Delivery App (Using PreferencesManager):

```kotlin
// In Repository
fun someApiCall(): Flow<Resource<Data>> = flow {
    emit(Resource.Loading())
    try {
        val response = apiService.someEndpoint()
        if (response.isSuccessful && response.body() != null) {
            val data = response.body()!!
            if (data.success) {
                emit(Resource.Success(data))
            } else {
                emit(Resource.Error(data.message ?: "Request failed"))
            }
        } else {
            // Handle 401 - token expired
            if (response.code() == 401) {
                preferencesManager.clearAll()
                emit(Resource.Error("Session expired. Please login again.", isAuthError = true))
            } else {
                emit(Resource.Error(response.message() ?: "Request failed"))
            }
        }
    } catch (e: Exception) {
        emit(Resource.Error(e.localizedMessage ?: "An error occurred"))
    }
}

// Enhanced Resource class
sealed class Resource<T>(
    val data: T? = null,
    val message: String? = null,
    val isAuthError: Boolean = false  // NEW: Flag for auth errors
) {
    class Success<T>(data: T) : Resource<T>(data)
    class Error<T>(message: String, data: T? = null, isAuthError: Boolean = false) : 
        Resource<T>(data, message, isAuthError)
    class Loading<T>(data: T? = null) : Resource<T>(data)
}
```

#### ViewModel Layer:

```kotlin
// In ViewModel
fun someAction() {
    viewModelScope.launch {
        repository.someApiCall().collect { resource ->
            when (resource) {
                is Resource.Success -> { /* handle success */ }
                is Resource.Error -> {
                    if (resource.isAuthError) {
                        // Emit special event for session expired
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
```

#### Activity/Fragment Layer:

```kotlin
// In Fragment/Activity
viewModel.sessionExpired.observe(this) { expired ->
    if (expired) {
        showSessionExpiredDialog()
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
```

---

## Why This Approach is Better

### ❌ Previous Quick-Fix Approach (What Not To Do):
- Modified `AuthInterceptor` to check expiration
- Added navigation logic to interceptor
- Mixed concerns (network layer handling UI navigation)
- Complex refresh token logic

### ✅ Proper Approach (What To Do):
- **Increase token expiry** to 1 day (no refresh needed)
- **Handle 401 at repository layer** (where API calls happen)
- **Clear expired tokens** immediately
- **Emit proper events** for ViewModels to handle
- **Show user-friendly dialog** and redirect to login
- **Keep interceptor simple** (just adds auth header)

---

## Implementation Checklist

### Backend Configuration
- [ ] Login to Supabase dashboard
- [ ] Navigate to Auth > Configuration
- [ ] Change JWT expiry from 3600 to 86400 seconds
- [ ] Save configuration
- [ ] Test login to verify `expires_in: 86400`

### Android Apps

#### Customer App
- [ ] Add `TokenExpiredException` class
- [ ] Update repositories to detect 401 and clear tokens
- [ ] Add `_sessionExpired` LiveData to ViewModels
- [ ] Update fragments to observe and handle session expiry
- [ ] Test: Login → Wait >1 day → Try action → See login dialog

#### Admin App
- [ ] Add `TokenExpiredException` class
- [ ] Update repositories to detect 401 and clear tokens  
- [ ] Add `_sessionExpired` LiveData to ViewModels
- [ ] Update fragments to observe and handle session expiry
- [ ] Test: Login → Wait >1 day → Try action → See login dialog

#### Delivery App
- [ ] Update `Resource` class with `isAuthError` flag
- [ ] Update repositories to detect 401 and set `isAuthError = true`
- [ ] Add `_sessionExpired` LiveData to ViewModels
- [ ] Update activities to observe and handle session expiry
- [ ] Test: Login → Wait >1 day → Try action → See login dialog

---

## Testing

### Test Scenario 1: Normal Usage (Token Valid)
1. Login to app
2. Use app immediately
3. ✅ **Expected**: Everything works normally

### Test Scenario 2: Token Expired After 1 Day
1. Login to app
2. Change system date to +2 days (to simulate expiry)
3. Try any API action (refresh, view profile, etc.)
4. ✅ **Expected**: 
   - API returns 401
   - Tokens cleared automatically
   - Dialog shows: "Session expired. Please login again."
   - Clicking "Login" redirects to LoginActivity
   - User can log in again successfully

### Test Scenario 3: Token Expired While App is Open
1. Login to app
2. Leave app open in background for >1 day
3. Resume app and try any action
4. ✅ **Expected**: Same as Scenario 2

---

## Benefits of This Solution

### ✅ User Experience
- **Longer sessions**: Users can use app for 24 hours without re-login
- **Clear messaging**: "Session expired" dialog is user-friendly
- **One-click recovery**: Login button immediately shows login screen
- **No confusion**: No generic "401" or "Unauthorized" errors

### ✅ Security
- **Tokens still expire**: 1-day limit prevents indefinite exposure
- **Immediate cleanup**: Expired tokens removed right away
- **Forced re-authentication**: Can't use expired tokens

### ✅ Code Quality
- **Separation of concerns**: Network layer doesn't handle UI
- **Maintainable**: Logic is in the right places
- **Testable**: Each layer can be tested independently
- **No complex refresh logic**: Simple expiry + re-login approach

### ✅ Reliability
- **Always works**: Guaranteed to handle 401 errors
- **No edge cases**: Simple expiry detection
- **No race conditions**: No concurrent refresh attempts
- **Predictable**: Same behavior across all apps

---

## Summary

### The Complete Solution:

1. **Backend**: Change Supabase JWT expiry to 86400 seconds (1 day)
2. **Android**: Handle 401 errors at repository layer
3. **Android**: Clear tokens when 401 detected
4. **Android**: Show "Session expired" dialog
5. **Android**: Redirect to login screen

### What Users Experience:

**Before**: 
- App works for 1 hour → Breaks with confusing errors → User frustrated

**After**:
- App works for 24 hours → Shows "Session expired, please login" → User logs in again → Continues using app

### Key Takeaway:

**With 1-day token expiry + proper 401 handling, you don't need complex token refresh logic.** Users simply re-login once per day, which is acceptable UX for most apps.

---

**Document Version**: 2.0  
**Date**: October 31, 2025  
**Status**: Ready for implementation
