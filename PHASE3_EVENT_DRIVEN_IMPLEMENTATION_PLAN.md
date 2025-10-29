# Phase 3: Event-Driven Architecture Implementation Plan
## Real-Time Features Across All Three Apps

**Date**: January 29, 2025  
**Status**: 🚀 READY TO BEGIN  
**Previous Phases**: Phase 1 (Backend ✅) | Phase 2 (Android Infrastructure ✅) | Phase 3 Part 1 (Cart ✅)

---

## 📋 Executive Summary

### What's Already Done ✅
- **Phase 1**: Backend EventBroadcaster module implemented
- **Phase 2**: Android EventBus + RealtimeManager infrastructure ready
- **Phase 3 Part 1**: Cart real-time sync fully working (with known limitation: events missed during WebSocket disconnections)
- **SOP Created**: Comprehensive integration guide with all fixes documented

### What We're Doing Now 🎯
Implement real-time event-driven features for:
1. **GroceryCustomer**: Orders status tracking, Product stock updates
2. **GroceryAdmin**: Dashboard metrics, Inventory management, Order management
3. **GroceryDelivery**: Live delivery assignments, Location tracking

### Success Criteria 🏆
- Customer sees order status updates without refresh
- Customer sees product stock changes immediately
- Admin dashboard updates in real-time
- Admin sees new orders appear automatically
- Delivery driver receives assignments instantly
- All apps sync across multiple devices

---

## 🗂️ Current Project Status

### Infrastructure Status

| Component | Status | Notes |
|-----------|--------|-------|
| Supabase Realtime | ✅ Active | hfxdxxpmcemdjsvhsdcf.supabase.co |
| Backend API | ✅ Deployed | https://andoid-app-kotlin.vercel.app |
| EventBroadcaster | ✅ Implemented | `grocery-delivery-api/lib/eventBroadcaster.js` |
| EventBus (Android) | ✅ Implemented | All three apps |
| RealtimeManager | ✅ Implemented | All three apps |
| Serialization Config | ✅ Fixed | Kotlin plugin + JSON dependency |

### Database Tables (Supabase)

| Table | Rows | Purpose | RLS Enabled |
|-------|------|---------|-------------|
| `user_profiles` | 9 | User accounts | ✅ |
| `products` | 9 | Product catalog | ✅ |
| `product_categories` | 5 | Categories | ✅ |
| `inventory` | 9 | Stock levels | ✅ |
| `orders` | 32 | Customer orders | ✅ |
| `order_items` | 38 | Order line items | ✅ |
| `delivery_assignments` | 19 | Driver assignments | ✅ |
| `delivery_locations` | 28 | GPS tracking | ✅ |
| `cart` | 0 | Shopping carts | ✅ |
| `admin_activity_logs` | 51 | Admin actions | ✅ |

### API Endpoints Available

**Customer Endpoints**:
- ✅ `POST /api/auth/login`
- ✅ `POST /api/auth/register`
- ✅ `GET /api/products/list`
- ✅ `GET /api/products/[id]`
- ✅ `POST /api/cart` (broadcasts events ✅)
- ✅ `PUT /api/cart/[productId]` (broadcasts events ✅)
- ✅ `DELETE /api/cart/[productId]` (broadcasts events ✅)
- ✅ `POST /api/orders/create`
- ✅ `GET /api/orders/history`
- ✅ `GET /api/orders/[id]`

**Admin Endpoints**:
- ✅ `POST /api/admin/auth/login`
- ✅ `GET /api/admin/dashboard/metrics`
- ✅ `GET /api/admin/orders`
- ✅ `GET /api/admin/orders/[id]`
- ✅ `PUT /api/admin/orders/[id]/status`
- ✅ `POST /api/admin/orders/assign`
- ✅ `GET /api/admin/products`
- ✅ `POST /api/admin/products`
- ✅ `PUT /api/admin/products/[id]`
- ✅ `GET /api/admin/inventory`
- ✅ `PUT /api/admin/inventory`

**Delivery Endpoints**:
- ✅ `POST /api/delivery/auth/login`
- ✅ `GET /api/delivery/available-orders`
- ✅ `POST /api/delivery/accept`
- ✅ `POST /api/delivery/decline`
- ✅ `PUT /api/delivery/update-status`
- ✅ `POST /api/delivery/update-location`
- ✅ `GET /api/delivery/orders/history`

---

## 🎯 Phase 3 Breakdown

### Part 2: GroceryCustomer - Orders & Products (6-8 days)

#### Feature 1: Real-Time Order Status Updates (3-4 days)

**What Users Will Experience**:
```
Scenario: Customer places order
1. Order created → Status: "pending"
2. Admin confirms order → Status badge changes to "confirmed" (INSTANTLY on customer's phone)
3. Admin assigns driver → "Track Delivery" button appears (INSTANTLY)
4. Driver accepts → Status changes to "preparing" (INSTANTLY)
5. Driver starts delivery → Status "out_for_delivery" + map tracking appears (INSTANTLY)
6. Driver completes → Status "delivered" + thank you message (INSTANTLY)

NO MANUAL REFRESH NEEDED ✨
```

**Implementation Steps**:

**Step 1: Backend - Add Order Event Broadcasting** (1 hour)

File: `grocery-delivery-api/pages/api/orders/create.js`
```javascript
import eventBroadcaster from '../../../lib/eventBroadcaster'

// After order creation
await eventBroadcaster.orderCreated({
  order_id: orderId,
  user_id: userId,
  order_number: orderNumber,
  status: 'pending',
  total_amount: totalAmount
})
```

File: `grocery-delivery-api/pages/api/admin/orders/[id]/status.js`
```javascript
// After status update
await eventBroadcaster.orderStatusChanged({
  order_id: id,
  user_id: order.user_id,
  old_status: order.status,
  new_status: newStatus,
  updated_by: 'admin'
})
```

File: `grocery-delivery-api/pages/api/delivery/update-status.js`
```javascript
// After delivery status update
await eventBroadcaster.orderStatusChanged({
  order_id: assignment.order_id,
  user_id: order.user_id,
  old_status: order.status,
  new_status: mappedOrderStatus,
  updated_by: 'delivery_driver'
})
```

**Step 2: Android - Add Event Models** (30 minutes)

File: `GroceryCustomer/app/src/main/java/com/grocery/customer/data/local/Event.kt`
```kotlin
sealed class Event {
    // ... existing cart events ...
    
    @Serializable
    data class OrderCreated(
        val orderId: String,
        val orderNumber: String,
        val status: String,
        val totalAmount: Double,
        val timestamp: String
    ) : Event()
    
    @Serializable
    data class OrderStatusChanged(
        val orderId: String,
        val oldStatus: String?,
        val newStatus: String,
        val timestamp: String
    ) : Event()
    
    @Serializable
    data class OrderAssigned(
        val orderId: String,
        val driverId: String,
        val driverName: String,
        val timestamp: String
    ) : Event()
}
```

**Step 3: RealtimeManager - Subscribe to Order Channels** (1 hour)

File: `GroceryCustomer/app/src/main/java/com/grocery/customer/data/remote/RealtimeManager.kt`
```kotlin
/**
 * Subscribe to order updates for a specific order
 */
fun subscribeToOrder(orderId: String) {
    if (channels.containsKey("order:$orderId")) {
        Log.d(TAG, "Already subscribed to order:$orderId")
        return
    }
    
    scope.launch {
        try {
            val channel = supabaseClient.channel("order:$orderId")
            
            channel.on<OrderStatusChangedPayload>("status_changed") { payload ->
                Log.d(TAG, "Order status changed: $payload")
                eventBus.publish(Event.OrderStatusChanged(
                    orderId = payload.order_id,
                    oldStatus = payload.old_status,
                    newStatus = payload.new_status,
                    timestamp = payload.timestamp ?: System.currentTimeMillis().toString()
                ))
            }
            
            channel.subscribe()
            channels["order:$orderId"] = channel
            Log.d(TAG, "Subscribed to order:$orderId")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to subscribe to order:$orderId", e)
        }
    }
}

@Serializable
data class OrderStatusChangedPayload(
    val order_id: String,
    val old_status: String?,
    val new_status: String,
    val timestamp: String?
)
```

**Step 4: ViewModel - Handle Order Events** (2 hours)

File: `GroceryCustomer/app/src/main/java/com/grocery/customer/ui/viewmodels/OrderDetailViewModel.kt`
```kotlin
@HiltViewModel
class OrderDetailViewModel @Inject constructor(
    private val ordersRepository: OrdersRepository,
    private val realtimeManager: RealtimeManager,
    private val eventBus: EventBus
) : BaseViewModel() {
    
    private val _order = MutableStateFlow<OrderDTO?>(null)
    val order: StateFlow<OrderDTO?> = _order.asStateFlow()
    
    private val _showToast = MutableStateFlow<String?>(null)
    val showToast: StateFlow<String?> = _showToast.asStateFlow()
    
    init {
        // Subscribe to order status change events
        viewModelScope.launch {
            eventBus.events
                .filterIsInstance<Event.OrderStatusChanged>()
                .collect { event ->
                    // Only update if it's our order
                    if (event.orderId == _order.value?.id) {
                        Log.d(TAG, "Received status change for current order: ${event.newStatus}")
                        
                        // Update order status locally
                        _order.value = _order.value?.copy(status = event.newStatus)
                        
                        // Show toast notification
                        _showToast.value = "Order status updated: ${event.newStatus}"
                        
                        // If status is out_for_delivery, show tracking button
                        if (event.newStatus == "out_for_delivery") {
                            _showToast.value = "Your order is on the way!"
                        }
                    }
                }
        }
        
        // Subscribe to order assigned events
        viewModelScope.launch {
            eventBus.events
                .filterIsInstance<Event.OrderAssigned>()
                .collect { event ->
                    if (event.orderId == _order.value?.id) {
                        Log.d(TAG, "Order assigned to driver: ${event.driverName}")
                        _showToast.value = "Driver assigned: ${event.driverName}"
                    }
                }
        }
    }
    
    fun loadOrder(orderId: String) {
        executeWithLoading {
            val order = ordersRepository.getOrder(orderId)
            _order.value = order
            
            // Subscribe to real-time updates for this order
            realtimeManager.subscribeToOrder(orderId)
        }
    }
    
    override fun onCleared() {
        super.onCleared()
        _order.value?.id?.let { orderId ->
            realtimeManager.unsubscribe("order:$orderId")
        }
    }
}
```

**Step 5: UI - Add Toast Notifications** (30 minutes)

File: `GroceryCustomer/app/src/main/java/com/grocery/customer/ui/fragments/OrderDetailFragment.kt`
```kotlin
override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
    super.onViewCreated(view, savedInstanceState)
    
    // ... existing code ...
    
    // Observe toast messages
    viewLifecycleOwner.lifecycleScope.launch {
        viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
            viewModel.showToast.collect { message ->
                message?.let {
                    Toast.makeText(requireContext(), it, Toast.LENGTH_SHORT).show()
                }
            }
        }
    }
}
```

**Testing Checklist**:
- [ ] Place order from Customer app
- [ ] Update status from Admin app
- [ ] Verify Customer app shows new status without refresh
- [ ] Verify toast notification appears
- [ ] Check logcat for event flow
- [ ] Test with multiple orders
- [ ] Test with two phones (same user)

---

#### Feature 2: Real-Time Product Stock Updates (2-3 days)

**What Users Will Experience**:
```
Scenario: Customer viewing product page
1. Product shows: "In stock: 100 items"
2. Admin updates stock to 5
3. Product page updates: "In stock: 5 items" + "Low Stock!" badge (INSTANTLY)
4. Admin sets stock to 0
5. Product shows: "Out of Stock" + "Add to Cart" button disabled (INSTANTLY)

In product list:
1. Product card shows "Add to Cart" button
2. Admin sets stock to 0
3. Product card shows "Out of Stock" badge, button becomes "Notify Me" (INSTANTLY)
```

**Implementation Steps**:

**Step 1: Backend - Add Inventory Event Broadcasting** (1 hour)

File: `grocery-delivery-api/pages/api/admin/inventory/index.js`
```javascript
import eventBroadcaster from '../../../lib/eventBroadcaster'

// After inventory update
await eventBroadcaster.productStockChanged({
  product_id: productId,
  old_stock: oldStock,
  new_stock: newStock,
  is_low_stock: newStock < 10,
  is_out_of_stock: newStock === 0
})
```

**Step 2: Android - Add Event Models** (15 minutes)

File: `GroceryCustomer/app/src/main/java/com/grocery/customer/data/local/Event.kt`
```kotlin
@Serializable
data class ProductStockChanged(
    val productId: String,
    val oldStock: Int,
    val newStock: Int,
    val isLowStock: Boolean,
    val isOutOfStock: Boolean,
    val timestamp: String
) : Event()
```

**Step 3: RealtimeManager - Subscribe to Product Stock** (1 hour)

File: `GroceryCustomer/app/src/main/java/com/grocery/customer/data/remote/RealtimeManager.kt`
```kotlin
/**
 * Subscribe to product stock changes (global channel)
 */
fun subscribeToProductStock() {
    if (channels.containsKey("products")) {
        Log.d(TAG, "Already subscribed to products")
        return
    }
    
    scope.launch {
        try {
            val channel = supabaseClient.channel("products")
            
            channel.on<ProductStockPayload>("stock_updated") { payload ->
                Log.d(TAG, "Stock updated: ${payload.product_id} -> ${payload.new_stock}")
                eventBus.publish(Event.ProductStockChanged(
                    productId = payload.product_id,
                    oldStock = payload.old_stock,
                    newStock = payload.new_stock,
                    isLowStock = payload.is_low_stock,
                    isOutOfStock = payload.is_out_of_stock,
                    timestamp = payload.timestamp ?: System.currentTimeMillis().toString()
                ))
            }
            
            channel.subscribe()
            channels["products"] = channel
            Log.d(TAG, "Subscribed to products channel")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to subscribe to products", e)
        }
    }
}

@Serializable
data class ProductStockPayload(
    val product_id: String,
    val old_stock: Int,
    val new_stock: Int,
    val is_low_stock: Boolean,
    val is_out_of_stock: Boolean,
    val timestamp: String?
)
```

**Step 4: ViewModel - Handle Stock Events** (2 hours)

File: `GroceryCustomer/app/src/main/java/com/grocery/customer/ui/viewmodels/ProductDetailViewModel.kt`
```kotlin
init {
    // Subscribe to product stock change events
    viewModelScope.launch {
        eventBus.events
            .filterIsInstance<Event.ProductStockChanged>()
            .collect { event ->
                // Only update if it's our product
                if (event.productId == _productDetail.value?.product?.id) {
                    Log.d(TAG, "Stock changed for current product: ${event.newStock}")
                    
                    // Update product stock
                    _productDetail.value?.let { currentDetail ->
                        val updatedInventory = currentDetail.inventory.copy(stock = event.newStock)
                        val updatedProduct = currentDetail.product.copy(inventory = updatedInventory)
                        _productDetail.value = currentDetail.copy(product = updatedProduct)
                        
                        // Adjust quantity if necessary
                        if (_quantity.value > event.newStock) {
                            _quantity.value = event.newStock.coerceAtLeast(0)
                        }
                        
                        // Show notification
                        when {
                            event.isOutOfStock -> _showToast.value = "Product is now out of stock"
                            event.isLowStock -> _showToast.value = "Only ${event.newStock} items left!"
                            event.newStock > event.oldStock -> _showToast.value = "Stock replenished!"
                        }
                    }
                }
            }
    }
}
```

**Step 5: UI - Update Stock Display** (1 hour)

File: `GroceryCustomer/app/src/main/java/com/grocery/customer/ui/fragments/ProductDetailFragment.kt`
```kotlin
private fun updateStockInfo(stock: Int) {
    binding.apply {
        when {
            stock == 0 -> {
                textStockStatus.text = "Out of Stock"
                textStockStatus.setTextColor(ContextCompat.getColor(requireContext(), R.color.error))
                buttonAddToCart.isEnabled = false
                buttonAddToCart.text = "Notify Me"
                layoutQuantitySelector.visibility = View.GONE
            }
            stock < 10 -> {
                textStockStatus.text = "Low Stock: Only $stock left!"
                textStockStatus.setTextColor(ContextCompat.getColor(requireContext(), R.color.warning))
                buttonAddToCart.isEnabled = true
                layoutQuantitySelector.visibility = View.VISIBLE
            }
            else -> {
                textStockStatus.text = "In stock: $stock items"
                textStockStatus.setTextColor(ContextCompat.getColor(requireContext(), R.color.success))
                buttonAddToCart.isEnabled = true
                layoutQuantitySelector.visibility = View.VISIBLE
            }
        }
    }
}
```

**Testing Checklist**:
- [ ] Open product detail page
- [ ] Admin reduces stock to 5
- [ ] Verify "Low Stock" warning appears
- [ ] Admin sets stock to 0
- [ ] Verify "Out of Stock" + disabled button
- [ ] Admin increases stock to 100
- [ ] Verify stock info updates
- [ ] Test on product list view
- [ ] Test with two phones viewing same product

---

### Part 3: GroceryAdmin - Dashboard, Inventory, Orders (6-8 days)

#### Feature 3: Real-Time Dashboard Metrics (2-3 days)

**What Admins Will Experience**:
```
Dashboard shows:
- Total orders: 23
- Total revenue: ₹12,345

Customer places new order:
- Total orders updates to: 24 (INSTANTLY)
- Total revenue updates to: ₹12,397 (INSTANTLY)
- "New order received" notification appears
- Order appears in recent orders list (INSTANTLY)

Driver completes delivery:
- Pending orders count decreases (INSTANTLY)
- Delivered orders count increases (INSTANTLY)
```

**Implementation Steps**:

**Step 1: Backend - Broadcast Dashboard Events** (2 hours)

File: `grocery-delivery-api/lib/eventBroadcaster.js`
```javascript
async dashboardMetricsUpdated(metrics) {
  await this.broadcastToChannel('admin:dashboard', 'metrics_updated', {
    total_orders: metrics.total_orders,
    total_revenue: metrics.total_revenue,
    pending_orders: metrics.pending_orders,
    active_deliveries: metrics.active_deliveries
  })
}

async newOrderReceived(order) {
  await this.broadcastToChannel('admin:orders', 'new_order', {
    order_id: order.id,
    order_number: order.order_number,
    customer_name: order.customer_name,
    total_amount: order.total_amount,
    timestamp: new Date().toISOString()
  })
}
```

Update `grocery-delivery-api/pages/api/orders/create.js`:
```javascript
// After order creation
await eventBroadcaster.newOrderReceived({
  id: orderId,
  order_number: orderNumber,
  customer_name: user.full_name,
  total_amount: totalAmount
})
```

**Step 2: Android - Event Models** (30 minutes)

File: `GroceryAdmin/app/src/main/java/com/grocery/admin/data/local/Event.kt`
```kotlin
sealed class Event {
    @Serializable
    data class DashboardMetricsUpdated(
        val totalOrders: Int,
        val totalRevenue: Double,
        val pendingOrders: Int,
        val activeDeliveries: Int,
        val timestamp: String
    ) : Event()
    
    @Serializable
    data class NewOrderReceived(
        val orderId: String,
        val orderNumber: String,
        val customerName: String,
        val totalAmount: Double,
        val timestamp: String
    ) : Event()
}
```

**Step 3: RealtimeManager - Subscribe to Admin Channels** (1 hour)

File: `GroceryAdmin/app/src/main/java/com/grocery/admin/data/remote/RealtimeManager.kt`
```kotlin
fun subscribeToAdminDashboard() {
    if (channels.containsKey("admin:dashboard")) return
    
    scope.launch {
        val channel = supabaseClient.channel("admin:dashboard")
        
        channel.on<DashboardMetricsPayload>("metrics_updated") { payload ->
            eventBus.publish(Event.DashboardMetricsUpdated(
                totalOrders = payload.total_orders,
                totalRevenue = payload.total_revenue,
                pendingOrders = payload.pending_orders,
                activeDeliveries = payload.active_deliveries,
                timestamp = payload.timestamp ?: System.currentTimeMillis().toString()
            ))
        }
        
        channel.subscribe()
        channels["admin:dashboard"] = channel
    }
}

fun subscribeToAdminOrders() {
    if (channels.containsKey("admin:orders")) return
    
    scope.launch {
        val channel = supabaseClient.channel("admin:orders")
        
        channel.on<NewOrderPayload>("new_order") { payload ->
            eventBus.publish(Event.NewOrderReceived(
                orderId = payload.order_id,
                orderNumber = payload.order_number,
                customerName = payload.customer_name,
                totalAmount = payload.total_amount,
                timestamp = payload.timestamp
            ))
        }
        
        channel.subscribe()
        channels["admin:orders"] = channel
    }
}
```

**Step 4: ViewModel - Update Dashboard Metrics** (2 hours)

File: `GroceryAdmin/app/src/main/java/com/grocery/admin/ui/viewmodels/DashboardViewModel.kt`
```kotlin
@HiltViewModel
class DashboardViewModel @Inject constructor(
    private val dashboardRepository: DashboardRepository,
    private val realtimeManager: RealtimeManager,
    private val eventBus: EventBus
) : BaseViewModel() {
    
    private val _metrics = MutableStateFlow<DashboardMetrics?>(null)
    val metrics: StateFlow<DashboardMetrics?> = _metrics.asStateFlow()
    
    private val _newOrderNotification = MutableStateFlow<String?>(null)
    val newOrderNotification: StateFlow<String?> = _newOrderNotification.asStateFlow()
    
    init {
        // Subscribe to dashboard metrics updates
        viewModelScope.launch {
            eventBus.events
                .filterIsInstance<Event.DashboardMetricsUpdated>()
                .collect { event ->
                    Log.d(TAG, "Dashboard metrics updated")
                    _metrics.value = _metrics.value?.copy(
                        totalOrders = event.totalOrders,
                        totalRevenue = event.totalRevenue,
                        pendingOrders = event.pendingOrders,
                        activeDeliveries = event.activeDeliveries
                    )
                }
        }
        
        // Subscribe to new order notifications
        viewModelScope.launch {
            eventBus.events
                .filterIsInstance<Event.NewOrderReceived>()
                .collect { event ->
                    Log.d(TAG, "New order received: ${event.orderNumber}")
                    _newOrderNotification.value = "New order: ${event.orderNumber} - ₹${event.totalAmount}"
                    
                    // Increment pending orders count
                    _metrics.value?.let { current ->
                        _metrics.value = current.copy(
                            totalOrders = current.totalOrders + 1,
                            pendingOrders = current.pendingOrders + 1
                        )
                    }
                }
        }
        
        // Initialize subscriptions
        realtimeManager.subscribeToAdminDashboard()
        realtimeManager.subscribeToAdminOrders()
    }
}
```

**Testing Checklist**:
- [ ] Open admin dashboard
- [ ] Place order from customer app
- [ ] Verify metrics update automatically
- [ ] Verify "New order" notification appears
- [ ] Complete delivery from driver app
- [ ] Verify pending orders decrease
- [ ] Test with multiple admins logged in

---

#### Feature 4: Real-Time Inventory Management (2-3 days)

**What Admins Will Experience**:
```
Admin A viewing inventory page:
- Apple: 100 in stock

Admin B updates Apple stock to 50:
- Admin A sees: "Apple stock updated to 50" (INSTANTLY)
- Low stock badge appears if < 10 (INSTANTLY)
- Out of stock badge appears if = 0 (INSTANTLY)
```

**Implementation Steps**: Similar to Product Stock Updates, but for admin UI

---

#### Feature 5: Real-Time Order Management (2 days)

**What Admins Will Experience**:
```
Admin viewing orders list:
- Order #1026: Status "pending"

Driver accepts order:
- Order #1026: Status changes to "preparing" (INSTANTLY)
- Order moves to "In Progress" tab automatically

Driver completes:
- Order #1026: Status "delivered" (INSTANTLY)
- Order moves to "Completed" tab automatically
```

**Implementation Steps**: Reuse order status events, apply to admin UI

---

### Part 4: GroceryDelivery - Live Tracking, Assignments (6-8 days)

#### Feature 6: Real-Time Delivery Assignments (2-3 days)

**What Drivers Will Experience**:
```
Driver app open, "Available Orders" screen:
- No orders shown

Admin assigns order to driver:
- Order #1027 appears instantly in list
- Notification sound plays
- Badge shows "1 new assignment"
```

**Implementation Steps**:

**Step 1: Backend - Broadcast Assignment Events** (1 hour)

File: `grocery-delivery-api/pages/api/admin/orders/assign.js`
```javascript
import eventBroadcaster from '../../../lib/eventBroadcaster'

// After assignment
await eventBroadcaster.orderAssignedToDriver({
  assignment_id: assignmentId,
  order_id: orderId,
  driver_id: deliveryPersonnelId,
  order_number: order.order_number,
  customer_name: customerInfo.full_name,
  delivery_address: deliveryAddress,
  total_amount: order.total_amount
})
```

**Step 2: RealtimeManager - Subscribe to Driver Channel** (1 hour)

File: `GroceryDelivery/app/src/main/java/com/grocery/delivery/data/remote/RealtimeManager.kt`
```kotlin
fun subscribeToDriverAssignments(driverId: String) {
    if (channels.containsKey("driver:$driverId")) return
    
    scope.launch {
        val channel = supabaseClient.channel("driver:$driverId")
        
        channel.on<OrderAssignmentPayload>("order_assigned") { payload ->
            Log.d(TAG, "New order assigned: ${payload.order_number}")
            eventBus.publish(Event.OrderAssignedToDriver(
                assignmentId = payload.assignment_id,
                orderId = payload.order_id,
                orderNumber = payload.order_number,
                customerName = payload.customer_name,
                totalAmount = payload.total_amount,
                timestamp = payload.timestamp
            ))
        }
        
        channel.subscribe()
        channels["driver:$driverId"] = channel
    }
}
```

**Step 3: ViewModel - Handle Assignment Events** (2 hours)

File: `GroceryDelivery/app/src/main/java/com/grocery/delivery/ui/viewmodels/AvailableOrdersViewModel.kt`
```kotlin
init {
    viewModelScope.launch {
        eventBus.events
            .filterIsInstance<Event.OrderAssignedToDriver>()
            .collect { event ->
                Log.d(TAG, "New assignment received: ${event.orderNumber}")
                
                // Add to available orders list
                _assignments.value = _assignments.value.toMutableList().apply {
                    add(0, event.toAssignment())
                }
                
                // Show notification
                _newAssignmentNotification.value = "New order: ${event.orderNumber}"
                
                // Play notification sound
                playNotificationSound()
            }
    }
}
```

**Testing Checklist**:
- [ ] Open delivery app (Available Orders)
- [ ] Admin assigns order to driver
- [ ] Verify order appears instantly
- [ ] Verify notification sound plays
- [ ] Test with multiple drivers
- [ ] Test assignment cancellation

---

#### Feature 7: Live Location Tracking (2-3 days)

**What Customers Will Experience**:
```
Customer viewing order detail:
- Order status: "out_for_delivery"
- Map shows driver's marker

Driver's location updates every 15 seconds:
- Marker moves smoothly on map (REAL-TIME)
- ETA updates dynamically
- Route line adjusts
```

**Implementation Steps**:

**Step 1: Backend - Broadcast Location Updates** (30 minutes)

File: `grocery-delivery-api/pages/api/delivery/update-location.js`
```javascript
// After saving location
await eventBroadcaster.driverLocationUpdated({
  driver_id: userId,
  order_id: orderId,
  latitude: latitude,
  longitude: longitude,
  speed: speed,
  heading: heading,
  timestamp: new Date().toISOString()
})
```

**Step 2: Customer App - Subscribe to Driver Location** (2 hours)

File: `GroceryCustomer/app/src/main/java/com/grocery/customer/data/remote/RealtimeManager.kt`
```kotlin
fun subscribeToDriverLocation(driverId: String) {
    if (channels.containsKey("driver_location:$driverId")) return
    
    scope.launch {
        val channel = supabaseClient.channel("driver_location:$driverId")
        
        channel.on<DriverLocationPayload>("location_updated") { payload ->
            eventBus.publish(Event.DriverLocationUpdated(
                driverId = payload.driver_id,
                latitude = payload.latitude,
                longitude = payload.longitude,
                speed = payload.speed,
                heading = payload.heading,
                timestamp = payload.timestamp
            ))
        }
        
        channel.subscribe()
        channels["driver_location:$driverId"] = channel
    }
}
```

**Step 3: Google Maps Integration** (2 hours)

File: `GroceryCustomer/app/src/main/java/com/grocery/customer/ui/fragments/TrackDeliveryFragment.kt`
```kotlin
private lateinit var driverMarker: Marker
private lateinit var googleMap: GoogleMap

init {
    viewModelScope.launch {
        eventBus.events
            .filterIsInstance<Event.DriverLocationUpdated>()
            .collect { event ->
                // Update marker position smoothly
                updateDriverMarkerPosition(
                    LatLng(event.latitude, event.longitude),
                    event.heading
                )
            }
    }
}

private fun updateDriverMarkerPosition(position: LatLng, heading: Double) {
    // Smooth animation
    val propertyLatitude = Property.of(Marker::class.java, Double::class.java, "latitude")
    val propertyLongitude = Property.of(Marker::class.java, Double::class.java, "longitude")
    
    ObjectAnimator.ofPropertyValuesHolder(
        driverMarker,
        PropertyValuesHolder.ofFloat(propertyLatitude, driverMarker.position.latitude, position.latitude),
        PropertyValuesHolder.ofFloat(propertyLongitude, driverMarker.position.longitude, position.longitude)
    ).apply {
        duration = 1000
        interpolator = LinearInterpolator()
        start()
    }
    
    // Update rotation based on heading
    driverMarker.rotation = heading.toFloat()
}
```

**Testing Checklist**:
- [ ] Customer places order
- [ ] Admin assigns to driver
- [ ] Driver accepts and starts delivery
- [ ] Customer opens tracking screen
- [ ] Verify map shows driver location
- [ ] Walk with driver's phone
- [ ] Verify marker moves in real-time on customer's phone
- [ ] Test with poor network connection

---

## 📋 Implementation Schedule

### Week 1: GroceryCustomer (Orders & Products)
| Day | Feature | Tasks | Hours |
|-----|---------|-------|-------|
| Mon | Order Events Setup | Backend broadcasting, Event models, RealtimeManager | 4h |
| Tue | Order Status Updates | ViewModel integration, UI updates, Testing | 6h |
| Wed | Product Stock Events | Backend broadcasting, Event models | 4h |
| Thu | Product Stock Updates | ViewModel, UI, Product list updates | 5h |
| Fri | Testing & Polish | Multi-device testing, Bug fixes, Documentation | 5h |

### Week 2: GroceryAdmin (Dashboard, Inventory, Orders)
| Day | Feature | Tasks | Hours |
|-----|---------|-------|-------|
| Mon | Dashboard Events | Backend, Event models, RealtimeManager | 4h |
| Tue | Dashboard Metrics | ViewModel, UI cards, Notifications | 6h |
| Wed | Inventory Events | Backend, ViewModel, UI updates | 5h |
| Thu | Order Management | Real-time order list, Status changes | 5h |
| Fri | Testing & Polish | Multi-admin testing, Bug fixes | 4h |

### Week 3: GroceryDelivery (Assignments, Tracking)
| Day | Feature | Tasks | Hours |
|-----|---------|-------|-------|
| Mon | Assignment Events | Backend, Event models, RealtimeManager | 4h |
| Tue | Assignment Updates | ViewModel, UI, Notifications | 5h |
| Wed | Location Tracking Backend | Location broadcasting, Event models | 3h |
| Thu | Location Tracking UI | Google Maps integration, Marker animation | 6h |
| Fri | Testing & Polish | End-to-end testing, Bug fixes, Documentation | 6h |

**Total Estimated Time**: 18-24 days (3 weeks)

---

## 🧪 Testing Strategy

### Multi-Device Testing Scenarios

**Scenario 1: Order Status Flow (End-to-End)**
1. Phone A (Customer): Place order → See status "pending"
2. Phone B (Admin): See new order appear in dashboard
3. Phone B (Admin): Assign order to driver
4. Phone C (Driver): See order appear in available orders
5. Phone A (Customer): See status change to "confirmed"
6. Phone C (Driver): Accept order
7. Phone A (Customer): See status "preparing"
8. Phone C (Driver): Start delivery
9. Phone A (Customer): See status "out_for_delivery" + map
10. Phone C (Driver): Mark delivered
11. Phone A (Customer): See status "delivered"
12. Phone B (Admin): See metrics update

**Scenario 2: Stock Updates (Multi-User)**
1. Phone A (Customer): Open product "Apple" (100 in stock)
2. Phone B (Admin): Reduce stock to 5
3. Phone A (Customer): See "Low Stock!" warning appear
4. Phone C (Customer): Browse products, see "Low Stock" badge on Apple
5. Phone B (Admin): Set stock to 0
6. Phone A (Customer): See "Out of Stock" + button disabled
7. Phone C (Customer): See "Out of Stock" badge in list

**Scenario 3: Location Tracking**
1. Phone A (Customer): Order with status "out_for_delivery"
2. Open tracking screen, see driver marker
3. Phone B (Driver): Walk around with location updates
4. Phone A (Customer): Watch marker move in real-time
5. Phone C (Admin): View order detail, see driver location
6. All locations should sync within 1-2 seconds

### Performance Testing
- Test with 10+ simultaneous events
- Monitor memory usage during long sessions
- Check battery consumption
- Test network reconnection handling
- Test offline → online transitions

---

## 🔧 Troubleshooting Guide

### Common Issues & Solutions

**Issue 1: Events Not Received**
```
Symptoms: UI doesn't update automatically
Check:
1. Supabase connection: RealtimeManager logs "Subscribed to..."
2. Channel names match: "order:{orderId}" format
3. Serialization: @Serializable on all payload classes
4. EventBus: Check publish() logs
5. ViewModel: Check filterIsInstance<Event.XYZ>() logs

Solution: Add detailed logging at each step
```

**Issue 2: WebSocket Disconnections**
```
Symptoms: Events work, then stop after 15 seconds
Known Limitation: Vercel serverless + Supabase heartbeat timeouts
Workaround: Client-side retry mechanism (already in SOP)
Solution: Events resume after reconnection
```

**Issue 3: Serialization Errors**
```
Symptoms: "Serializer for class 'X' is not found"
Solution:
1. Add @Serializable to data class
2. Verify kotlin("plugin.serialization") in build.gradle
3. Check all fields have explicit types (no Any?)
```

**Issue 4: Multiple Subscriptions**
```
Symptoms: Duplicate events, phx_close messages
Solution:
1. Always check channels.containsKey() before subscribing
2. Unsubscribe in ViewModel onCleared()
3. Use unique channel names per resource
```

---

## 📝 Success Metrics

### Completion Checklist

#### GroceryCustomer
- [ ] Order status updates without refresh
- [ ] Product stock changes reflected instantly
- [ ] Toast notifications for all events
- [ ] Multi-device sync working
- [ ] No crashes or memory leaks
- [ ] All events logged in logcat

#### GroceryAdmin
- [ ] Dashboard metrics update in real-time
- [ ] New orders appear automatically
- [ ] Inventory changes sync across admins
- [ ] Order status changes reflected instantly
- [ ] No performance issues with many events

#### GroceryDelivery
- [ ] New assignments appear instantly
- [ ] Location tracking works smoothly
- [ ] Customer sees driver movement in real-time
- [ ] Notifications sound for new assignments
- [ ] Background location tracking works

### Performance Targets
- Event latency: < 2 seconds
- Memory usage: < 100MB increase
- Battery drain: < 5% per hour
- Network usage: < 1MB per hour (location tracking)

---

## 🚀 Getting Started

### Immediate Next Steps

1. **Read EVENT_DRIVEN_ARCHITECTURE_SOP.md** (15 minutes)
   - Review all 6 critical mistakes and solutions
   - Understand serialization requirements
   - Study channel subscription best practices

2. **Verify Backend Status** (5 minutes)
   ```bash
   # Check if EventBroadcaster exists
   E:\warp projects\kotlin mobile application\grocery-delivery-api\lib\eventBroadcaster.js
   ```

3. **Start with GroceryCustomer Orders** (Week 1, Day 1)
   - Follow Feature 1 implementation steps
   - Add backend broadcasting first
   - Test with manual events before backend integration

4. **Use Working Cart Feature as Reference**
   - Cart real-time sync is fully working
   - Copy patterns from CartViewModel
   - Reuse RealtimeManager subscription logic

---

## 📚 Key Documents Reference

- **EVENT_DRIVEN_ARCHITECTURE_SOP.md**: Complete integration guide with all fixes
- **API_INTEGRATION_GUIDE.md**: All available endpoints and request/response formats
- **DESIGN_SYSTEM_GUIDE.md**: UI components and styling standards
- **PHASE1_BACKEND_EVENT_BROADCASTING_COMPLETE.md**: Backend architecture
- **PHASE2_ANDROID_EVENT_INFRASTRUCTURE_COMPLETE.md**: Android EventBus setup
- **PHASE3_VIEWMODEL_INTEGRATION_COMPLETE.md**: Cart implementation (reference)

---

## 🎯 Final Notes

### What Makes This Different from Cart Implementation
- **Cart**: User-specific channel (`cart:{userId}`)
- **Orders**: Per-order channel (`order:{orderId}`) + user channel
- **Products**: Global channel (`products`) - all users receive
- **Admin**: Role-specific channels (`admin:dashboard`, `admin:orders`)
- **Delivery**: Driver-specific channel (`driver:{driverId}`)

### Key Success Factors
1. ✅ Follow SOP patterns religiously
2. ✅ Always use @Serializable data classes
3. ✅ Check channels.containsKey() before subscribing
4. ✅ Add comprehensive logging
5. ✅ Test with multiple devices
6. ✅ Handle WebSocket reconnections gracefully

### Known Limitations
- Events can be missed during WebSocket disconnections (documented in SOP)
- Vercel serverless prevents persistent event queues
- Client-side retry needed for production
- Supabase Realtime has connection limits (check plan)

---

**Document Version**: 1.0  
**Status**: Ready for Implementation  
**Next Action**: Start with GroceryCustomer Orders (Feature 1)  
**Estimated Completion**: 3 weeks (18-24 days)

---

**Good luck! Remember: The cart feature is your reference implementation. If in doubt, check how it works there. 🚀**
