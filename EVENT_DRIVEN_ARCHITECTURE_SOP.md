# Event-Driven Architecture Integration SOP

## Standard Operating Procedure for Real-Time Event Integration

**Document Version:** 1.1  
**Date:** January 29, 2025  
**Last Updated:** January 29, 2025 (Added Event Queue Section)

---

## Table of Contents
1. [Overview](#overview)
2. [Architecture Components](#architecture-components)
3. [Integration Workflow](#integration-workflow)
4. [Common Mistakes & Solutions](#common-mistakes--solutions)
5. [Checklist](#checklist)
6. [Troubleshooting Guide](#troubleshooting-guide)

---

## Overview

This document outlines the standard procedure for integrating real-time events into the Grocery Customer application. The event-driven architecture consists of three main layers:

1. **Backend (Vercel API)** - Broadcasts events via Supabase Realtime
2. **Android RealtimeManager** - Receives broadcasts and publishes to EventBus
3. **Android ViewModels** - Subscribes to EventBus and updates UI

**Event Flow:**
```
Backend API → Supabase Realtime → RealtimeManager → EventBus → ViewModel → UI
```

---

## Architecture Components

### 1. Backend Components

**Location:** `grocery-delivery-api/`

- **EventBroadcaster** (`lib/eventBroadcaster.js`)
  - Manages Supabase Realtime channels
  - Broadcasts events to subscribed clients
  
- **API Endpoints** (`pages/api/`)
  - Handle HTTP requests
  - Call EventBroadcaster after database operations

### 2. Android Components

**Location:** `GroceryCustomer/app/src/main/java/com/grocery/customer/`

- **Event Classes** (`data/local/Event.kt`)
  - Define event types
  
- **EventBus** (`data/local/EventBus.kt`)
  - Local event distribution system
  
- **RealtimeManager** (`data/remote/RealtimeManager.kt`)
  - Subscribes to Supabase Realtime channels
  - Converts broadcasts to EventBus events
  
- **ViewModels** (`ui/viewmodels/`)
  - Subscribe to EventBus events
  - Update UI state

---

## Integration Workflow

### Step 1: Define Event Type

**File:** `GroceryCustomer/app/src/main/java/com/grocery/customer/data/local/Event.kt`

```kotlin
sealed class Event {
    // Add your new event here
    data class CartItemAdded(val productId: String, val quantity: Int) : Event()
}
```

**✅ Correct:**
- Use sealed class hierarchy
- Include all necessary data in event parameters
- Use descriptive names (e.g., `CartItemAdded`, not `Update`)

**❌ Mistake to Avoid:**
- Don't use generic event names
- Don't omit necessary data fields

---

### Step 2: Create Serializable Payload (if needed)

**File:** `GroceryCustomer/app/src/main/java/com/grocery/customer/data/remote/RealtimeManager.kt`

```kotlin
@Serializable
data class CartUpdatePayload(
    val action: String,
    val productId: String? = null,
    val itemId: String? = null,
    val quantity: Int? = null,
    val timestamp: String? = null
)
```

**✅ Correct:**
- Mark class with `@Serializable` annotation
- Use explicit types (String, Int, etc.), NOT `Any?`
- Make fields nullable if they're optional
- Ensure Kotlin serialization plugin is configured

**❌ Critical Mistakes We Made:**
1. ❌ Used `Map<String, Any?>` instead of `@Serializable` data class
   - **Problem:** `Any?` type cannot be serialized
   - **Error:** `SerializationException: Serializer for class 'Any' is not found`
   - **Solution:** Create proper data class with explicit types

2. ❌ Forgot to add Kotlin serialization plugin
   - **Problem:** `@Serializable` annotation doesn't generate serializers without plugin
   - **Error:** `Serializer for class 'CartUpdatePayload' is not found`
   - **Solution:** Add to `app/build.gradle.kts`:
     ```kotlin
     plugins {
         kotlin("plugin.serialization") version "1.9.20"
     }
     dependencies {
         implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.6.0")
     }
     ```

---

### Step 3: Add RealtimeManager Subscription

**File:** `GroceryCustomer/app/src/main/java/com/grocery/customer/data/remote/RealtimeManager.kt`

```kotlin
private fun subscribeToCartUpdates(userId: String) {
    scope.launch {
        try {
            val channelName = "cart:$userId"
            
            // ✅ CRITICAL: Check if channel already exists
            if (channels.containsKey(channelName)) {
                Log.d(TAG, "Already subscribed to $channelName")
                return@launch
            }
            
            val channel = supabaseClient.realtime.channel(channelName)
            
            // Subscribe to channel FIRST
            channel.subscribe()
            channels[channelName] = channel
            Log.d(TAG, "Subscribed to $channelName")
            
            // THEN listen for broadcast events
            channel.broadcastFlow<CartUpdatePayload>("updated")
                .onEach { payload ->
                    Log.d(TAG, "Event received: $payload")
                    
                    when (payload.action) {
                        "item_added" -> {
                            if (payload.productId != null && payload.quantity != null) {
                                eventBus.publish(Event.CartItemAdded(payload.productId, payload.quantity))
                            }
                        }
                    }
                }
                .launchIn(scope)
        } catch (e: Exception) {
            Log.e(TAG, "Error subscribing to cart updates", e)
        }
    }
}
```

**✅ Correct:**
1. Check `channels.containsKey()` to prevent duplicates
2. Call `channel.subscribe()` BEFORE setting up `broadcastFlow()`
3. Store channel in `channels` map
4. Use proper `@Serializable` data class for payload type
5. Add comprehensive logging
6. Publish to EventBus after decoding

**❌ Critical Mistakes We Made:**

1. ❌ Created duplicate channel subscriptions
   - **Problem:** Multiple threads created channels with same name
   - **Symptom:** Supabase sent `phx_close` immediately after subscription
   - **Error in logs:** `Received event broadcast for channel null`
   - **Solution:** Always check `channels.containsKey()` first

2. ❌ Set up `broadcastFlow()` BEFORE calling `subscribe()`
   - **Problem:** Flow listener registered before channel connected
   - **Symptom:** Events not received
   - **Solution:** Call `channel.subscribe()` first, then set up `broadcastFlow()`

3. ❌ Used wrong payload type in `broadcastFlow<>()`
   - **Problem:** Used `Map<String, Any?>` instead of serializable class
   - **Solution:** Use `@Serializable` data class

---

### Step 4: Add ViewModel Subscription

**File:** `GroceryCustomer/app/src/main/java/com/grocery/customer/ui/viewmodels/CartViewModel.kt`

```kotlin
@HiltViewModel
class CartViewModel @Inject constructor(
    private val cartRepository: CartRepository,
    private val eventBus: EventBus
) : BaseViewModel() {
    
    private val TAG = "CartViewModel"
    
    init {
        // Subscribe to cart events
        viewModelScope.launch {
            eventBus.subscribe<Event.CartItemAdded>().collect { event ->
                Log.d(TAG, "CartItemAdded event received: productId=${event.productId}")
                refreshCart()
            }
        }
    }
    
    fun refreshCart() {
        viewModelScope.launch {
            try {
                Log.d(TAG, "Refreshing cart from server...")
                cartRepository.refreshCart()
                Log.d(TAG, "Cart refreshed successfully")
            } catch (exception: Exception) {
                Log.e(TAG, "Error refreshing cart: ${exception.message}", exception)
            }
        }
    }
}
```

**✅ Correct:**
- Subscribe in `init` block or `viewModelScope`
- Add logging for debugging
- Handle errors gracefully
- Call repository to refresh data

**❌ Mistake to Avoid:**
- Don't subscribe in UI components (Fragments/Activities)
- Don't forget error handling
- Don't block the main thread

---

### Step 5: Add Backend Event Broadcasting

**File:** `grocery-delivery-api/pages/api/cart/index.js`

```javascript
import eventBroadcaster from '../../../lib/eventBroadcaster.js'

export default async function handler(req, res) {
    const userId = userData.user.id
    
    if (req.method === 'POST') {
        // ... database operations ...
        
        // ✅ CRITICAL: Broadcast event after successful DB operation
        await eventBroadcaster.cartUpdated(userId, { 
            action: 'item_added', 
            productId: product_id, 
            quantity 
        })
        
        return res.status(200).json(formatSuccessResponse(result.data))
    }
}
```

**✅ Correct:**
1. Import `eventBroadcaster` at the top
2. Broadcast AFTER successful database operation
3. Use descriptive action names
4. Include all necessary data in payload
5. Await the broadcast (it's async)

**❌ Critical Mistake We Made:**
- ❌ Backend forgot to subscribe channels before broadcasting
  - **Problem:** `eventBroadcaster.getChannel()` created channel but never called `.subscribe()`
  - **Symptom:** Broadcasts sent but not received by clients
  - **Solution:** Modified `eventBroadcaster.js` to auto-subscribe when creating channels:
    ```javascript
    getChannel(channelName) {
        if (!this.channels.has(channelName)) {
            const channel = supabase.channel(channelName)
            // ✅ Subscribe immediately
            channel.subscribe((status) => {
                console.log(`[EventBroadcaster] Subscribed to channel: ${channelName}`)
            })
            this.channels.set(channelName, channel)
        }
        return this.channels.get(channelName)
    }
    ```

---

### Step 6: Add Backend Event Method (if needed)

**File:** `grocery-delivery-api/lib/eventBroadcaster.js`

```javascript
/**
 * Broadcast cart update to user
 * @param {string} userId - User ID
 * @param {object} cart - Cart data
 */
async cartUpdated(userId, cart) {
    try {
        await this.broadcastToChannel(`cart:${userId}`, 'updated', cart)
        console.log(`[EventBroadcaster] Cart updated for user ${userId}`)
    } catch (error) {
        console.error('[EventBroadcaster] Error broadcasting cart update:', error)
    }
}
```

**✅ Correct:**
- Use consistent channel naming (`cart:${userId}`)
- Use descriptive event names (`updated`)
- Add error handling
- Add logging

---

### Step 7: Event Queue with Automatic Retry (Optional - See Limitations)

**⚠️ IMPORTANT LIMITATION:** Event queue with `setInterval()` retry worker **does NOT work on Vercel** due to serverless stateless architecture. Function instances shut down after request completion, wiping in-memory queues.

**When Queue Works:**
- ✅ Traditional Node.js servers (Express, Fastify, etc.)
- ✅ Long-running processes
- ✅ Docker containers

**When Queue FAILS:**
- ❌ Vercel serverless functions
- ❌ AWS Lambda
- ❌ Any stateless FaaS platform

**File:** `grocery-delivery-api/lib/eventBroadcaster.js`

```javascript
class EventBroadcaster {
  constructor() {
    this.channels = new Map()
    this.eventQueue = new Map() // Map<channelName, Array<{event, payload, timestamp}>>
    this.retryInterval = 5000 // Retry every 5 seconds
    this.maxRetries = 12 // Retry for up to 1 minute
    this.startRetryWorker() // ⚠️ Won't persist on Vercel!
  }
  
  /**
   * Start background worker to retry failed broadcasts
   * ⚠️ WARNING: This does NOT work on Vercel serverless!
   */
  startRetryWorker() {
    setInterval(() => {
      this.processEventQueue()
    }, this.retryInterval)
  }
  
  /**
   * Process queued events and retry sending
   */
  async processEventQueue() {
    for (const [channelName, events] of this.eventQueue.entries()) {
      if (events.length === 0) continue
      
      console.log(`[EventBroadcaster] Processing ${events.length} queued events for ${channelName}`)
      
      const remainingEvents = []
      
      for (const queuedEvent of events) {
        const retryCount = queuedEvent.retryCount || 0
        
        if (retryCount >= this.maxRetries) {
          console.warn(`[EventBroadcaster] Dropping event (max retries)`)
          continue
        }
        
        try {
          const channel = this.getChannel(channelName)
          await channel.send({
            type: 'broadcast',
            event: queuedEvent.event,
            payload: queuedEvent.payload
          })
          console.log(`[EventBroadcaster] ✅ Retry successful`)
        } catch (error) {
          console.error(`[EventBroadcaster] ❌ Retry failed:`, error.message)
          remainingEvents.push({
            ...queuedEvent,
            retryCount: retryCount + 1
          })
        }
      }
      
      if (remainingEvents.length > 0) {
        this.eventQueue.set(channelName, remainingEvents)
      } else {
        this.eventQueue.delete(channelName)
      }
    }
  }
  
  /**
   * Queue event for retry if broadcast fails
   */
  queueEvent(channelName, event, payload) {
    if (!this.eventQueue.has(channelName)) {
      this.eventQueue.set(channelName, [])
    }
    
    this.eventQueue.get(channelName).push({
      event,
      payload,
      timestamp: Date.now(),
      retryCount: 0
    })
    
    console.log(`[EventBroadcaster] Queued ${event} for retry`)
  }
  
  async broadcastToChannel(channelName, event, payload) {
    try {
      const channel = this.getChannel(channelName)
      await channel.send({
        type: 'broadcast',
        event,
        payload: { ...payload, timestamp: new Date().toISOString() }
      })
      console.log(`[EventBroadcaster] ✅ Broadcasted ${event}`)
    } catch (error) {
      console.error(`[EventBroadcaster] ❌ Broadcast failed:`, error.message)
      // Queue for retry (but won't work on Vercel!)
      this.queueEvent(channelName, event, payload)
    }
  }
}
```

**Why This Doesn't Work on Vercel:**
1. Vercel functions are **stateless** - each request gets a fresh instance
2. `setInterval()` background worker **terminates** when function completes
3. In-memory `eventQueue` Map is **wiped** between requests
4. No persistent storage for retry queue

**Alternative Solutions for Serverless:**

**Option 1: Client-Side Retry (Recommended for Vercel)**
- Android app detects missed events on reconnection
- Polls cart API for a few seconds after reconnect
- Simpler and works with stateless backends

**Option 2: External Queue Service**
- Use Redis, RabbitMQ, or AWS SQS
- Store failed broadcasts in persistent queue
- Separate worker service processes queue
- More complex but production-ready

**Option 3: Supabase Edge Functions**
- Deploy EventBroadcaster as Supabase Edge Function
- Use Supabase's built-in Realtime infrastructure
- May have better persistence options

**Current Recommendation:**
For Vercel deployment, **accept that some broadcasts may be missed during disconnections** and rely on:
1. Android app's automatic reconnection
2. Cart refresh when user navigates to cart screen
3. Manual refresh if needed

For production, consider migrating to a traditional Node.js server or implementing Option 2 with external queue service.

---

## Common Mistakes & Solutions

### Mistake 1: Serialization Errors

**Symptom:**
```
E Realtime: SerializationException: Serializer for class 'Any' is not found
E Realtime: Serializer for class 'CartUpdatePayload' is not found
```

**Root Causes:**
1. Using `Map<String, Any?>` instead of `@Serializable` data class
2. Missing Kotlin serialization plugin
3. Forgot `@Serializable` annotation

**Solution:**
1. Create `@Serializable` data class with explicit types
2. Add serialization plugin to `build.gradle.kts`
3. Use the data class in `broadcastFlow<CartUpdatePayload>()`

---

### Mistake 2: Duplicate Channel Subscriptions

**Symptom:**
```
D Realtime: Joined channel realtime:cart:8114f180-e4de-4a06-812b-f907b80c9eb7
D Realtime: Received event phx_close for channel realtime:cart:8114f180-e4de-4a06-812b-f907b80c9eb7
D Realtime: Unsubscribed from channel realtime:cart:8114f180-e4de-4a06-812b-f907b80c9eb7
D Realtime: Received event broadcast for channel null
```

**Root Cause:**
- Multiple calls to `supabaseClient.realtime.channel(name)` with same name
- Supabase detects duplicates and closes them

**Solution:**
```kotlin
private fun subscribeToCartUpdates(userId: String) {
    scope.launch {
        val channelName = "cart:$userId"
        
        // ✅ Check if already subscribed
        if (channels.containsKey(channelName)) {
            Log.d(TAG, "Already subscribed to $channelName")
            return@launch
        }
        
        val channel = supabaseClient.realtime.channel(channelName)
        // ... rest of code
    }
}
```

---

### Mistake 3: Wrong Subscription Order

**Symptom:**
- Channels join successfully
- No broadcasts received
- No errors in logs

**Root Cause:**
- Set up `broadcastFlow()` listener BEFORE calling `channel.subscribe()`

**Solution:**
```kotlin
// ✅ CORRECT ORDER:
val channel = supabaseClient.realtime.channel(channelName)

// 1. Subscribe FIRST
channel.subscribe()
channels[channelName] = channel

// 2. Set up flow listener AFTER
channel.broadcastFlow<CartUpdatePayload>("updated")
    .onEach { /* handle */ }
    .launchIn(scope)
```

---

### Mistake 4: Backend Not Broadcasting

**Symptom:**
- Android app subscribes successfully
- No broadcasts received
- No backend logs

**Root Cause:**
1. Forgot to call `eventBroadcaster` in API endpoint
2. Backend channels not subscribed before broadcasting

**Solution:**
1. Add broadcast call after DB operation:
   ```javascript
   await eventBroadcaster.cartUpdated(userId, { action: 'item_added', productId, quantity })
   ```

2. Ensure EventBroadcaster auto-subscribes channels:
   ```javascript
   getChannel(channelName) {
       if (!this.channels.has(channelName)) {
           const channel = supabase.channel(channelName)
           channel.subscribe() // ✅ Subscribe immediately
           this.channels.set(channelName, channel)
       }
       return this.channels.get(channelName)
   }
   ```

---

### Mistake 5: Expired Supabase Keys

**Symptom:**
```
E Realtime: WebSocket error: 401 Unauthorized
```

**Root Cause:**
- Using outdated Supabase anon key

**Solution:**
1. Get fresh key: `supabase projects api-keys --project-ref=<project-id>`
2. Update `NetworkModule.kt`:
   ```kotlin
   val supabaseKey = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9..." // ✅ Current key
   ```

---

### Mistake 6: Heartbeat Timeouts

**Symptom:**
```
E Realtime: Heartbeat timeout. Trying to reconnect in 7s
D Realtime: Closing websocket connection
```

**Root Cause:**
- Network instability
- Supabase free tier limitations
- Long-running operations blocking heartbeat thread

**Current Status:**
- This is a known issue with Supabase Realtime WebSocket
- Reconnection logic is working
- Broadcasts ARE received when connection is active
- **Not a blocker for functionality**

**Mitigation:**
- Ensure `isInitialized` flag prevents re-initialization
- Let reconnection logic handle automatically
- Consider upgrading Supabase plan for production

---

## Checklist

### Before Starting Integration

- [ ] Read this entire SOP document
- [ ] Understand event flow: Backend → Supabase → RealtimeManager → EventBus → ViewModel
- [ ] Verify Supabase project is active and healthy
- [ ] Verify Kotlin serialization plugin is configured

### Backend Changes

- [ ] Define event method in `eventBroadcaster.js` (if new event type)
- [ ] Import `eventBroadcaster` in API endpoint file
- [ ] Add broadcast call AFTER successful database operation
- [ ] Use descriptive action names (e.g., `item_added`, not `update`)
- [ ] Include all necessary data in payload
- [ ] Test backend locally
- [ ] Commit and push to trigger Vercel deployment
- [ ] Verify deployment successful

### Android Changes

- [ ] Add event class to `Event.kt` sealed class
- [ ] Create `@Serializable` payload data class if needed (NO `Any?` types!)
- [ ] Add subscription method to `RealtimeManager` if needed
- [ ] Add `channels.containsKey()` check to prevent duplicates
- [ ] Call `channel.subscribe()` BEFORE `broadcastFlow()`
- [ ] Use `@Serializable` data class in `broadcastFlow<Type>()`
- [ ] Add comprehensive logging
- [ ] Publish to EventBus after decoding
- [ ] Add EventBus subscription in relevant ViewModel
- [ ] Add logging in ViewModel event handler
- [ ] Call repository refresh method
- [ ] Build and test on device

### Testing

- [ ] Clear logs: `adb logcat -c`
- [ ] Install APK on TWO devices
- [ ] Login with same account on both devices
- [ ] Trigger event from Device A
- [ ] Verify logs show:
  - [ ] `RealtimeManager: Subscribed to channel`
  - [ ] `Realtime: Joined channel`
  - [ ] `Realtime: Received event broadcast`
  - [ ] `RealtimeManager: [Event] event received`
  - [ ] `[ViewModel]: [Event] event received`
  - [ ] `[ViewModel]: Refreshing... / Refreshed successfully`
- [ ] Verify Device B UI updates without manual refresh
- [ ] Test multiple times to ensure consistency
- [ ] Test with network interruption (airplane mode on/off)

---

## Troubleshooting Guide

### No broadcasts received

**Check:**
1. Is RealtimeManager initialized? Search logs for "Initializing RealtimeManager"
2. Did channel join successfully? Search logs for "Joined channel"
3. Is backend broadcasting? Check Vercel deployment logs
4. Is Supabase key valid? Check NetworkModule.kt
5. Are there duplicate subscriptions? Search logs for "phx_close"

### Serialization errors

**Check:**
1. Is `@Serializable` annotation present on data class?
2. Is Kotlin serialization plugin in build.gradle.kts?
3. Are all fields using explicit types (not `Any?`)?
4. Is `kotlinx-serialization-json` dependency added?

### Events received but UI not updating

**Check:**
1. Is ViewModel subscribing to EventBus? Search logs for "[ViewModel]: event received"
2. Is repository refresh being called? Add logging
3. Is cart Flow being collected in Fragment/Activity?
4. Are there coroutine scope issues?

### WebSocket constantly reconnecting

**Check:**
1. Is network stable?
2. Is Supabase project healthy? Check dashboard
3. Are there too many concurrent connections?
4. Consider: This is a known Supabase limitation on free tier

---

## Summary of Our Bug Fixes

### Issue Timeline

1. **Session 1: Initial Setup**
   - ✅ Created event-driven architecture
   - ✅ Added RealtimeManager and EventBus
   - ❌ Used `Map<String, Any?>` for payloads

2. **Session 2: Serialization Issues**
   - ❌ Got serialization error for `Any?` type
   - ✅ Created `@Serializable CartUpdatePayload` class
   - ❌ Forgot to add Kotlin serialization plugin
   - ✅ Added plugin and dependency

3. **Session 3: Duplicate Channels**
   - ❌ Channels closed immediately with `phx_close`
   - ✅ Added `channels.containsKey()` checks
   - ✅ Prevented duplicate subscriptions

4. **Session 4: Backend Not Broadcasting**
   - ❌ Backend created channels but never subscribed
   - ✅ Modified EventBroadcaster to auto-subscribe
   - ✅ Broadcasts now work!

5. **Session 5: Testing & Polish**
   - ✅ Added comprehensive logging
   - ✅ Verified end-to-end functionality
   - 🔄 Discovered missing broadcasts for quantity updates and cart clearing
   - ✅ Fixed remaining API endpoints to broadcast all events

6. **Session 6: Event Queue Attempt**
   - ✅ Implemented event queue with automatic retry mechanism
   - ✅ Added background worker to retry failed broadcasts every 5s
   - ❌ **Discovered Vercel limitation**: Serverless functions are stateless
   - ❌ `setInterval()` worker doesn't persist between function invocations
   - ❌ In-memory queue gets wiped when function instance shuts down
   - 📝 **Learning**: Event queue only works on traditional Node.js servers, not FaaS

### Current Status

**✅ Fully Working:**
- Add item to cart from product details page
- Quantity updates (+/- buttons) in cart
- Remove item from cart  
- Clear cart operation
- Real-time sync from Phone A → Phone B (when WebSocket connected)
- All events broadcast correctly from backend
- Android receives and processes all event types

**⚠️ Known Limitation:**
- **Missed events during WebSocket disconnections**
  - When Phone B's WebSocket disconnects, it misses broadcasts
  - Backend event queue doesn't work on Vercel (stateless)
  - Solution: User refreshes cart or navigates away/back
  - **Production fix needed**: Implement client-side retry or external queue service

---

## Best Practices

1. **Always use explicit types** - Never use `Any?` in serializable classes
2. **Check for duplicates** - Always verify channel doesn't exist before creating
3. **Subscribe before listening** - Call `channel.subscribe()` before `broadcastFlow()`
4. **Broadcast after DB success** - Only broadcast when operation completes successfully
5. **Add comprehensive logging** - Makes debugging 100x easier
6. **Test on multiple devices** - Real-time features need real-time testing
7. **Keep SOP updated** - Document new patterns and mistakes

---

## Contact

For questions or issues with this SOP:
- Update this document with new findings
- Document all mistakes and solutions
- Keep architecture diagram current

---

**End of Document**
