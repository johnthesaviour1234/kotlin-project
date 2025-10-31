# 🚀 Realtime Sync Implementation Guide

## Overview
This guide provides step-by-step instructions for implementing the Realtime Sync Migration Plan for the Grocery Delivery System.

**Date**: January 30, 2025  
**Version**: 1.0.0  
**Estimated Time**: 1 week development + 2-3 weeks staged rollout

---

## ✅ Prerequisites Checklist

Before starting implementation, verify:

- [ ] Supabase project is active (ID: hfxdxxpmcemdjsvhsdcf)
- [ ] All 12 database tables exist and have RLS enabled
- [ ] Backend API is deployed and running on Vercel
- [ ] Test credentials are working:
  - Customer: abcd@gmail.com / Password123
  - Admin: admin@grocery.com / AdminPass123
  - Delivery: driver@grocery.com / DriverPass123
- [ ] Android development environment is set up (E:\Android)
- [ ] All three Android apps have basic structure in place

---

## 📋 Phase 1: Backend Setup (Day 1)

### Step 1.1: Apply Supabase Migration

**File**: `supabase_realtime_migration.sql` (already created)

**Option A: Using Supabase Dashboard**
1. Go to https://supabase.com/dashboard/project/hfxdxxpmcemdjsvhsdcf
2. Navigate to SQL Editor
3. Open `supabase_realtime_migration.sql`
4. Copy entire content
5. Paste into SQL Editor
6. Click "Run" button
7. Verify "Success" message

**Option B: Using Supabase CLI** (if installed)
```powershell
Set-Location "E:\warp projects\kotlin mobile application"
supabase db push supabase_realtime_migration.sql
```

**Expected Result**:
- 5 trigger functions created
- 5 triggers attached to tables
- Realtime enabled on cart, orders, delivery_assignments, delivery_locations tables
- Permissions granted

**Verification**:
```sql
-- Check if functions exist
SELECT routine_name 
FROM information_schema.routines 
WHERE routine_schema = 'public' 
AND routine_name LIKE 'broadcast_%';

-- Expected output: 5 functions listed
-- broadcast_cart_changes
-- broadcast_order_placed
-- broadcast_order_status_changed
-- broadcast_delivery_assignment
-- broadcast_delivery_location

-- Check if triggers exist
SELECT trigger_name, event_object_table 
FROM information_schema.triggers 
WHERE trigger_schema = 'public' 
AND trigger_name LIKE 'broadcast_%';

-- Expected output: 5 triggers listed
```

### Step 1.2: Test Backend Triggers

Run these test queries in Supabase SQL Editor:

```sql
-- Test 1: Cart update (should trigger broadcast)
UPDATE cart 
SET quantity = quantity + 1 
WHERE user_id = (SELECT id FROM auth.users WHERE email = 'abcd@gmail.com' LIMIT 1)
AND id = (SELECT id FROM cart LIMIT 1);

-- Test 2: Order status change (should trigger broadcast)
UPDATE orders 
SET status = 'confirmed' 
WHERE customer_id = (SELECT id FROM auth.users WHERE email = 'abcd@gmail.com' LIMIT 1)
AND id = (SELECT id FROM orders ORDER BY created_at DESC LIMIT 1);

-- Test 3: Check if pg_notify was called (Supabase logs)
-- Go to Dashboard → Logs → Database Logs
-- Look for "NOTIFY" messages
```

**Expected**: You should see pg_notify() calls in the Supabase logs with the correct channel names.

### Step 1.3: Enable Realtime in Supabase Dashboard

1. Go to https://supabase.com/dashboard/project/hfxdxxpmcemdjsvhsdcf/settings/api
2. Navigate to "Settings" → "API"
3. Scroll to "Realtime" section
4. Ensure "Realtime" is enabled
5. Note the "Realtime URL": `wss://hfxdxxpmcemdjsvhsdcf.supabase.co/realtime/v1`
6. Verify quotas: Should support concurrent connections

---

## 📋 Phase 2: GroceryCustomer App Implementation (Days 2-3)

### Step 2.1: Add Supabase Realtime Dependencies

**File**: `GroceryCustomer/app/build.gradle.kts`

Add these dependencies:

```kotlin
dependencies {
    // Existing dependencies...
    
    // Supabase Realtime (Add these)
    implementation("io.github.jan-tennert.supabase:realtime-kt:2.6.0")
    implementation("io.github.jan-tennert.supabase:postgrest-kt:2.6.0")
    implementation("io.ktor:ktor-client-cio:2.3.7") // Required for WebSockets
    implementation("io.ktor:ktor-client-websockets:2.3.7")
}
```

**Sync Gradle** after adding dependencies.

### Step 2.2: Update BuildConfig with Supabase Credentials

**File**: `GroceryCustomer/app/build.gradle.kts`

Add build config fields:

```kotlin
android {
    // ... existing config
    
    defaultConfig {
        // ... existing config
        
        buildConfigField("String", "SUPABASE_URL", "\"https://hfxdxxpmcemdjsvhsdcf.supabase.co\"")
        buildConfigField("String", "SUPABASE_ANON_KEY", "\"YOUR_ANON_KEY_HERE\"")
    }
}
```

**Note**: Get SUPABASE_ANON_KEY from:
https://supabase.com/dashboard/project/hfxdxxpmcemdjsvhsdcf/settings/api

Or use the MCP tool:
```kotlin
// Use the get_anon_key MCP tool
// project_id: hfxdxxpmcemdjsvhsdcf
```

### Step 2.3: Create Supabase Client Module

**File**: `GroceryCustomer/app/src/main/java/com/grocery/customer/di/SupabaseModule.kt`

```kotlin
package com.grocery.customer.di

import android.content.Context
import com.grocery.customer.BuildConfig
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.createSupabaseClient
import io.github.jan.supabase.gotrue.Auth
import io.github.jan.supabase.postgrest.Postgrest
import io.github.jan.supabase.realtime.Realtime
import io.github.jan.supabase.realtime.RealtimeChannel
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object SupabaseModule {
    
    @Provides
    @Singleton
    fun provideSupabaseClient(
        @ApplicationContext context: Context
    ): SupabaseClient {
        return createSupabaseClient(
            supabaseUrl = BuildConfig.SUPABASE_URL,
            supabaseKey = BuildConfig.SUPABASE_ANON_KEY
        ) {
            install(Auth)
            install(Postgrest)
            install(Realtime) {
                // Realtime configuration
            }
        }
    }
}
```

### Step 2.4: Create RealtimeManager

**File**: `GroceryCustomer/app/src/main/java/com/grocery/customer/data/sync/RealtimeManager.kt`

```kotlin
package com.grocery.customer.data.sync

import android.util.Log
import com.grocery.customer.domain.repository.CartRepository
import com.grocery.customer.domain.repository.OrderRepository
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.realtime.RealtimeChannel
import io.github.jan.supabase.realtime.channel
import io.github.jan.supabase.realtime.realtime
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonPrimitive
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class RealtimeManager @Inject constructor(
    private val supabaseClient: SupabaseClient,
    private val cartRepository: CartRepository,
    private val orderRepository: OrderRepository
) {
    private companion object {
        const val TAG = "RealtimeManager"
    }
    
    private var cartChannel: RealtimeChannel? = null
    private var ordersChannel: RealtimeChannel? = null
    
    private val _connectionState = MutableStateFlow<RealtimeConnectionState>(
        RealtimeConnectionState.Disconnected
    )
    val connectionState: StateFlow<RealtimeConnectionState> = _connectionState
    
    /**
     * Subscribe to cart changes for the current user
     * Topic: cart:{user_id}
     */
    suspend fun subscribeToCartChanges(userId: String) {
        try {
            Log.d(TAG, "Subscribing to cart changes for user: $userId")
            
            // Unsubscribe from existing channel if any
            unsubscribeFromCart()
            
            // Create new channel
            cartChannel = supabaseClient.realtime.createChannel("cart:$userId") {
                // Channel configuration
            }
            
            // Listen for all cart events
            cartChannel?.on<JsonObject>(event = "CART_ITEM_ADDED") { payload ->
                Log.d(TAG, "Cart item added: $payload")
                handleCartChange()
            }
            
            cartChannel?.on<JsonObject>(event = "CART_ITEM_UPDATED") { payload ->
                Log.d(TAG, "Cart item updated: $payload")
                handleCartChange()
            }
            
            cartChannel?.on<JsonObject>(event = "CART_ITEM_REMOVED") { payload ->
                Log.d(TAG, "Cart item removed: $payload")
                handleCartChange()
            }
            
            // Subscribe to the channel
            cartChannel?.subscribe()
            
            _connectionState.value = RealtimeConnectionState.Connected
            Log.d(TAG, "Successfully subscribed to cart changes")
            
        } catch (e: Exception) {
            Log.e(TAG, "Failed to subscribe to cart changes", e)
            _connectionState.value = RealtimeConnectionState.Error(e.message ?: "Unknown error")
        }
    }
    
    /**
     * Subscribe to order changes for the current user
     * Topic: orders:{user_id}
     */
    suspend fun subscribeToOrderChanges(userId: String) {
        try {
            Log.d(TAG, "Subscribing to order changes for user: $userId")
            
            // Unsubscribe from existing channel if any
            unsubscribeFromOrders()
            
            // Create new channel
            ordersChannel = supabaseClient.realtime.createChannel("orders:$userId") {
                // Channel configuration
            }
            
            // Listen for order placed
            ordersChannel?.on<JsonObject>(event = "ORDER_PLACED") { payload ->
                Log.d(TAG, "Order placed: $payload")
                handleOrderChange()
            }
            
            // Listen for order confirmed
            ordersChannel?.on<JsonObject>(event = "ORDER_CONFIRMED") { payload ->
                Log.d(TAG, "Order confirmed: $payload")
                handleOrderChange()
            }
            
            // Listen for order assigned to delivery
            ordersChannel?.on<JsonObject>(event = "ORDER_ASSIGNED") { payload ->
                Log.d(TAG, "Order assigned to delivery: $payload")
                handleOrderChange()
                showOrderAssignedNotification(payload)
            }
            
            // Listen for order status changes
            ordersChannel?.on<JsonObject>(event = "ORDER_STATUS_CHANGED") { payload ->
                Log.d(TAG, "Order status changed: $payload")
                handleOrderChange()
                showOrderStatusNotification(payload)
            }
            
            // Listen for delivery location updates
            ordersChannel?.on<JsonObject>(event = "DELIVERY_LOCATION_UPDATE") { payload ->
                Log.d(TAG, "Delivery location updated: $payload")
                // Could update UI with delivery person location
            }
            
            // Subscribe to the channel
            ordersChannel?.subscribe()
            
            Log.d(TAG, "Successfully subscribed to order changes")
            
        } catch (e: Exception) {
            Log.e(TAG, "Failed to subscribe to order changes", e)
        }
    }
    
    /**
     * Unsubscribe from cart changes
     */
    suspend fun unsubscribeFromCart() {
        try {
            cartChannel?.unsubscribe()
            cartChannel = null
            Log.d(TAG, "Unsubscribed from cart changes")
        } catch (e: Exception) {
            Log.e(TAG, "Error unsubscribing from cart", e)
        }
    }
    
    /**
     * Unsubscribe from order changes
     */
    suspend fun unsubscribeFromOrders() {
        try {
            ordersChannel?.unsubscribe()
            ordersChannel = null
            Log.d(TAG, "Unsubscribed from order changes")
        } catch (e: Exception) {
            Log.e(TAG, "Error unsubscribing from orders", e)
        }
    }
    
    /**
     * Unsubscribe from all channels
     */
    suspend fun unsubscribeAll() {
        unsubscribeFromCart()
        unsubscribeFromOrders()
        _connectionState.value = RealtimeConnectionState.Disconnected
        Log.d(TAG, "Unsubscribed from all channels")
    }
    
    private suspend fun handleCartChange() {
        try {
            cartRepository.refreshCart()
            Log.d(TAG, "Cart refreshed after realtime update")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to refresh cart after realtime update", e)
        }
    }
    
    private suspend fun handleOrderChange() {
        try {
            orderRepository.refreshOrders()
            Log.d(TAG, "Orders refreshed after realtime update")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to refresh orders after realtime update", e)
        }
    }
    
    private fun showOrderAssignedNotification(payload: JsonObject) {
        // TODO: Implement notification using NotificationManager
        // Show: "Your order has been assigned to a delivery person"
    }
    
    private fun showOrderStatusNotification(payload: JsonObject) {
        // TODO: Implement notification using NotificationManager
        // Extract new_status from payload and show appropriate message
        val newStatus = payload["new_status"]?.jsonPrimitive?.content
        // Show: "Your order is now: $newStatus"
    }
}

sealed class RealtimeConnectionState {
    object Disconnected : RealtimeConnectionState()
    object Connecting : RealtimeConnectionState()
    object Connected : RealtimeConnectionState()
    data class Error(val message: String) : RealtimeConnectionState()
}
```

### Step 2.5: Update MainActivity for Lifecycle Management

**File**: `GroceryCustomer/app/src/main/java/com/grocery/customer/ui/activities/MainActivity.kt`

Add realtime subscription lifecycle:

```kotlin
package com.grocery.customer.ui.activities

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.lifecycle.lifecycleScope
import com.grocery.customer.data.sync.RealtimeManager
import com.grocery.customer.domain.repository.AuthRepository
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    
    @Inject
    lateinit var realtimeManager: RealtimeManager
    
    @Inject
    lateinit var authRepository: AuthRepository
    
    private var currentUserId: String? = null
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Observe user session
        lifecycleScope.launch {
            authRepository.getCurrentUser()?.let { user ->
                currentUserId = user.id
            }
        }
    }
    
    override fun onResume() {
        super.onResume()
        
        // Subscribe to realtime when app comes to foreground
        lifecycleScope.launch {
            currentUserId?.let { userId ->
                realtimeManager.subscribeToCartChanges(userId)
                realtimeManager.subscribeToOrderChanges(userId)
            }
        }
    }
    
    override fun onPause() {
        super.onPause()
        
        // Unsubscribe when app goes to background to save battery
        lifecycleScope.launch {
            realtimeManager.unsubscribeAll()
        }
    }
    
    override fun onDestroy() {
        super.onDestroy()
        
        // Cleanup on destroy
        lifecycleScope.launch {
            realtimeManager.unsubscribeAll()
        }
    }
}
```

### Step 2.6: Update Repositories for Refresh Methods

**File**: `GroceryCustomer/app/src/main/java/com/grocery/customer/domain/repository/CartRepository.kt`

Add refresh method interface:

```kotlin
interface CartRepository {
    // Existing methods...
    
    /**
     * Refresh cart from server after realtime update
     */
    suspend fun refreshCart(): Result<Unit>
}
```

**File**: `GroceryCustomer/app/src/main/java/com/grocery/customer/data/repository/CartRepositoryImpl.kt`

Implement refresh:

```kotlin
override suspend fun refreshCart(): Result<Unit> {
    return try {
        // Fetch latest cart from API
        val response = apiService.getCart()
        if (response.success && response.data != null) {
            // Update local state or emit to flow
            // Notify observers that cart has changed
            Result.success(Unit)
        } else {
            Result.failure(Exception(response.error ?: "Failed to refresh cart"))
        }
    } catch (e: Exception) {
        Result.failure(e)
    }
}
```

**File**: `GroceryCustomer/app/src/main/java/com/grocery/customer/domain/repository/OrderRepository.kt`

Add refresh method:

```kotlin
interface OrderRepository {
    // Existing methods...
    
    /**
     * Refresh orders from server after realtime update
     */
    suspend fun refreshOrders(): Result<Unit>
}
```

### Step 2.7: Build and Test

```powershell
Set-Location "E:\warp projects\kotlin mobile application\GroceryCustomer"
.\gradlew assembleDebug

& "E:\Android\Sdk\platform-tools\adb.exe" uninstall com.grocery.customer.debug
& "E:\Android\Sdk\platform-tools\adb.exe" install "app\build\outputs\apk\debug\app-debug.apk"
& "E:\Android\Sdk\platform-tools\adb.exe" shell monkey -p com.grocery.customer -c android.intent.category.LAUNCHER 1

# Watch logs for realtime events
Start-Process powershell -ArgumentList "-NoExit", "-Command", "& 'E:\Android\Sdk\platform-tools\adb.exe' logcat | Select-String 'RealtimeManager|cart|order|realtime'"
```

**Manual Testing**:
1. Login to customer app on Device A
2. Login to same account on Device B (or web browser)
3. Add item to cart on Device A
4. **Expected**: Device B shows the item within 1-2 seconds (check logs for "Cart item added")

---

## 📋 Phase 3: Admin App Implementation (Day 4)

Similar structure to Customer App, but with different topics:
- Subscribe to: `orders:new` (all new orders)
- Subscribe to: `orders:*` (all order updates)

**Key Files to Create**:
1. `GroceryAdmin/app/src/main/java/com/grocery/admin/di/SupabaseModule.kt`
2. `GroceryAdmin/app/src/main/java/com/grocery/admin/data/sync/AdminRealtimeManager.kt`
3. Update MainActivity with lifecycle management

---

## 📋 Phase 4: Delivery App Implementation (Day 5)

Topics to subscribe to:
- `delivery:{delivery_person_id}` (assigned orders)
- `orders:confirmed:*` (available orders pool)

**Key Files to Create**:
1. `GroceryDelivery/app/src/main/java/com/grocery/delivery/di/SupabaseModule.kt`
2. `GroceryDelivery/app/src/main/java/com/grocery/delivery/data/sync/DeliveryRealtimeManager.kt`
3. Update MainActivity with lifecycle management

---

## 📋 Phase 5: Integration Testing (Days 6-7)

### Test Scenarios:

**Test 1: Multi-Device Cart Sync**
1. Login on Phone A (Customer)
2. Login on Phone B (Customer, same account)
3. Add item to cart on Phone A
4. **Expected**: Phone B shows new item within 1-2 seconds

**Test 2: End-to-End Order Flow**
1. Customer places order
2. **Expected**: Admin app shows new order immediately
3. Admin confirms order
4. **Expected**: Delivery app shows order in available pool
5. Admin assigns order to delivery person
6. **Expected**: Delivery app shows order in "My Deliveries"
7. Delivery person updates status to "out_for_delivery"
8. **Expected**: Customer app shows updated status
9. Delivery person marks as delivered
10. **Expected**: Customer app shows "Delivered" status

**Test 3: Offline Resilience**
1. Customer places order while online
2. Disconnect Admin app from network
3. Reconnect Admin app
4. **Expected**: Admin app catches up via fallback sync

---

## 📋 Phase 6: Deployment Strategy (Weeks 2-4)

### Stage 1: Internal Testing (QA Team)
- Duration: 1 week
- Rollout: QA devices only
- Rollback threshold: Any critical bug

### Stage 2: Alpha Testing
- Duration: 1 week
- Rollout: 5% of users
- Monitor: Error rate, battery drain, connection stability
- Rollback threshold: >5% error rate

### Stage 3: Beta Testing
- Duration: 2 weeks
- Rollout: 25% of users
- Rollback threshold: >2% error rate

### Stage 4: General Availability
- Rollout: 100% of users
- Ongoing monitoring

---

## 🎯 Success Metrics

| Metric | Target | Measurement |
|--------|--------|-------------|
| Cart sync latency | < 2 seconds | Time from trigger to UI update |
| Order update latency | < 2 seconds | Time from status change to notification |
| WebSocket uptime | > 95% | Connection stability over 24 hours |
| Battery drain increase | < 5% | Compare before/after realtime implementation |
| User satisfaction | > 90% | Survey after rollout |

---

## ⚠️ Rollback Plan

If critical issues occur:

1. **Immediate**: Disable realtime via remote config
2. **Fallback**: Re-enable WorkManager polling
3. **Investigate**: Review logs and error reports
4. **Fix**: Address issues and re-deploy

**Remote Config Flag**:
```kotlin
if (remoteConfig.getBoolean("realtime_sync_enabled")) {
    realtimeManager.subscribeToCartChanges(userId)
} else {
    BackgroundSyncWorker.scheduleForegroundSync(context)
}
```

---

## 📚 Additional Resources

- **Supabase Realtime Docs**: https://supabase.com/docs/guides/realtime
- **Supabase Kotlin Client**: https://github.com/supabase-community/supabase-kt
- **Project Status**: See PROJECT_STATUS_AND_NEXT_STEPS.md
- **API Integration**: See API_INTEGRATION_GUIDE.md
- **Design System**: See DESIGN_SYSTEM_GUIDE.md

---

## ✅ Next Steps

1. **Review this guide** with the team
2. **Apply database migration** (Phase 1, Step 1.1)
3. **Test backend triggers** (Phase 1, Step 1.2)
4. **Implement Customer App** (Phase 2)
5. **Proceed with Admin and Delivery apps** (Phases 3-4)
6. **Conduct integration testing** (Phase 5)
7. **Begin staged rollout** (Phase 6)

---

**Document Version**: 1.0.0  
**Last Updated**: January 30, 2025  
**Status**: Ready for implementation
