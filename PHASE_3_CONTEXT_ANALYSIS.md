# Phase 3: Context Analysis - Admin & Delivery Apps

## Overview

Gathered comprehensive context for implementing realtime sync in both **GroceryAdmin** and **GroceryDelivery** apps.

---

## GroceryAdmin App

### Current Configuration
- **Package**: `com.grocery.admin`
- **Application ID**: `com.grocery.admin` (debug: `.debug`)
- **Supabase URL**: `https://sjujrmvfzzzfskknvgjx.supabase.co` ⚠️ **INCORRECT**
- **Supabase Key**: Old/incorrect key
- **API Base URL**: `https://andoid-app-kotlin.vercel.app/api/`

### Architecture
- **DI Framework**: Hilt/Dagger
- **Networking**: Retrofit + OkHttp
- **Database**: Room
- **Navigation**: Navigation Component with Bottom Navigation
- **View Binding**: Enabled

### Key Repositories
1. **OrdersRepository** - Manages admin order operations:
   - `getOrders(page, limit, status)` - Get all orders (admin sees all)
   - `getOrderById(orderId)` - Get specific order
   - `updateOrderStatus(orderId, status, notes)` - Update order status
   - `assignOrder(orderId, deliveryPersonnelId, estimatedMinutes)` - Assign delivery driver
   - `getDeliveryPersonnel(activeOnly)` - Get available drivers

2. **ProductsRepository** - Manages product catalog:
   - `getProducts(page, limit)` - Get all products
   - `createProduct(request)` - Create new product
   - `updateProduct(productId, request)` - Update product
   - `deleteProduct(productId)` - Delete product
   - `getProductCategories()` - Get categories
   - `updateInventoryStock(productId, stock)` - Update stock

3. **InventoryRepository** - Stock management
4. **DashboardRepository** - Dashboard statistics
5. **AuthRepository** - Admin authentication

### MainActivity Structure
- Extends `BaseActivity<ActivityMainBinding>`
- Uses Navigation Component with NavHostFragment
- Bottom Navigation UI
- Toolbar with logout action
- Token-based authentication via `TokenStore`

### Realtime Sync Requirements
Admin app needs real-time updates for:
1. **All Orders** (not just own orders - admin sees everything)
   - New order placed by customers
   - Order status changes
   - Order assignments
   - Delivery updates

2. **Delivery Assignments**
   - When drivers accept/decline orders
   - Driver location updates
   - Assignment status changes

3. **Product Inventory** (optional but useful)
   - Stock level changes
   - Product updates

### Files to Create/Modify
**Create:**
1. `SupabaseModule.kt` - DI module for Supabase client
2. `RealtimeManager.kt` - Manage realtime subscriptions
3. `RealtimeConnectionState.kt` - Connection state sealed class
4. `network_security_config.xml` - Network security config

**Modify:**
1. `build.gradle.kts` - Add Supabase dependencies + fix Supabase URL/key
2. `MainActivity.kt` - Integrate realtime lifecycle
3. `OrdersRepositoryImpl.kt` - Add refreshOrders() method
4. `AndroidManifest.xml` - Add network security config reference

---

## GroceryDelivery App

### Current Configuration
- **Package**: `com.grocery.delivery`
- **Application ID**: `com.grocery.delivery` (debug: `.debug`)
- **Supabase URL**: `https://sjujrmvfzzzfskknvgjx.supabase.co` ⚠️ **INCORRECT**
- **Supabase Key**: Old/incorrect key
- **API Base URL**: `https://andoid-app-kotlin.vercel.app/api/`
- **Supabase Dependencies**: Currently commented out!

### Architecture
- **DI Framework**: Hilt/Dagger
- **Networking**: Retrofit + OkHttp
- **Database**: Room
- **Location Services**: Google Play Services Location
- **View Binding**: Enabled

### Key Repository
**DeliveryRepository** - All delivery operations:
- `getAvailableOrders()` - Get orders available to accept
- `getActiveOrder()` - Get current active delivery
- `getOrderHistory(limit)` - Get past deliveries
- `acceptOrder(assignmentId, notes)` - Accept delivery assignment
- `declineOrder(assignmentId, reason)` - Decline assignment
- `updateOrderStatus(assignmentId, status, notes, proofOfDelivery)` - Update status
- `updateLocation(lat, lng, orderId, accuracy, speed, heading)` - Send location
- `getProfile()` - Get driver profile
- `updateProfile(...)` - Update driver info

### MainActivity Structure
- Extends `BaseActivity<ActivityMainBinding>`
- Uses ViewModel: `AvailableOrdersViewModel`
- RecyclerView with `AvailableOrdersAdapter`
- Bottom Navigation (4 tabs):
  - Available Orders
  - Active Delivery
  - History
  - Profile
- SwipeRefreshLayout for manual refresh
- Observes: `ordersState` and `actionState` LiveData

### Realtime Sync Requirements
Delivery app needs real-time updates for:
1. **Delivery Assignments** (assigned to this driver)
   - New assignments
   - Assignment cancellations
   - Order details changes

2. **Active Order Status**
   - Customer/admin updates to active order
   - Delivery instructions changes
   - Order cancellations

3. **Location Broadcasting** (outgoing)
   - Send location updates in real-time to customers/admin

### Files to Create/Modify
**Create:**
1. `SupabaseModule.kt` - DI module for Supabase client
2. `RealtimeManager.kt` - Manage realtime subscriptions
3. `RealtimeConnectionState.kt` - Connection state sealed class
4. `network_security_config.xml` - Network security config

**Modify:**
1. `build.gradle.kts` - Uncomment and update Supabase dependencies + fix URL/key
2. `MainActivity.kt` - Integrate realtime lifecycle
3. `DeliveryRepository.kt` - Add refresh methods
4. `AndroidManifest.xml` - Add network security config reference

---

## Critical Issues Found

### 🚨 Incorrect Supabase Configuration
Both Admin and Delivery apps are configured with:
- **Wrong Supabase URL**: `sjujrmvfzzzfskknvgjx.supabase.co`
- **Wrong Supabase Key**: Old/incorrect anon key

**Correct Configuration (from Customer app):**
- **URL**: `https://hfxdxxpmcemdjsvhsdcf.supabase.co`
- **Anon Key**: `eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiJzdXBhYmFzZSIsInJlZiI6ImhmeGR4eHBtY2VtZGpzdmhzZGNmIiwicm9sZSI6ImFub24iLCJpYXQiOjE3NjA2NjU5MTcsImV4cCI6MjA3NjI0MTkxN30.X9lzy9TnHRjuzvWLYdsLUXHcd0668WwemOJ-GFQHqG8`

**This MUST be fixed first!**

---

## Realtime Subscription Strategy

### Admin App Subscriptions

#### 1. Orders Channel - Admin sees ALL orders
```kotlin
// Subscribe to all orders (no user filter)
channel("admin_orders_all")
  .postgresChangeFlow<PostgresAction>(schema = "public") {
    table = "orders"
    // No filter - admin sees all orders
  }
```

#### 2. Delivery Assignments Channel
```kotlin
// Subscribe to all delivery assignments
channel("admin_delivery_assignments")
  .postgresChangeFlow<PostgresAction>(schema = "public") {
    table = "delivery_assignments"
    // No filter - admin sees all assignments
  }
```

#### 3. Products/Inventory Channel (Optional)
```kotlin
// Subscribe to inventory changes
channel("admin_inventory")
  .postgresChangeFlow<PostgresAction>(schema = "public") {
    table = "inventory"
  }
```

### Delivery App Subscriptions

#### 1. Delivery Assignments - Driver's assignments only
```kotlin
// Subscribe to driver's own assignments
channel("driver_assignments_{driverId}")
  .postgresChangeFlow<PostgresAction>(schema = "public") {
    table = "delivery_assignments"
    filter = "delivery_personnel_id=eq.{driverId}"
  }
```

#### 2. Active Order Updates
```kotlin
// Subscribe to specific order when active
channel("driver_order_{orderId}")
  .postgresChangeFlow<PostgresAction>(schema = "public") {
    table = "orders"
    filter = "id=eq.{orderId}"
  }
```

---

## Database Tables Requiring Realtime

### Already Enabled ✅
- `cart`
- `orders`

### Need to Enable 📋
- `delivery_assignments`
- `delivery_locations` (for location tracking)
- `inventory` (optional for admin)
- `products` (optional for admin)

---

## RLS Policies Required

### For Admin (needs policies allowing admins to see everything)

```sql
-- Admin can view all orders
CREATE POLICY "Admins can view all orders"
  ON public.orders
  FOR SELECT
  TO authenticated
  USING (
    EXISTS (
      SELECT 1 FROM public.user_profiles
      WHERE id = auth.uid()
      AND user_type = 'admin'
    )
  );

-- Admin can view all delivery assignments
CREATE POLICY "Admins can view all delivery assignments"
  ON public.delivery_assignments
  FOR SELECT
  TO authenticated
  USING (
    EXISTS (
      SELECT 1 FROM public.user_profiles
      WHERE id = auth.uid()
      AND user_type = 'admin'
    )
  );
```

### For Delivery Driver

```sql
-- Drivers can view their assigned deliveries
CREATE POLICY "Drivers can view their own assignments"
  ON public.delivery_assignments
  FOR SELECT
  TO authenticated
  USING (
    auth.uid() = delivery_personnel_id
    OR auth.uid() = assigned_by
  );

-- Drivers can view orders they're assigned to
CREATE POLICY "Drivers can view assigned orders"
  ON public.orders
  FOR SELECT
  TO authenticated
  USING (
    id IN (
      SELECT order_id 
      FROM public.delivery_assignments
      WHERE delivery_personnel_id = auth.uid()
    )
  );
```

---

## Implementation Plan

### Step 1: Fix Supabase Configuration
- Update `build.gradle.kts` in both apps with correct URL and key
- Sync gradle files

### Step 2: Add Dependencies
- Add Supabase realtime dependencies (OkHttp engine)
- Add WebSocket support
- Add kotlinx-serialization

### Step 3: Create Realtime Infrastructure
- Copy `SupabaseModule.kt` from Customer app (with adjustments)
- Create `RealtimeManager.kt` for each app (different subscriptions)
- Create `RealtimeConnectionState.kt`
- Create `network_security_config.xml`

### Step 4: Enable Database Realtime
- Enable realtime publication for `delivery_assignments` table
- Create/verify RLS policies for admin and drivers

### Step 5: Integrate with Activities
- Update MainActivity in both apps
- Add lifecycle management for subscriptions
- Add refresh methods to repositories

### Step 6: Test
- Build both apps
- Install and test realtime connections
- Verify subscriptions work correctly
- Test actual data flow

---

## Success Criteria

### Admin App ✅
- [  ] Receives new order notifications in real-time
- [  ] Sees order status changes immediately
- [  ] Gets notified when drivers accept/decline assignments
- [  ] WebSocket connection stable
- [  ] No SSL errors
- [  ] Proper lifecycle management

### Delivery App ✅
- [  ] Receives new assignment notifications
- [  ] Sees order updates in real-time
- [  ] Active order updates automatically
- [  ] WebSocket connection stable
- [  ] No SSL errors
- [  ] Proper lifecycle management

---

## Next Steps

1. **Fix Supabase credentials** in both apps
2. **Implement Admin realtime** (priority: order updates)
3. **Implement Delivery realtime** (priority: assignment updates)
4. **Enable required realtime publications** in database
5. **Test end-to-end** realtime flow across all three apps

**Ready to proceed with implementation!** 🚀
