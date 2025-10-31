# 📋 Complete Multi-App Real-Time Sync Migration Plan

## **Overview**
Build a **multi-tenant real-time system** where Customer App, Admin App, and Delivery App stay synchronized via Supabase Realtime Broadcast.

---

## **System Architecture** 🏗️

### **Data Flow**

```
Customer App → Backend → [Admin App, Other Customer Devices]
Admin App → Backend → [Delivery App, Customer App]
Delivery App → Backend → [Admin App, Customer App]
```

### **Broadcast Topics Strategy**

| Event | Topic | Subscribers |
|-------|-------|-------------|
| **Cart updated** | `cart:{user_id}` | Customer's all devices |
| **Order placed** | `orders:new`, `orders:{user_id}` | Admin App, Customer's devices |
| **Order confirmed** | `orders:confirmed:{order_id}`, `orders:{user_id}` | Delivery App, Customer's devices |
| **Order assigned** | `delivery:{delivery_person_id}`, `orders:{order_id}` | Delivery App, Admin App |
| **Order status updated** | `orders:{order_id}`, `orders:{user_id}` | Customer App, Admin App |
| **Order delivered** | `orders:{order_id}`, `orders:{user_id}` | Customer App, Admin App |

---

## **Phase 1: Backend Setup** 🗄️

### **Step 1.1: Enable Realtime on Supabase**
- [ ] Enable Realtime in Supabase Dashboard
- [ ] Verify `realtime.messages` table exists
- [ ] Set up Realtime quotas for production load

### **Step 1.2: Create RLS Policies**

```sql
-- Policy: Customers can receive their own cart/order broadcasts
CREATE POLICY "customers_receive_own_broadcasts"
ON "realtime"."messages"
FOR SELECT
TO authenticated
USING (
  (topic = 'cart:' || auth.uid()::text) OR
  (topic = 'orders:' || auth.uid()::text) OR
  (topic LIKE 'orders:%' AND EXISTS (
    SELECT 1 FROM public.orders 
    WHERE orders.id::text = SUBSTRING(topic FROM 8) 
    AND orders.user_id = auth.uid()
  ))
);

-- Policy: Admin can receive all order broadcasts
CREATE POLICY "admin_receive_all_orders"
ON "realtime"."messages"
FOR SELECT
TO authenticated
USING (
  (auth.jwt() ->> 'role' = 'admin') AND
  (topic LIKE 'orders:%' OR topic = 'orders:new')
);

-- Policy: Delivery can receive assigned order broadcasts
CREATE POLICY "delivery_receive_assigned_orders"
ON "realtime"."messages"
FOR SELECT
TO authenticated
USING (
  (auth.jwt() ->> 'role' = 'delivery') AND
  (
    topic = 'delivery:' || auth.uid()::text OR
    topic LIKE 'orders:confirmed:%' OR
    topic LIKE 'delivery:%'
  )
);
```

### **Step 1.3: Create Broadcast Trigger Functions**

#### **Cart Changes (Customer → Customer)**
```sql
CREATE OR REPLACE FUNCTION public.broadcast_cart_changes()
RETURNS trigger
SECURITY DEFINER SET search_path = ''
LANGUAGE plpgsql
AS $$
DECLARE
    user_id_text text;
BEGIN
    user_id_text := COALESCE(NEW.user_id, OLD.user_id)::text;
    
    -- Broadcast to user's all devices
    PERFORM realtime.broadcast_changes(
        'cart:' || user_id_text,
        TG_OP,
        TG_OP,
        TG_TABLE_NAME,
        TG_TABLE_SCHEMA,
        NEW,
        OLD
    );
    
    RETURN NULL;
END;
$$;
```

#### **Order Placed (Customer → Admin + Customer)**
```sql
CREATE OR REPLACE FUNCTION public.broadcast_order_placed()
RETURNS trigger
SECURITY DEFINER SET search_path = ''
LANGUAGE plpgsql
AS $$
BEGIN
    IF (TG_OP = 'INSERT') THEN
        -- Broadcast to admin (all new orders)
        PERFORM realtime.broadcast_changes(
            'orders:new',
            'ORDER_PLACED',
            TG_OP,
            TG_TABLE_NAME,
            TG_TABLE_SCHEMA,
            NEW,
            NULL
        );
        
        -- Broadcast to customer's devices
        PERFORM realtime.broadcast_changes(
            'orders:' || NEW.user_id::text,
            'ORDER_PLACED',
            TG_OP,
            TG_TABLE_NAME,
            TG_TABLE_SCHEMA,
            NEW,
            NULL
        );
    END IF;
    
    RETURN NULL;
END;
$$;
```

#### **Order Confirmed (Admin → Delivery + Customer)**
```sql
CREATE OR REPLACE FUNCTION public.broadcast_order_confirmed()
RETURNS trigger
SECURITY DEFINER SET search_path = ''
LANGUAGE plpgsql
AS $$
BEGIN
    -- Check if status changed to 'confirmed'
    IF (TG_OP = 'UPDATE' AND OLD.status != 'confirmed' AND NEW.status = 'confirmed') THEN
        
        -- Broadcast to delivery pool (all delivery persons)
        PERFORM realtime.broadcast_changes(
            'orders:confirmed:' || NEW.id::text,
            'ORDER_CONFIRMED',
            TG_OP,
            TG_TABLE_NAME,
            TG_TABLE_SCHEMA,
            NEW,
            OLD
        );
        
        -- Broadcast to customer
        PERFORM realtime.broadcast_changes(
            'orders:' || NEW.user_id::text,
            'ORDER_CONFIRMED',
            TG_OP,
            TG_TABLE_NAME,
            TG_TABLE_SCHEMA,
            NEW,
            OLD
        );
        
    END IF;
    
    RETURN NULL;
END;
$$;
```

#### **Order Assigned (Admin → Delivery + Customer)**
```sql
CREATE OR REPLACE FUNCTION public.broadcast_order_assigned()
RETURNS trigger
SECURITY DEFINER SET search_path = ''
LANGUAGE plpgsql
AS $$
BEGIN
    -- Check if delivery_person_id was assigned
    IF (TG_OP = 'UPDATE' AND OLD.delivery_person_id IS NULL AND NEW.delivery_person_id IS NOT NULL) THEN
        
        -- Broadcast to assigned delivery person
        PERFORM realtime.broadcast_changes(
            'delivery:' || NEW.delivery_person_id::text,
            'ORDER_ASSIGNED',
            TG_OP,
            TG_TABLE_NAME,
            TG_TABLE_SCHEMA,
            NEW,
            OLD
        );
        
        -- Broadcast to customer
        PERFORM realtime.broadcast_changes(
            'orders:' || NEW.user_id::text,
            'ORDER_ASSIGNED',
            TG_OP,
            TG_TABLE_NAME,
            TG_TABLE_SCHEMA,
            NEW,
            OLD
        );
        
        -- Broadcast to admin
        PERFORM realtime.broadcast_changes(
            'orders:' || NEW.id::text,
            'ORDER_ASSIGNED',
            TG_OP,
            TG_TABLE_NAME,
            TG_TABLE_SCHEMA,
            NEW,
            OLD
        );
        
    END IF;
    
    RETURN NULL;
END;
$$;
```

#### **Order Status Updated (Delivery → Customer + Admin)**
```sql
CREATE OR REPLACE FUNCTION public.broadcast_order_status_changed()
RETURNS trigger
SECURITY DEFINER SET search_path = ''
LANGUAGE plpgsql
AS $$
BEGIN
    -- Check if status changed
    IF (TG_OP = 'UPDATE' AND OLD.status != NEW.status) THEN
        
        -- Broadcast to customer
        PERFORM realtime.broadcast_changes(
            'orders:' || NEW.user_id::text,
            'ORDER_STATUS_CHANGED',
            TG_OP,
            TG_TABLE_NAME,
            TG_TABLE_SCHEMA,
            NEW,
            OLD
        );
        
        -- Broadcast to admin
        PERFORM realtime.broadcast_changes(
            'orders:' || NEW.id::text,
            'ORDER_STATUS_CHANGED',
            TG_OP,
            TG_TABLE_NAME,
            TG_TABLE_SCHEMA,
            NEW,
            OLD
        );
        
        -- If delivered, notify delivery person too
        IF (NEW.status = 'delivered' AND NEW.delivery_person_id IS NOT NULL) THEN
            PERFORM realtime.broadcast_changes(
                'delivery:' || NEW.delivery_person_id::text,
                'ORDER_DELIVERED',
                TG_OP,
                TG_TABLE_NAME,
                TG_TABLE_SCHEMA,
                NEW,
                OLD
            );
        END IF;
        
    END IF;
    
    RETURN NULL;
END;
$$;
```

### **Step 1.4: Attach Triggers to Tables**

```sql
-- Cart items trigger
DROP TRIGGER IF EXISTS broadcast_cart_changes_trigger ON public.cart_items;
CREATE TRIGGER broadcast_cart_changes_trigger
AFTER INSERT OR UPDATE OR DELETE ON public.cart_items
FOR EACH ROW
EXECUTE FUNCTION public.broadcast_cart_changes();

-- Order placed trigger (on INSERT)
DROP TRIGGER IF EXISTS broadcast_order_placed_trigger ON public.orders;
CREATE TRIGGER broadcast_order_placed_trigger
AFTER INSERT ON public.orders
FOR EACH ROW
EXECUTE FUNCTION public.broadcast_order_placed();

-- Order confirmed trigger (on status change to 'confirmed')
DROP TRIGGER IF EXISTS broadcast_order_confirmed_trigger ON public.orders;
CREATE TRIGGER broadcast_order_confirmed_trigger
AFTER UPDATE ON public.orders
FOR EACH ROW
EXECUTE FUNCTION public.broadcast_order_confirmed();

-- Order assigned trigger (on delivery_person_id assignment)
DROP TRIGGER IF EXISTS broadcast_order_assigned_trigger ON public.orders;
CREATE TRIGGER broadcast_order_assigned_trigger
AFTER UPDATE ON public.orders
FOR EACH ROW
EXECUTE FUNCTION public.broadcast_order_assigned();

-- Order status changed trigger (on any status change)
DROP TRIGGER IF EXISTS broadcast_order_status_changed_trigger ON public.orders;
CREATE TRIGGER broadcast_order_status_changed_trigger
AFTER UPDATE ON public.orders
FOR EACH ROW
EXECUTE FUNCTION public.broadcast_order_status_changed();
```

### **Step 1.5: Test Backend Triggers**

```sql
-- Test 1: Cart update
INSERT INTO cart_items (user_id, product_id, quantity) 
VALUES ('test-customer-id', 1, 2);

-- Test 2: Order placement
INSERT INTO orders (user_id, total_amount, status) 
VALUES ('test-customer-id', 50.00, 'pending');

-- Test 3: Order confirmation
UPDATE orders SET status = 'confirmed' WHERE id = 'test-order-id';

-- Test 4: Order assignment
UPDATE orders SET delivery_person_id = 'test-delivery-id' WHERE id = 'test-order-id';

-- Test 5: Order status change
UPDATE orders SET status = 'out_for_delivery' WHERE id = 'test-order-id';

-- Verify messages in realtime.messages
SELECT * FROM realtime.messages 
ORDER BY inserted_at DESC 
LIMIT 10;
```

---

## **Phase 2: Customer App (Android Client)** 📱

### **Step 2.1: Add Dependencies**

```kotlin
// app/build.gradle.kts
dependencies {
    // Existing dependencies...
    
    // Supabase Realtime
    implementation("io.github.jan-tennert.supabase:realtime-kt:2.6.0")
    implementation("io.ktor:ktor-client-cio:2.3.7") // Required for WebSockets
}
```

### **Step 2.2: Update Supabase Client Configuration**

```kotlin
// di/NetworkModule.kt
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
```

### **Step 2.3: Create RealtimeManager (Customer App)**

```kotlin
// data/sync/RealtimeManager.kt
package com.grocery.customer.data.sync

import android.util.Log
import com.grocery.customer.domain.repository.CartRepository
import com.grocery.customer.domain.repository.OrderRepository
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.realtime.RealtimeChannel
import io.github.jan.supabase.realtime.channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.serialization.json.JsonObject
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
     */
    suspend fun subscribeToCartChanges(userId: String) {
        try {
            Log.d(TAG, "Subscribing to cart changes for user: $userId")
            
            cartChannel = supabaseClient.channel("cart:$userId")
            
            // Listen for all cart events
            cartChannel?.on<JsonObject>(event = "INSERT") { payload ->
                Log.d(TAG, "Cart item added: $payload")
                handleCartChange()
            }
            
            cartChannel?.on<JsonObject>(event = "UPDATE") { payload ->
                Log.d(TAG, "Cart item updated: $payload")
                handleCartChange()
            }
            
            cartChannel?.on<JsonObject>(event = "DELETE") { payload ->
                Log.d(TAG, "Cart item removed: $payload")
                handleCartChange()
            }
            
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
     */
    suspend fun subscribeToOrderChanges(userId: String) {
        try {
            Log.d(TAG, "Subscribing to order changes for user: $userId")
            
            ordersChannel = supabaseClient.channel("orders:$userId")
            
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
            }
            
            // Listen for order status changes
            ordersChannel?.on<JsonObject>(event = "ORDER_STATUS_CHANGED") { payload ->
                Log.d(TAG, "Order status changed: $payload")
                handleOrderChange()
            }
            
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
}

sealed class RealtimeConnectionState {
    object Disconnected : RealtimeConnectionState()
    object Connecting : RealtimeConnectionState()
    object Connected : RealtimeConnectionState()
    data class Error(val message: String) : RealtimeConnectionState()
}
```

### **Step 2.4: Update MainActivity for Lifecycle Management**

```kotlin
// ui/activities/MainActivity.kt
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
            authRepository.currentSession.collect { session ->
                currentUserId = session?.user?.id
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
}
```

### **Step 2.5: Fix WorkManager Intervals (Fallback Sync)**

```kotlin
// data/workers/BackgroundSyncWorker.kt
companion object {
    // Fix intervals to respect WorkManager minimum (15 minutes)
    private const val SYNC_INTERVAL_FOREGROUND_MINUTES = 15L
    private const val SYNC_INTERVAL_BACKGROUND_MINUTES = 30L
    
    fun scheduleForegroundSync(context: Context) {
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()
        
        val syncRequest = PeriodicWorkRequestBuilder<BackgroundSyncWorker>(
            SYNC_INTERVAL_FOREGROUND_MINUTES,
            TimeUnit.MINUTES,
            5, // Flex interval
            TimeUnit.MINUTES
        )
            .setConstraints(constraints)
            .setBackoffCriteria(
                BackoffPolicy.EXPONENTIAL,
                WorkRequest.MIN_BACKOFF_MILLIS,
                TimeUnit.MILLISECONDS
            )
            .addTag("sync")
            .addTag("foreground")
            .build()
        
        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
            WORK_NAME_FOREGROUND,
            ExistingPeriodicWorkPolicy.KEEP,
            syncRequest
        )
        
        Log.d(TAG, "Foreground sync scheduled (every $SYNC_INTERVAL_FOREGROUND_MINUTES minutes)")
    }
    
    fun scheduleBackgroundSync(context: Context) {
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .setRequiresBatteryNotLow(true)
            .build()
        
        val syncRequest = PeriodicWorkRequestBuilder<BackgroundSyncWorker>(
            SYNC_INTERVAL_BACKGROUND_MINUTES,
            TimeUnit.MINUTES,
            5,
            TimeUnit.MINUTES
        )
            .setConstraints(constraints)
            .setBackoffCriteria(
                BackoffPolicy.EXPONENTIAL,
                WorkRequest.MIN_BACKOFF_MILLIS,
                TimeUnit.MILLISECONDS
            )
            .addTag("sync")
            .addTag("background")
            .build()
        
        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
            WORK_NAME_BACKGROUND,
            ExistingPeriodicWorkPolicy.KEEP,
            syncRequest
        )
        
        Log.d(TAG, "Background sync scheduled (every $SYNC_INTERVAL_BACKGROUND_MINUTES minutes)")
    }
}
```

---

## **Phase 3: Admin App Setup** 👨‍💼

### **Step 3.1: Admin RealtimeManager**

```kotlin
// Admin app - data/sync/AdminRealtimeManager.kt
@Singleton
class AdminRealtimeManager @Inject constructor(
    private val supabaseClient: SupabaseClient,
    private val orderRepository: OrderRepository
) {
    private companion object {
        const val TAG = "AdminRealtimeManager"
    }
    
    private var newOrdersChannel: RealtimeChannel? = null
    private var allOrdersChannel: RealtimeChannel? = null
    
    /**
     * Subscribe to new orders from customers
     */
    suspend fun subscribeToNewOrders() {
        try {
            Log.d(TAG, "Subscribing to new orders")
            
            newOrdersChannel = supabaseClient.channel("orders:new")
            
            newOrdersChannel?.on<JsonObject>(event = "ORDER_PLACED") { payload ->
                Log.d(TAG, "New order received: $payload")
                handleNewOrder(payload)
            }
            
            newOrdersChannel?.subscribe()
            Log.d(TAG, "Successfully subscribed to new orders")
            
        } catch (e: Exception) {
            Log.e(TAG, "Failed to subscribe to new orders", e)
        }
    }
    
    /**
     * Subscribe to all order status changes
     */
    suspend fun subscribeToAllOrderUpdates() {
        try {
            Log.d(TAG, "Subscribing to all order updates")
            
            allOrdersChannel = supabaseClient.channel("orders:*")
            
            allOrdersChannel?.on<JsonObject>(event = "ORDER_STATUS_CHANGED") { payload ->
                Log.d(TAG, "Order status changed: $payload")
                handleOrderUpdate(payload)
            }
            
            allOrdersChannel?.on<JsonObject>(event = "ORDER_ASSIGNED") { payload ->
                Log.d(TAG, "Order assigned: $payload")
                handleOrderUpdate(payload)
            }
            
            allOrdersChannel?.subscribe()
            Log.d(TAG, "Successfully subscribed to order updates")
            
        } catch (e: Exception) {
            Log.e(TAG, "Failed to subscribe to order updates", e)
        }
    }
    
    private suspend fun handleNewOrder(payload: JsonObject) {
        try {
            // Refresh orders list
            orderRepository.refreshOrders()
            
            // Show notification to admin
            // notificationManager.showNewOrderNotification(...)
            
        } catch (e: Exception) {
            Log.e(TAG, "Failed to handle new order", e)
        }
    }
    
    private suspend fun handleOrderUpdate(payload: JsonObject) {
        try {
            orderRepository.refreshOrders()
        } catch (e: Exception) {
            Log.e(TAG, "Failed to handle order update", e)
        }
    }
    
    suspend fun unsubscribeAll() {
        newOrdersChannel?.unsubscribe()
        allOrdersChannel?.unsubscribe()
        newOrdersChannel = null
        allOrdersChannel = null
    }
}
```

---

## **Phase 4: Delivery App Setup** 🚚

### **Step 4.1: Delivery RealtimeManager**

```kotlin
// Delivery app - data/sync/DeliveryRealtimeManager.kt
@Singleton
class DeliveryRealtimeManager @Inject constructor(
    private val supabaseClient: SupabaseClient,
    private val deliveryRepository: DeliveryRepository
) {
    private companion object {
        const val TAG = "DeliveryRealtimeManager"
    }
    
    private var assignedOrdersChannel: RealtimeChannel? = null
    private var confirmedOrdersChannel: RealtimeChannel? = null
    
    /**
     * Subscribe to orders assigned to this delivery person
     */
    suspend fun subscribeToAssignedOrders(deliveryPersonId: String) {
        try {
            Log.d(TAG, "Subscribing to assigned orders for: $deliveryPersonId")
            
            assignedOrdersChannel = supabaseClient.channel("delivery:$deliveryPersonId")
            
            assignedOrdersChannel?.on<JsonObject>(event = "ORDER_ASSIGNED") { payload ->
                Log.d(TAG, "New order assigned: $payload")
                handleOrderAssigned(payload)
            }
            
            assignedOrdersChannel?.on<JsonObject>(event = "ORDER_DELIVERED") { payload ->
                Log.d(TAG, "Order marked as delivered: $payload")
                handleOrderDelivered(payload)
            }
            
            assignedOrdersChannel?.subscribe()
            Log.d(TAG, "Successfully subscribed to assigned orders")
            
        } catch (e: Exception) {
            Log.e(TAG, "Failed to subscribe to assigned orders", e)
        }
    }
    
    /**
     * Subscribe to pool of confirmed orders (for self-assignment)
     */
    suspend fun subscribeToConfirmedOrders() {
        try {
            Log.d(TAG, "Subscribing to confirmed orders pool")
            
            confirmedOrdersChannel = supabaseClient.channel("orders:confirmed:*")
            
            confirmedOrdersChannel?.on<JsonObject>(event = "ORDER_CONFIRMED") { payload ->
                Log.d(TAG, "Order available for pickup: $payload")
                handleOrderConfirmed(payload)
            }
            
            confirmedOrdersChannel?.subscribe()
            Log.d(TAG, "Successfully subscribed to confirmed orders")
            
        } catch (e: Exception) {
            Log.e(TAG, "Failed to subscribe to confirmed orders", e)
        }
    }
    
    private suspend fun handleOrderAssigned(payload: JsonObject) {
        try {
            deliveryRepository.refreshAssignedOrders()
            
            // Show notification
            // notificationManager.showNewOrderAssignedNotification(...)
            
        } catch (e: Exception) {
            Log.e(TAG, "Failed to handle order assigned", e)
        }
    }
    
    private suspend fun handleOrderConfirmed(payload: JsonObject) {
        try {
            deliveryRepository.refreshAvailableOrders()
        } catch (e: Exception) {
            Log.e(TAG, "Failed to handle order confirmed", e)
        }
    }
    
    private suspend fun handleOrderDelivered(payload: JsonObject) {
        try {
            deliveryRepository.refreshAssignedOrders()
        } catch (e: Exception) {
            Log.e(TAG, "Failed to handle order delivered", e)
        }
    }
    
    suspend fun unsubscribeAll() {
        assignedOrdersChannel?.unsubscribe()
        confirmedOrdersChannel?.unsubscribe()
        assignedOrdersChannel = null
        confirmedOrdersChannel = null
    }
}
```

---

## **Phase 5: Integration & Testing** 🧪

### **Step 5.1: End-to-End Testing Scenarios**

#### **Test 1: Multi-Device Cart Sync**
1. Login on Phone A (Customer)
2. Login on Phone B (Customer, same account)
3. Add item to cart on Phone A
4. **Expected**: Phone B shows new item within 1-2 seconds

#### **Test 2: Order Flow (Customer → Admin → Delivery → Customer)**
1. Customer places order on Phone A
2. **Expected**: 
   - Admin app shows new order immediately
   - Phone A shows order in "Orders" tab
3. Admin confirms order
4. **Expected**:
   - Delivery app shows order in available pool
   - Customer app shows "Order Confirmed" status
5. Admin assigns order to delivery person
6. **Expected**:
   - Delivery app shows order in "My Deliveries"
   - Customer app shows "Out for Delivery" with delivery person info
7. Delivery person marks order as delivered
8. **Expected**:
   - Customer app shows "Delivered" status
   - Admin app updates order status

#### **Test 3: Offline Resilience**
1. Customer places order while online
2. Disconnect Admin app from network
3. Reconnect Admin app
4. **Expected**: Admin app catches up via fallback sync

#### **Test 4: Multiple Admins**
1. Two admin users logged in
2. Admin 1 confirms order
3. **Expected**: Admin 2 sees order status change immediately

### **Step 5.2: Performance Testing**
- [ ] Load test with 100 concurrent orders
- [ ] Measure broadcast latency (should be < 500ms)
- [ ] Monitor WebSocket connection stability
- [ ] Test battery drain (should be < 5% increase)

### **Step 5.3: Security Testing**
- [ ] Verify RLS policies prevent unauthorized access
- [ ] Test that Customer A cannot see Customer B's cart
- [ ] Test that Delivery person only sees assigned orders
- [ ] Test that unauthorized users cannot broadcast messages

---

## **Phase 6: Monitoring & Analytics** 📊

### **Step 6.1: Add Realtime Metrics**

```kotlin
// Track realtime events
analyticsLogger.logEvent("realtime_cart_update", mapOf(
    "latency_ms" to latency,
    "update_type" to "INSERT"
))

analyticsLogger.logEvent("realtime_order_update", mapOf(
    "order_id" to orderId,
    "event_type" to "ORDER_CONFIRMED",
    "latency_ms" to latency
))
```

### **Step 6.2: Dashboard Metrics**
- **Connection uptime**: % of time WebSocket is connected
- **Average broadcast latency**: Time from trigger to client receipt
- **Fallback sync triggers**: How often WorkManager fallback is used
- **Failed broadcasts**: Messages that didn't reach clients

### **Step 6.3: Error Monitoring**

```kotlin
// Log realtime errors to Crashlytics/Sentry
if (connectionState is RealtimeConnectionState.Error) {
    Sentry.captureException(
        RealtimeConnectionException(connectionState.message)
    )
}
```

---

## **Phase 7: Deployment Strategy** 🚀

### **Step 7.1: Staged Rollout**

| Stage | Percentage | Duration | Rollback Threshold |
|-------|------------|----------|-------------------|
| **Internal Testing** | QA only | 1 week | Any critical bug |
| **Alpha** | 5% users | 1 week | >5% error rate |
| **Beta** | 25% users | 2 weeks | >2% error rate |
| **General Availability** | 100% users | - | >1% error rate |

### **Step 7.2: Feature Flags**

```kotlin
// Remote config for gradual rollout
object FeatureFlags {
    val isRealtimeSyncEnabled: Boolean
        get() = remoteConfig.getBoolean("realtime_sync_enabled")
    
    val realtimeRolloutPercentage: Int
        get() = remoteConfig.getLong("realtime_rollout_percentage").toInt()
}

// Usage
if (FeatureFlags.isRealtimeSyncEnabled) {
    realtimeManager.subscribeToCartChanges(userId)
} else {
    // Fall back to WorkManager polling
    BackgroundSyncWorker.scheduleForegroundSync(context)
}
```

### **Step 7.3: Rollback Plan**

```kotlin
// Immediate rollback via remote config
if (!FeatureFlags.isRealtimeSyncEnabled) {
    // Disable realtime
    lifecycleScope.launch {
        realtimeManager.unsubscribeAll()
    }
    
    // Re-enable WorkManager polling
    BackgroundSyncWorker.scheduleForegroundSync(applicationContext)
}
```

---

## **Phase 8: Documentation** 📚

### **Step 8.1: Developer Documentation**
- [ ] API documentation for realtime events
- [ ] Integration guide for future apps
- [ ] Troubleshooting guide

### **Step 8.2: Operations Documentation**
- [ ] Monitoring playbook
- [ ] Incident response procedures
- [ ] Capacity planning guidelines

---

## **Timeline Estimate** ⏱️

| Phase | Duration | Dependencies |
|-------|----------|--------------|
| **Phase 1: Backend Setup** | 1 day | Database access |
| **Phase 2: Customer App** | 2 days | Phase 1 complete |
| **Phase 3: Admin App** | 1 day | Phase 1 complete |
| **Phase 4: Delivery App** | 1 day | Phase 1 complete |
| **Phase 5: Testing** | 3 days | Phases 2-4 complete |
| **Phase 6: Monitoring** | 1 day | Phase 5 complete |
| **Phase 7: Deployment** | 2-3 weeks (staged) | Phase 6 complete |
| **Total** | ~1 week dev + 2-3 weeks rollout | |

---

## **Success Metrics** 📈

| Metric | Target | Current |
|--------|--------|---------|
| **Cart sync latency** | < 2 seconds | 15 minutes |
| **Order update latency** | < 2 seconds | Manual refresh |
| **WebSocket uptime** | > 95% | N/A |
| **Battery drain increase** | < 5% | N/A |
| **Data conflict rate** | < 0.1% | Unknown |
| **User satisfaction** | > 90% | Baseline |

---

## **Risks & Mitigations** ⚠️

| Risk | Impact | Probability | Mitigation |
|------|--------|-------------|------------|
| **WebSocket instability** | High | Medium | Fallback to WorkManager |
| **Battery drain** | Medium | Low | Unsubscribe in background |
| **Supabase quota exceeded** | High | Low | Monitor usage, upgrade plan |
| **Data conflicts** | Medium | Low | Robust conflict resolution |
| **Security breach** | High | Very Low | Strict RLS policies |

---

## **Next Steps** ✅

1. **Immediate**: Review and approve migration plan
2. **Week 1**: Implement Phase 1 (Backend)
3. **Week 2**: Implement Phases 2-4 (Client apps)
4. **Week 3**: Testing and monitoring setup
5. **Week 4+**: Staged deployment

---

## **Questions & Clarifications**

- [ ] Confirm database schema for `orders` table (columns: `user_id`, `delivery_person_id`, `status`, etc.)
- [ ] Confirm role-based authentication is set up in Supabase
- [ ] Confirm Admin and Delivery apps exist and are ready for integration
- [ ] Confirm Supabase plan supports required Realtime quota
