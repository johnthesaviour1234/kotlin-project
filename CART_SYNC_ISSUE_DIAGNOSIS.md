# Cart Real-Time Sync Issue - Diagnosis & Fix

**Date**: January 29, 2025  
**Issue**: Cart changes not syncing between devices  
**Status**: 🐛 **ROOT CAUSE IDENTIFIED**  

---

## 🔍 Problem Description

When testing with 2 phones (A and B) logged in as `abcd@gmail.com`:

**Phone A**: Add item to cart from product details page
- ❌ **Phone B (Home Page)**: Cart badge NOT updated in real-time
- ❌ **Phone B (Cart Page)**: New item NOT shown in cart until manual refresh (back to home → cart again)

**Expected Behavior**: Changes made on Phone A should appear instantly on Phone B (within 1-2 seconds) without manual refresh.

---

## 🔬 Investigation Results

### ✅ Backend Event Broadcasting (Working Correctly)

**File**: `grocery-delivery-api/pages/api/cart/index.js`

```javascript
// Line 134: POST - Add item
await eventBroadcaster.cartUpdated(userId, { action: 'item_added', productId, quantity })

// Line 186: PUT - Update quantity  
await eventBroadcaster.cartUpdated(userId, { action: 'quantity_updated', itemId, quantity })

// Line 165: PUT - Remove item (quantity=0)
await eventBroadcaster.cartUpdated(userId, { action: 'item_removed', itemId })

// Line 205: DELETE - Clear cart
await eventBroadcaster.cartUpdated(userId, { action: 'cart_cleared' })
```

**Channel**: `cart:{userId}`  
**Event**: `updated`  
**Status**: ✅ **Backend IS broadcasting correctly**

---

### ✅ RealtimeManager Subscription Logic (Correctly Implemented)

**File**: `GroceryCustomer/app/src/main/java/com/grocery/customer/data/remote/RealtimeManager.kt`

```kotlin
// Line 62: Called in initialize()
subscribeToCartUpdates(userId)

// Lines 247-300: Subscription implementation
private fun subscribeToCartUpdates(userId: String) {
    val channelName = "cart:$userId"
    val channel = supabaseClient.realtime.channel(channelName)
    
    channel.broadcastFlow<Map<String, Any?>>("updated")
        .onEach { payload ->
            val action = payload["action"] as? String
            when (action) {
                "item_added" -> eventBus.publish(Event.CartItemAdded(...))
                "quantity_updated" -> eventBus.publish(Event.CartItemQuantityChanged(...))
                "item_removed" -> eventBus.publish(Event.CartItemRemoved(...))
                "cart_cleared" -> eventBus.publish(Event.CartCleared)
                else -> eventBus.publish(Event.CartUpdated)
            }
        }
        .launchIn(scope)
    
    channel.subscribe()
}
```

**Status**: ✅ **Subscription logic IS correct**

---

### ✅ CartViewModel Event Handling (Correctly Implemented)

**File**: `GroceryCustomer/app/src/main/java/com/grocery/customer/ui/viewmodels/CartViewModel.kt`

```kotlin
// Lines 56-90: Event subscriptions in init block
init {
    viewModelScope.launch {
        eventBus.subscribe<Event.CartUpdated>().collect {
            refreshCart() // ✅ Fetches latest from server
        }
    }
    
    viewModelScope.launch {
        eventBus.subscribe<Event.CartItemAdded>().collect { event ->
            refreshCart() // ✅ Triggers on item added
        }
    }
    
    // ... similar subscriptions for CartItemQuantityChanged, CartItemRemoved, CartCleared
}
```

**Status**: ✅ **ViewModel IS subscribed to events**

---

## 🐛 **ROOT CAUSE IDENTIFIED**

### ❌ RealtimeManager is NEVER Initialized

**Expected Flow** (from documentation):
```kotlin
// After successful login
realtimeManager.initialize(userId)
```

**Actual Implementation**: ❌ **NOWHERE IN THE CODE**

**Files Checked**:
- ✅ `GroceryCustomerApplication.kt` - No initialization
- ✅ `LoginActivity.kt` - No initialization  
- ✅ `MainActivity.kt` - No RealtimeManager reference
- ✅ `AuthViewModel.kt` - No RealtimeManager injected
- ✅ `AuthRepositoryImpl.kt` - No RealtimeManager reference

**Result**: The app NEVER calls `realtimeManager.initialize(userId)`, so:
1. ❌ No subscription to `cart:{userId}` channel
2. ❌ No subscription to `user:{userId}` channel  
3. ❌ No subscription to `products` channel
4. ❌ Supabase Realtime events are broadcast but NOT received

---

## ✅ Solution

### Fix 1: Initialize RealtimeManager After Login

**Option A: In MainActivity (RECOMMENDED)**

**File**: `GroceryCustomer/app/src/main/java/com/grocery/customer/ui/activities/MainActivity.kt`

```kotlin
@AndroidEntryPoint
class MainActivity : BaseActivity<ActivityMainBinding>() {
    
    @Inject
    lateinit var realtimeManager: RealtimeManager
    
    @Inject
    lateinit var tokenStore: TokenStore
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Initialize realtime connection
        lifecycleScope.launch {
            val userId = tokenStore.getUserId() // Need to add getUserId() to TokenStore
            if (userId != null) {
                realtimeManager.initialize(userId)
                Log.d("MainActivity", "RealtimeManager initialized for user: $userId")
            }
        }
    }
    
    override fun onDestroy() {
        super.onDestroy()
        realtimeManager.unsubscribeAll()
    }
}
```

---

### Fix 2: Add getUserId() to TokenStore

**File**: `GroceryCustomer/app/src/main/java/com/grocery/customer/data/local/TokenStore.kt`

```kotlin
// Add this method
suspend fun getUserId(): String? {
    return dataStore.data.firstOrNull()?.get(USER_ID_KEY)
}

// Add companion object key
companion object {
    private val USER_ID_KEY = stringPreferencesKey("user_id")
}
```

**And update saveTokens() to save userId**:
```kotlin
suspend fun saveTokens(accessToken: String, refreshToken: String, expiresAt: Long, userId: String) {
    dataStore.edit { preferences ->
        preferences[ACCESS_TOKEN_KEY] = accessToken
        preferences[REFRESH_TOKEN_KEY] = refreshToken
        preferences[EXPIRES_AT_KEY] = expiresAt
        preferences[USER_ID_KEY] = userId // NEW
    }
}
```

---

### Fix 3: Update AuthRepositoryImpl to Save User ID

**File**: `GroceryCustomer/app/src/main/java/com/grocery/customer/data/repository/AuthRepositoryImpl.kt`

```kotlin
override suspend fun login(email: String, password: String): Result<LoginResponse> {
    return try {
        val response = api.login(LoginRequest(email, password))
        
        if (response.isSuccessful) {
            val body = response.body()
            if (body?.success == true && body.data != null) {
                val tokens = body.data.tokens
                val userId = body.data.user.id // Extract user ID
                
                // Save tokens AND user ID
                tokenStore.saveTokens(
                    tokens.accessToken, 
                    tokens.refreshToken, 
                    tokens.expiresAt,
                    userId // NEW
                )
                
                Result.success(body.data)
            } else {
                Result.failure(Exception(body?.error ?: "Login failed"))
            }
        } else {
            Result.failure(Exception(parseApiError(response.errorBody())))
        }
    } catch (e: Exception) {
        Result.failure(e)
    }
}
```

---

## 📊 Event Flow After Fix

```
1. User logs in
   ↓
2. AuthRepositoryImpl.login() saves tokens + userId
   ↓
3. Navigate to MainActivity
   ↓
4. MainActivity.onCreate() calls realtimeManager.initialize(userId) ✅ NEW
   ↓
5. RealtimeManager subscribes to:
   - user:{userId}
   - products
   - cart:{userId} ✅ CRITICAL
   ↓
6. Phone A: User adds item to cart
   ↓
7. Backend: eventBroadcaster.cartUpdated() broadcasts to cart:{userId}
   ↓
8. Phone A & B: RealtimeManager receives broadcast ✅
   ↓
9. EventBus publishes Event.CartItemAdded ✅
   ↓
10. CartViewModel receives event → refreshCart() ✅
    ↓
11. Both phones: Cart UI updates automatically ✅
```

---

## 🧪 Testing Checklist

After implementing the fix:

- [ ] Build and install app on 2 devices
- [ ] Login as `abcd@gmail.com` on both devices
- [ ] Check logcat for: `RealtimeManager initialized for user: {userId}`
- [ ] Check logcat for: `Subscribed to cart:{userId}`
- [ ] Phone A: Add item to cart
- [ ] Phone B: Verify cart updates within 1-2 seconds (no manual refresh)
- [ ] Phone A: Update quantity
- [ ] Phone B: Verify quantity updates automatically
- [ ] Phone A: Remove item
- [ ] Phone B: Verify item removed automatically

---

## 📝 Files to Modify

| File | Change | Lines |
|------|--------|-------|
| `MainActivity.kt` | Inject & initialize RealtimeManager | +20 |
| `TokenStore.kt` | Add getUserId() + save userId | +10 |
| `AuthRepositoryImpl.kt` | Pass userId to saveTokens() | +5 |

**Total**: 3 files, ~35 lines

---

## 🎯 Priority

**CRITICAL** - This is the root cause preventing the entire event-driven architecture from working.

Without this fix:
- ❌ No real-time cart sync
- ❌ No real-time order status updates
- ❌ No real-time product stock updates
- ❌ No real-time delivery tracking

---

## 📚 Related Documentation

- `EVENT_DRIVEN_ARCHITECTURE_COMPLETE.md` - Complete architecture overview
- `PHASE2_ANDROID_EVENT_INFRASTRUCTURE_COMPLETE.md` - RealtimeManager implementation  
- `CART_REALTIME_SYNC_FIX.md` - Previous fix (subscription logic was correct)

---

**Status**: ✅ Root cause identified  
**Next Action**: Implement the 3 fixes above  
**Expected Outcome**: Full real-time synchronization across all features
