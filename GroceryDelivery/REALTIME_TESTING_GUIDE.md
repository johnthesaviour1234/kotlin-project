# Real-Time Sync Testing Guide - GroceryDelivery App

## Overview
This guide provides detailed instructions for testing the real-time synchronization feature in the GroceryDelivery app. The implementation uses Supabase Realtime Broadcast to receive order assignments and status updates in real-time.

## What Was Implemented

### 1. Dependencies
- ✅ Enabled Supabase SDK dependencies (postgrest-kt, realtime-kt, auth-kt, storage-kt)
- ✅ Added Ktor client for Android networking

### 2. Dependency Injection
**File:** `di/SupabaseModule.kt`
- ✅ Created Supabase client provider with Realtime plugin
- ✅ Configured reconnection behavior (5-second delay, 30-second heartbeat)
- ✅ Provides Auth, Postgrest, Realtime, and Storage instances

### 3. RealtimeManager Service
**File:** `services/RealtimeManager.kt`
- ✅ Subscribes to `delivery_orders` channel
- ✅ Listens for two broadcast events:
  - `order_assigned`: When admin assigns an order to driver
  - `order_confirmed`: When order status changes
- ✅ Emits SharedFlow events for consumption by ViewModels
- ✅ Manages connection lifecycle (connect/disconnect)
- ✅ Filters events by driver ID for security

### 4. ViewModel Integration
**File:** `ui/viewmodels/AvailableOrdersViewModel.kt`
- ✅ Observes realtime events in initialization
- ✅ Refreshes order list when new assignments arrive
- ✅ Updates UI when order status changes

### 5. MainActivity Lifecycle
**File:** `ui/activities/MainActivity.kt`
- ✅ Connects to realtime on activity start
- ✅ Reconnects on resume if disconnected
- ✅ Maintains connection in background (onPause)
- ✅ Disconnects on activity destruction
- ✅ Logs connection state changes

## Architecture

```
Backend (Supabase)
    ↓ [Broadcast Events]
RealtimeManager
    ↓ [SharedFlow Events]
AvailableOrdersViewModel
    ↓ [LiveData Updates]
MainActivity UI
```

## Testing Prerequisites

### Backend Requirements
Ensure the following database triggers are in place (from REALTIME_SYNC_MIGRATION_PLAN.md Phase 1):

1. **Order Assignment Trigger**
```sql
-- Trigger function for order assignments
CREATE OR REPLACE FUNCTION notify_order_assigned()
RETURNS TRIGGER AS $$
BEGIN
  PERFORM pg_notify(
    'delivery_orders',
    json_build_object(
      'event', 'order_assigned',
      'order_id', NEW.order_id,
      'assignment_id', NEW.id,
      'driver_id', NEW.delivery_person_id,
      'order_number', (SELECT order_number FROM orders WHERE id = NEW.order_id)
    )::text
  );
  RETURN NEW;
END;
$$ LANGUAGE plpgsql;

-- Attach trigger to delivery_assignments table
CREATE TRIGGER on_order_assigned
AFTER INSERT ON delivery_assignments
FOR EACH ROW
EXECUTE FUNCTION notify_order_assigned();
```

2. **Order Status Change Trigger**
```sql
-- Trigger function for order confirmations
CREATE OR REPLACE FUNCTION notify_order_confirmed()
RETURNS TRIGGER AS $$
BEGIN
  IF NEW.status = 'confirmed' AND OLD.status != 'confirmed' THEN
    PERFORM pg_notify(
      'delivery_orders',
      json_build_object(
        'event', 'order_confirmed',
        'order_id', NEW.id,
        'status', NEW.status,
        'delivery_person_id', NEW.delivery_person_id
      )::text
    );
  END IF;
  RETURN NEW;
END;
$$ LANGUAGE plpgsql;

-- Attach trigger to orders table
CREATE TRIGGER on_order_confirmed
AFTER UPDATE ON orders
FOR EACH ROW
EXECUTE FUNCTION notify_order_confirmed();
```

### App Configuration
1. Ensure `BuildConfig` has correct Supabase credentials:
   - `SUPABASE_URL`: https://sjujrmvfzzzfskknvgjx.supabase.co
   - `SUPABASE_ANON_KEY`: (your anon key)

2. User must be logged in as a delivery person
3. User ID should be stored in PreferencesManager

## Test Scenarios

### Test 1: Order Assignment Notification
**Objective:** Verify driver receives notification when admin assigns an order

**Steps:**
1. Login to GroceryDelivery app as a delivery person
2. Navigate to Available Orders screen (MainActivity)
3. Verify logcat shows: "Realtime connected"
4. From Admin app or database, create a new delivery_assignment:
   ```sql
   INSERT INTO delivery_assignments (order_id, delivery_person_id, status)
   VALUES ('order-uuid', 'driver-uuid', 'pending');
   ```
5. Watch GroceryDelivery app

**Expected Result:**
- Logcat shows: "Received order_assigned event"
- Logcat shows: "Order assigned: [ORDER_NUMBER]"
- ViewModel logs: "New order assigned: [ORDER_NUMBER]"
- Available orders list automatically refreshes
- New order appears in the list

**Success Criteria:** ✅ Order appears within 2 seconds without manual refresh

---

### Test 2: Order Status Change
**Objective:** Verify driver sees status updates in real-time

**Steps:**
1. Have an assigned order in "pending" status
2. From Admin app or database, confirm the order:
   ```sql
   UPDATE orders 
   SET status = 'confirmed' 
   WHERE id = 'order-uuid';
   ```
3. Watch GroceryDelivery app

**Expected Result:**
- Logcat shows: "Received order_confirmed event"
- Logcat shows: "Order status changed: [ORDER_ID] -> confirmed"
- Orders list refreshes automatically
- Order status badge updates to "Confirmed"

**Success Criteria:** ✅ Status change reflects within 2 seconds

---

### Test 3: Multi-Driver Filtering
**Objective:** Verify driver only receives events for their own orders

**Steps:**
1. Login as Driver A (user_id: driver-a-uuid)
2. From database, assign order to Driver B:
   ```sql
   INSERT INTO delivery_assignments (order_id, delivery_person_id, status)
   VALUES ('order-uuid', 'driver-b-uuid', 'pending');
   ```
3. Watch Driver A's app

**Expected Result:**
- Driver A's logcat shows broadcast received but ignored
- Driver A's orders list does NOT refresh
- No notification shown

**Success Criteria:** ✅ Driver only sees orders assigned to them

---

### Test 4: Connection Lifecycle
**Objective:** Verify realtime connection manages lifecycle properly

**Steps:**
1. Launch GroceryDelivery app
2. Check logcat for connection state
3. Press Home button (onPause)
4. Wait 10 seconds
5. Resume app (onResume)
6. Assign a new order
7. Verify notification received

**Expected Result:**
- On launch: "Realtime connecting..." → "Realtime connected"
- On pause: Connection maintained (no disconnect log)
- On resume: Either "already connected" or "reconnecting..."
- Events still received after resume

**Success Criteria:** ✅ Connection persists through app lifecycle

---

### Test 5: Reconnection on Network Loss
**Objective:** Verify app reconnects after network interruption

**Steps:**
1. Launch app with network enabled
2. Verify "Realtime connected" in logcat
3. Enable Airplane mode
4. Wait 10 seconds
5. Disable Airplane mode
6. Check logcat for reconnection attempt
7. Assign a new order
8. Verify event received

**Expected Result:**
- On network loss: Connection error logged
- On network restore: "Realtime connecting..." appears within 5 seconds
- After reconnect: "Realtime connected"
- Events received normally

**Success Criteria:** ✅ Auto-reconnects within 10 seconds of network restore

---

### Test 6: Logout Cleanup
**Objective:** Verify realtime disconnects on logout

**Steps:**
1. Login and verify connected
2. Tap logout from menu
3. Check logcat

**Expected Result:**
- "Disconnecting from realtime channel"
- "Successfully disconnected from realtime channel"
- Connection state: Disconnected

**Success Criteria:** ✅ Clean disconnect before logout completes

---

## Debugging Tips

### Enable Verbose Logging
Add to `RealtimeManager.kt`:
```kotlin
private const val VERBOSE_LOGGING = true

if (VERBOSE_LOGGING) {
    Log.v(TAG, "Raw broadcast message: ${message.payload}")
}
```

### Monitor Supabase Logs
1. Go to Supabase Dashboard → Logs
2. Filter by "Realtime"
3. Watch for connection events and broadcasts

### Check Database Triggers
Verify triggers are firing:
```sql
-- Check if trigger exists
SELECT * FROM pg_trigger WHERE tgname = 'on_order_assigned';

-- Test trigger manually
SELECT notify_order_assigned();
```

### Verify Channel Subscription
In Supabase Dashboard:
- Navigate to Realtime section
- Check active subscriptions
- Should see channel "delivery_orders" with 1+ subscribers

## Performance Metrics

Monitor these values during testing:

| Metric | Target | How to Measure |
|--------|--------|----------------|
| Connection Time | < 3 seconds | Time from `connect()` to "Connected" log |
| Event Latency | < 2 seconds | Time from trigger to event received |
| Reconnect Time | < 10 seconds | Time from network restore to connected |
| Memory Usage | < 50 MB | Android Profiler during 1-hour session |
| Battery Impact | < 5% per hour | Battery settings after extended use |

## Known Limitations

1. **Event Ordering:** Events may arrive out of order under high load
2. **Offline Queue:** Events triggered while offline are NOT queued
3. **Connection Limit:** Each device = 1 realtime connection
4. **Broadcast Size:** Event payload limited to 64 KB

## Rollback Plan

If realtime sync causes issues:

1. **Quick Fix:** Comment out realtime initialization in MainActivity:
```kotlin
// setupRealtimeConnection()
```

2. **Feature Flag:** Add to PreferencesManager:
```kotlin
fun isRealtimeEnabled(): Boolean = 
    sharedPreferences.getBoolean("realtime_enabled", false)
```

3. **Revert Dependencies:** Comment out Supabase dependencies in `build.gradle.kts`

## Next Steps

After successful testing:

1. ✅ Implement same pattern for Customer app
2. ✅ Implement same pattern for Admin app
3. ✅ Add metrics collection (Phase 6 of migration plan)
4. ✅ Implement error recovery strategies
5. ✅ Add user-facing connection indicators
6. ✅ Optimize battery usage for background connections

## Support

For issues or questions:
- Check logcat for error details
- Review Supabase dashboard logs
- Verify database triggers are active
- Ensure RLS policies allow broadcasts
- Test with fresh database inserts

## Change Log

| Date | Changes |
|------|---------|
| 2025-01-XX | Initial implementation - Phase 4 (Delivery app) |

---

**Status:** ✅ Ready for Testing
**Priority:** High
**Estimated Test Time:** 2-3 hours for full suite
