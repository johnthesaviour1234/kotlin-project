# Supabase Realtime Implementation SOP

## Standard Operating Procedure for Adding Realtime Sync to Apps/Pages

This document outlines the step-by-step process to add Supabase Realtime synchronization to any page or app in the Grocery ecosystem.

---

## Overview

Supabase Realtime uses **Broadcast** to push database changes to connected clients in real-time. When a database record is inserted, updated, or deleted, a trigger function inserts a message into the `realtime.messages` table with `extension='broadcast'`, and subscribed clients receive the update via WebSocket.

**Key Concepts:**
- **Topic**: Channel name that clients subscribe to (e.g., `delivery_orders_{driver_id}`)
- **Extension**: Must be set to `'broadcast'` for custom broadcast messages
- **Event**: The operation type passed as event name - `INSERT`, `UPDATE`, or `DELETE`
- **Payload**: JSONB data containing the change details

---

## Prerequisites

1. Supabase project with Realtime enabled
2. Kotlin app with Supabase Kotlin client dependencies
3. Database tables that need real-time sync
4. User authentication (JWT tokens)

---

## Implementation Steps

### Step 1: Database Setup - Create Broadcast Trigger

For each table that needs realtime sync, create a trigger function and trigger.

#### 1.1 Create RLS Policy for Realtime Messages (One-time setup)

```sql
-- Enable RLS on realtime.messages if not already enabled
ALTER TABLE realtime.messages ENABLE ROW LEVEL SECURITY;

-- Create policy to allow authenticated users to receive broadcasts
CREATE POLICY "Allow authenticated users to receive broadcasts"
ON realtime.messages
FOR SELECT
TO authenticated
USING (true);
```

#### 1.2 Create Trigger Function for Your Table

Replace `{table_name}` and `{topic_name}` with your specific values.

```sql
-- Create trigger function for broadcasting changes
-- IMPORTANT: The 'event' field is used as the broadcast event name that clients subscribe to
CREATE OR REPLACE FUNCTION public.broadcast_{table_name}_changes()
RETURNS TRIGGER
LANGUAGE plpgsql
SECURITY DEFINER
AS $$
DECLARE
  topic_name TEXT;
BEGIN
  -- Construct topic name (can be dynamic based on user_id, driver_id, etc.)
  -- Example for user-specific: topic_name := 'user_cart_' || NEW.user_id;
  -- Example for driver-specific: topic_name := 'delivery_orders_' || NEW.delivery_personnel_id;
  -- Example for admin (all): topic_name := 'admin_orders_all';
  
  IF TG_OP = 'DELETE' THEN
    topic_name := '{topic_prefix}_' || OLD.{id_column};  -- Use OLD for DELETE
  ELSE
    topic_name := '{topic_prefix}_' || NEW.{id_column};  -- Use NEW for INSERT/UPDATE
  END IF;
  
  -- Insert into realtime.messages
  -- The 'event' field (TG_OP) becomes the broadcast event name
  INSERT INTO realtime.messages (
    topic,
    extension,
    payload,
    event
  )
  VALUES (
    topic_name,                -- Topic clients subscribe to
    'broadcast',               -- REQUIRED: Must be 'broadcast' for custom broadcasts
    json_build_object(
      'type', TG_OP,
      'table', TG_TABLE_NAME,
      'record', CASE WHEN TG_OP = 'DELETE' THEN row_to_json(OLD) ELSE row_to_json(NEW) END
    )::jsonb,
    TG_OP                      -- INSERT, UPDATE, or DELETE - used as event name
  );
  
  RETURN COALESCE(NEW, OLD);
EXCEPTION WHEN OTHERS THEN
  -- Log error but don't block the transaction
  RAISE WARNING 'Broadcast failed: %', SQLERRM;
  RETURN COALESCE(NEW, OLD);
END;
$$;
```

#### 1.3 Create Trigger on Table

```sql
-- Drop existing trigger if exists
DROP TRIGGER IF EXISTS {table_name}_broadcast_trigger ON public.{table_name};

-- Create trigger
CREATE TRIGGER {table_name}_broadcast_trigger
AFTER INSERT OR UPDATE OR DELETE ON public.{table_name}
FOR EACH ROW
EXECUTE FUNCTION public.broadcast_{table_name}_changes();
```

#### 1.4 Verify Trigger Creation

```sql
-- Check if trigger exists
SELECT trigger_name, event_manipulation 
FROM information_schema.triggers 
WHERE event_object_table = '{table_name}' 
AND trigger_name LIKE '%broadcast%';
```

---

### Step 2: Android App Setup - RealtimeManager

Create or update a `RealtimeManager` class for managing WebSocket subscriptions.

#### 2.1 Create RealtimeManager Class

**File:** `app/src/main/java/{package}/data/sync/RealtimeManager.kt`

```kotlin
package {your.package}.data.sync

import android.util.Log
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.realtime.RealtimeChannel
import io.github.jan.supabase.realtime.channel
import io.github.jan.supabase.realtime.realtime
import io.github.jan.supabase.realtime.broadcastFlow
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.serialization.json.JsonObject
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class RealtimeManager @Inject constructor(
    private val supabaseClient: SupabaseClient
) {
    private companion object {
        const val TAG = "RealtimeManager"
    }
    
    private var channel: RealtimeChannel? = null
    private var jobs = mutableListOf<Job>()
    private val scope = CoroutineScope(Dispatchers.IO)
    
    private val _connectionState = MutableStateFlow<RealtimeConnectionState>(
        RealtimeConnectionState.Disconnected
    )
    val connectionState: StateFlow<RealtimeConnectionState> = _connectionState
    
    // SharedFlow to broadcast when data needs to be refreshed
    private val _refreshTrigger = MutableSharedFlow<String>(replay = 0)
    val refreshTrigger: SharedFlow<String> = _refreshTrigger.asSharedFlow()
    
    /**
     * Subscribe to a specific topic and listen for broadcast events
     * @param topicName The topic name (e.g., 'delivery_orders_abc123', 'admin_orders_all')
     * 
     * IMPORTANT: The event name parameter in broadcastFlow() must match
     * the 'event' field value from the database trigger (INSERT, UPDATE, DELETE)
     */
    suspend fun subscribeToTopic(topicName: String) {
        try {
            Log.d(TAG, "Subscribing to topic: $topicName")
            
            // Unsubscribe from existing channel if any
            unsubscribe()
            
            // Create new channel - channel name must match the 'topic' from trigger
            channel = supabaseClient.realtime.channel(topicName)
            
            // Subscribe to INSERT events
            // The string "INSERT" must match the 'event' field from trigger
            val insertJob = channel!!.broadcastFlow<JsonObject>("INSERT")
                .onEach { message ->
                    Log.d(TAG, "🆕 INSERT broadcast received on $topicName")
                    handleDataChange(topicName)
                }
                .launchIn(scope)
            jobs.add(insertJob)
            
            // Subscribe to UPDATE events
            val updateJob = channel!!.broadcastFlow<JsonObject>("UPDATE")
                .onEach { message ->
                    Log.d(TAG, "📝 UPDATE broadcast received on $topicName")
                    handleDataChange(topicName)
                }
                .launchIn(scope)
            jobs.add(updateJob)
            
            // Subscribe to DELETE events
            val deleteJob = channel!!.broadcastFlow<JsonObject>("DELETE")
                .onEach { message ->
                    Log.d(TAG, "🗑️ DELETE broadcast received on $topicName")
                    handleDataChange(topicName)
                }
                .launchIn(scope)
            jobs.add(deleteJob)
            
            // Subscribe to the channel to start receiving messages
            channel?.subscribe()
            
            _connectionState.value = RealtimeConnectionState.Connected
            Log.d(TAG, "✅ Successfully subscribed to $topicName")
            
        } catch (e: Exception) {
            Log.e(TAG, "❌ Failed to subscribe to $topicName", e)
            _connectionState.value = RealtimeConnectionState.Error(e.message ?: "Unknown error")
        }
    }
    
    /**
     * Unsubscribe from all channels
     */
    suspend fun unsubscribe() {
        try {
            jobs.forEach { it.cancel() }
            jobs.clear()
            channel?.unsubscribe()
            channel = null
            _connectionState.value = RealtimeConnectionState.Disconnected
            Log.d(TAG, "Unsubscribed from all channels")
        } catch (e: Exception) {
            Log.e(TAG, "Error unsubscribing", e)
        }
    }
    
    /**
     * Handle data change by triggering refresh
     */
    private fun handleDataChange(topicName: String) {
        Log.d(TAG, "📱 Data change detected on $topicName - broadcasting refresh trigger")
        scope.launch {
            _refreshTrigger.emit(topicName)
        }
    }
}

sealed class RealtimeConnectionState {
    object Disconnected : RealtimeConnectionState()
    object Connected : RealtimeConnectionState()
    data class Error(val message: String) : RealtimeConnectionState()
}
```

---

### Step 3: Integrate Realtime in Activity/Fragment

#### 3.1 Inject RealtimeManager

```kotlin
@AndroidEntryPoint
class YourActivity : AppCompatActivity() {
    
    @Inject
    lateinit var realtimeManager: RealtimeManager
    
    // ... rest of code
}
```

#### 3.2 Subscribe to Realtime in onResume()

```kotlin
override fun onResume() {
    super.onResume()
    
    // Subscribe to realtime when app comes to foreground
    lifecycleScope.launch {
        try {
            realtimeManager.subscribeToTopic("your_topic_name")
            Log.d(TAG, "✅ Realtime sync active")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to start realtime sync", e)
        }
    }
}
```

#### 3.3 Unsubscribe in onPause()

```kotlin
override fun onPause() {
    super.onPause()
    
    // Unsubscribe when app goes to background to save battery
    lifecycleScope.launch {
        realtimeManager.unsubscribe()
    }
}
```

#### 3.4 Observe Refresh Trigger in Fragment/ViewModel

**In Fragment:**

```kotlin
@AndroidEntryPoint
class YourFragment : Fragment() {
    
    @Inject
    lateinit var realtimeManager: RealtimeManager
    
    private val viewModel: YourViewModel by viewModels()
    
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        // Observe realtime updates
        observeRealtimeUpdates()
    }
    
    private fun observeRealtimeUpdates() {
        viewLifecycleOwner.lifecycleScope.launch {
            realtimeManager.refreshTrigger.collect { topicName ->
                Log.d(TAG, "🔔 Realtime update received from $topicName - auto-refreshing")
                viewModel.refreshData()  // Reload data from API
            }
        }
    }
}
```

**In ViewModel:**

```kotlin
@HiltViewModel
class YourViewModel @Inject constructor(
    private val repository: YourRepository
) : ViewModel() {
    
    fun refreshData() {
        // Reload data from repository/API
        loadData()
    }
}
```

---

### Step 4: Testing

#### 4.1 Test Realtime Connection

1. Open the app and navigate to the screen with realtime
2. Check logs for: `✅ Successfully subscribed to {topic_name}`
3. Check for heartbeat logs every 15 seconds: `Heartbeat received`

#### 4.2 Test Broadcast Events

1. Keep app open on realtime-enabled screen
2. Create/update/delete a record in the database (via another app/API)
3. Check logs for broadcast messages:
   - `🆕 INSERT broadcast received`
   - `📝 UPDATE broadcast received`
   - `🗑️ DELETE broadcast received`
4. Verify the UI refreshes automatically

#### 4.3 Check for Errors

```bash
# View all realtime logs
adb logcat -d | grep -i "realtime"

# View broadcast logs
adb logcat -d | grep -i "broadcast"

# View errors
adb logcat -d | grep -E "ERROR|Failed"
```

---

## Common Topics by App Type

### Admin App Topics
- `admin_orders_all` - All orders across all users
- `admin_delivery_assignments` - All delivery assignments
- `admin_inventory` - Inventory changes
- `admin_users` - User account changes

### Customer App Topics
- `user_orders_{user_id}` - User's specific orders
- `user_cart_{user_id}` - User's shopping cart
- `user_notifications_{user_id}` - User notifications

### Delivery App Topics
- `delivery_orders_{driver_id}` - Orders assigned to specific driver (e.g., `delivery_orders_abc-123-def`)
- `delivery_location_{driver_id}` - Real-time location updates for driver

---

## Topic Naming Convention

Use clear, descriptive topic names:

| Pattern | Example | Description |
|---------|---------|-------------|
| `{app}_{resource}_all` | `admin_orders_all` | Admin sees all records |
| `{resource}_{user_id}` | `orders_abc123` | User-specific data |
| `{app}_{resource}_{id}` | `driver_assignments_xyz789` | Specific entity for role |

---

## Troubleshooting

### Issue 1: No Broadcast Events Received

**Check:**
1. Trigger exists and is active:
```sql
SELECT * FROM information_schema.triggers 
WHERE event_object_table = 'your_table';
```

2. RLS policy exists on `realtime.messages`:
```sql
SELECT * FROM pg_policies 
WHERE tablename = 'messages' AND schemaname = 'realtime';
```

3. App is subscribed:
```
Check logs for: "✅ Successfully subscribed to {topic}"
```

### Issue 2: "null value in column 'extension'" Error

**Cause:** The `extension` column in `realtime.messages` is NOT NULL and must be set.

**Solution:** Always set `extension = 'broadcast'` in your trigger:
```sql
INSERT INTO realtime.messages (topic, extension, payload, event)
VALUES (
  'your_topic',
  'broadcast',  -- REQUIRED: Must be 'broadcast'
  '{}',
  'INSERT'
);
```

### Issue 3: Trigger Blocks Database Operations

**Solution:** Wrap broadcast in exception handler:
```sql
BEGIN
  -- broadcast code
EXCEPTION WHEN OTHERS THEN
  NULL;  -- Silently ignore errors
END;
```

---

## Best Practices

1. **Always unsubscribe in `onPause()`** to save battery
2. **Use specific topics** - avoid broad topics with high traffic
3. **Throttle refresh** - debounce multiple rapid updates
4. **Test offline behavior** - handle connection drops gracefully
5. **Log all realtime events** during development
6. **Add error handling** in trigger functions to not block transactions
7. **Use userId in topic names** for user-specific data
8. **Monitor Supabase Realtime quotas** in dashboard

---

## Security Considerations

### For Private Data (User-Specific)

If data should only be visible to specific users, set `private = true` and add RLS:

```sql
-- Set private = true in trigger
private = true,

-- Add RLS policy on realtime.messages
CREATE POLICY "Users can only see their own messages"
ON realtime.messages
FOR SELECT
TO authenticated
USING (
  -- Extract user_id from topic name
  topic = 'user_orders_' || auth.uid()::text
);
```

### For Public Data (Admin View All)

Use `private = false` and allow all authenticated users:

```sql
-- Set private = false in trigger
private = false,

-- Simple RLS policy
CREATE POLICY "Allow authenticated users to receive broadcasts"
ON realtime.messages
FOR SELECT
TO authenticated
USING (true);
```

---

## Performance Tips

1. **Limit payload size** - Only include necessary fields in broadcast
2. **Use pagination** - Don't refresh entire lists, just append new items
3. **Batch updates** - If multiple changes happen rapidly, debounce UI updates
4. **Index topic column** - Ensure `realtime.messages(topic)` is indexed

---

## Appendix: Complete Working Examples

### Example 1: Admin App - All Orders

See the `GroceryAdmin` app for a complete reference:

- **Database:** `orders` table with `orders_broadcast_trigger`
- **Topic:** `admin_orders_all` (all users)
- **RealtimeManager:** `app/src/main/java/com/grocery/admin/data/sync/RealtimeManager.kt`
- **Fragment:** `app/src/main/java/com/grocery/admin/ui/fragments/OrdersFragment.kt`
- **MainActivity:** Lifecycle management in `onResume()`/`onPause()`

### Example 2: Delivery App - Driver-Specific Orders

See the `GroceryDelivery` app for driver-specific implementation:

- **Database:** `delivery_assignments` table with `broadcast_delivery_assignments_changes()` trigger
- **Topic:** `delivery_orders_{driver_id}` (user-specific, e.g., `delivery_orders_abc-123`)
- **RealtimeManager:** `app/src/main/java/com/grocery/delivery/services/RealtimeManager.kt`
- **MainActivity:** Observes `orderRefreshTrigger` and calls `viewModel.refreshOrders()`
- **Key Feature:** Dynamic topic based on logged-in driver ID

---

## Summary Checklist

- [ ] Database trigger function created
- [ ] Database trigger attached to table
- [ ] RLS policy on `realtime.messages` created
- [ ] `RealtimeManager` implemented
- [ ] Realtime injected in Activity/Fragment
- [ ] Subscribe in `onResume()`
- [ ] Unsubscribe in `onPause()`
- [ ] Observe `refreshTrigger` in Fragment
- [ ] Tested with INSERT/UPDATE/DELETE operations
- [ ] Logs show successful subscription and broadcasts
- [ ] UI refreshes automatically on changes

---

**Document Version:** 1.0  
**Last Updated:** October 30, 2025  
**Author:** Grocery App Team
