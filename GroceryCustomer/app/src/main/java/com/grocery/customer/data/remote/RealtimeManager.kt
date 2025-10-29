package com.grocery.customer.data.remote

import android.util.Log
import com.grocery.customer.data.local.Event
import com.grocery.customer.data.local.EventBus
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.realtime.PostgresAction
import io.github.jan.supabase.realtime.Realtime
import io.github.jan.supabase.realtime.RealtimeChannel
import io.github.jan.supabase.realtime.channel
import io.github.jan.supabase.realtime.realtime
import io.github.jan.supabase.realtime.broadcastFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.Serializable
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Serializable data classes for Realtime broadcast events
 */
@Serializable
data class CartUpdatePayload(
    val action: String,
    val productId: String? = null,
    val itemId: String? = null,
    val quantity: Int? = null,
    val timestamp: String? = null
)

@Serializable
data class OrderStatusPayload(
    val order_id: String,
    val old_status: String? = null,
    val new_status: String,
    val timestamp: String? = null
)

@Serializable
data class OrderCreatedPayload(
    val order_id: String,
    val order_number: String,
    val user_id: String,
    val total_amount: Double,
    val status: String,
    val timestamp: String? = null
)

@Serializable
data class OrderAssignedPayload(
    val assignment_id: String,
    val order_id: String,
    val order_number: String,
    val delivery_personnel_id: String,
    val timestamp: String? = null
)

/**
 * Supabase Realtime Manager
 * Handles real-time channel subscriptions and event publishing to local EventBus
 *
 * Phase 2 of Event-Driven Architecture Refactoring
 * Date: January 29, 2025
 */
@Singleton
class RealtimeManager @Inject constructor(
    private val supabaseClient: SupabaseClient,
    private val eventBus: EventBus
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val TAG = "RealtimeManager"

    private val channels = mutableMapOf<String, RealtimeChannel>()
    private var isInitialized = false

    /**
     * Initialize realtime connection
     * Call this after user logs in
     */
    fun initialize(userId: String) {
        if (isInitialized) {
            Log.d(TAG, "Already initialized")
            return
        }

        Log.d(TAG, "Initializing RealtimeManager for user: $userId")
        isInitialized = true

        // Subscribe to user-specific events
        subscribeToUserEvents(userId)

        // Subscribe to product stock changes
        subscribeToProductStock()

        // Subscribe to cart updates
        subscribeToCartUpdates(userId)

        // Publish connection status
        scope.launch {
            eventBus.publish(Event.RealtimeConnectionChanged(isConnected = true))
        }
    }

    /**
     * Subscribe to order updates for a specific order
     * Call this when viewing order details
     */
    fun subscribeToOrder(orderId: String) {
        scope.launch {
            try {
                val channelName = "order:$orderId"
                if (channels.containsKey(channelName)) {
                    Log.d(TAG, "Already subscribed to $channelName")
                    return@launch
                }

                val channel = supabaseClient.realtime.channel(channelName)

                // Subscribe to channel FIRST
                channel.subscribe()
                channels[channelName] = channel
                Log.d(TAG, "Subscribed to $channelName")

                // THEN listen for status changes
                channel.broadcastFlow<OrderStatusPayload>("status_changed")
                    .onEach { payload ->
                        Log.d(TAG, "Order status changed: order_id=${payload.order_id}, old=${payload.old_status}, new=${payload.new_status}")
                        eventBus.publish(Event.OrderStatusChanged(payload.order_id, payload.new_status))
                    }
                    .launchIn(scope)

                // Listen for driver assignments
                channel.broadcastFlow<OrderAssignedPayload>("driver_assigned")
                    .onEach { payload ->
                        Log.d(TAG, "Driver assigned to order: order_id=${payload.order_id}, driver=${payload.delivery_personnel_id}")
                        eventBus.publish(
                            Event.OrderAssigned(
                                payload.order_id,
                                payload.assignment_id,
                                payload.delivery_personnel_id
                            )
                        )
                    }
                    .launchIn(scope)
            } catch (e: Exception) {
                Log.e(TAG, "Error subscribing to order $orderId", e)
            }
        }
    }

    /**
     * Subscribe to user-specific events (cart, orders)
     * This includes all events targeted at this user
     */
    private fun subscribeToUserEvents(userId: String) {
        scope.launch {
            try {
                val channelName = "user:$userId"
                if (channels.containsKey(channelName)) {
                    Log.d(TAG, "Already subscribed to $channelName")
                    return@launch
                }
                val channel = supabaseClient.realtime.channel(channelName)

                // Subscribe to channel FIRST
                channel.subscribe()
                channels[channelName] = channel
                Log.d(TAG, "Subscribed to $channelName")

                // THEN listen for order status changes
                channel.broadcastFlow<OrderStatusPayload>("order_status_changed")
                    .onEach { payload ->
                        Log.d(TAG, "Order status changed: order_id=${payload.order_id}, old=${payload.old_status}, new=${payload.new_status}")
                        eventBus.publish(Event.OrderStatusChanged(payload.order_id, payload.new_status))
                    }
                    .launchIn(scope)

                // Listen for new orders created
                channel.broadcastFlow<OrderCreatedPayload>("order_created")
                    .onEach { payload ->
                        Log.d(TAG, "Order created: order_id=${payload.order_id}, order_number=${payload.order_number}, total=${payload.total_amount}")
                        eventBus.publish(Event.OrderCreated(payload.order_id))
                    }
                    .launchIn(scope)

                // Listen for order assignments
                channel.broadcastFlow<OrderAssignedPayload>("order_assigned")
                    .onEach { payload ->
                        Log.d(TAG, "Order assigned: order_id=${payload.order_id}, driver=${payload.delivery_personnel_id}")
                        eventBus.publish(
                            Event.OrderAssigned(
                                payload.order_id,
                                payload.assignment_id,
                                payload.delivery_personnel_id
                            )
                        )
                    }
                    .launchIn(scope)
            } catch (e: Exception) {
                Log.e(TAG, "Error subscribing to user events", e)
            }
        }
    }

    /**
     * Subscribe to product stock changes
     * This allows real-time stock updates across all products
     */
    private fun subscribeToProductStock() {
        scope.launch {
            try {
                val channelName = "products"
                if (channels.containsKey(channelName)) {
                    Log.d(TAG, "Already subscribed to $channelName")
                    return@launch
                }
                val channel = supabaseClient.realtime.channel(channelName)

                // Listen for stock updates
                channel.broadcastFlow<Map<String, Any?>>("stock_updated")
                    .onEach { payload ->
                        Log.d(TAG, "Stock updated: $payload")

                        val productId = payload["productId"] as? String
                        val stock = (payload["stock"] as? Number)?.toInt()

                        if (productId != null && stock != null) {
                            eventBus.publish(Event.ProductStockChanged(productId, stock))

                            // Check for out of stock
                            if (stock == 0) {
                                eventBus.publish(Event.ProductOutOfStock(productId))
                            }
                        }
                    }
                    .launchIn(scope)

                // Listen for product updates
                channel.broadcastFlow<Map<String, Any?>>("updated")
                    .onEach { payload ->
                        Log.d(TAG, "Product updated: $payload")

                        val productId = payload["productId"] as? String
                        if (productId != null) {
                            eventBus.publish(Event.ProductUpdated(productId))
                        }
                    }
                    .launchIn(scope)

                // Listen for product deletions
                channel.broadcastFlow<Map<String, Any?>>("deleted")
                    .onEach { payload ->
                        Log.d(TAG, "Product deleted: $payload")

                        val productId = payload["productId"] as? String
                        if (productId != null) {
                            eventBus.publish(Event.ProductDeleted(productId))
                        }
                    }
                    .launchIn(scope)

                channel.subscribe()
                channels[channelName] = channel
                Log.d(TAG, "Subscribed to $channelName")
            } catch (e: Exception) {
                Log.e(TAG, "Error subscribing to product stock", e)
            }
        }
    }

    /**
     * Subscribe to cart updates for the user
     * This enables real-time cart synchronization across devices
     */
    private fun subscribeToCartUpdates(userId: String) {
        scope.launch {
            try {
                val channelName = "cart:$userId"
                if (channels.containsKey(channelName)) {
                    Log.d(TAG, "Already subscribed to $channelName")
                    return@launch
                }
                val channel = supabaseClient.realtime.channel(channelName)

                // Subscribe to channel FIRST
                channel.subscribe()
                channels[channelName] = channel
                Log.d(TAG, "Subscribed to $channelName")

                // THEN listen for cart update events
                channel.broadcastFlow<CartUpdatePayload>("updated")
                    .onEach { payload ->
                        Log.d(TAG, "Cart updated event received: action=${payload.action}, productId=${payload.productId}, itemId=${payload.itemId}, quantity=${payload.quantity}")

                        when (payload.action) {
                            "item_added" -> {
                                Log.d(TAG, "Cart item added: productId=${payload.productId}, quantity=${payload.quantity}")
                                if (payload.productId != null && payload.quantity != null) {
                                    eventBus.publish(Event.CartItemAdded(payload.productId, payload.quantity))
                                }
                            }
                            "quantity_updated" -> {
                                Log.d(TAG, "Cart quantity updated: itemId=${payload.itemId}, quantity=${payload.quantity}")
                                if (payload.itemId != null && payload.quantity != null) {
                                    eventBus.publish(Event.CartItemQuantityChanged(payload.itemId, payload.quantity))
                                }
                            }
                            "item_removed" -> {
                                Log.d(TAG, "Cart item removed: itemId=${payload.itemId}")
                                if (payload.itemId != null) {
                                    eventBus.publish(Event.CartItemRemoved(payload.itemId))
                                }
                            }
                            "cart_cleared" -> {
                                Log.d(TAG, "Cart cleared")
                                eventBus.publish(Event.CartCleared)
                            }
                            else -> {
                                // Generic cart updated event
                                Log.d(TAG, "Generic cart update, refreshing cart")
                                eventBus.publish(Event.CartUpdated)
                            }
                        }
                    }
                    .launchIn(scope)
            } catch (e: Exception) {
                Log.e(TAG, "Error subscribing to cart updates", e)
            }
        }
    }

    /**
     * Subscribe to delivery driver location for tracking
     * Call this when viewing order tracking with assigned driver
     */
    fun subscribeToDriverLocation(orderId: String, driverId: String) {
        scope.launch {
            try {
                val channelName = "order:${orderId}:tracking"
                if (channels.containsKey(channelName)) {
                    Log.d(TAG, "Already subscribed to $channelName")
                    return@launch
                }

                val channel = supabaseClient.realtime.channel(channelName)

                // Listen for driver location updates
                channel.broadcastFlow<Map<String, Any?>>("driver_location")
                    .onEach { payload ->
                        Log.d(TAG, "Driver location updated: $payload")

                        val latitude = (payload["latitude"] as? Number)?.toDouble()
                        val longitude = (payload["longitude"] as? Number)?.toDouble()

                        if (latitude != null && longitude != null) {
                            eventBus.publish(Event.LocationUpdated(driverId, latitude, longitude))
                        }
                    }
                    .launchIn(scope)

                channel.subscribe()
                channels[channelName] = channel
                Log.d(TAG, "Subscribed to $channelName")
            } catch (e: Exception) {
                Log.e(TAG, "Error subscribing to driver location", e)
            }
        }
    }

    /**
     * Unsubscribe from a specific channel
     */
    fun unsubscribe(channelName: String) {
        scope.launch {
            try {
                val channel = channels[channelName]
                if (channel != null) {
                    channel.unsubscribe()
                    channels.remove(channelName)
                    Log.d(TAG, "Unsubscribed from $channelName")
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error unsubscribing from $channelName", e)
            }
        }
    }

    /**
     * Unsubscribe from all channels
     * Call this on logout
     */
    fun unsubscribeAll() {
        scope.launch {
            try {
                channels.keys.toList().forEach { channelName ->
                    unsubscribe(channelName)
                }
                isInitialized = false
                eventBus.publish(Event.RealtimeConnectionChanged(isConnected = false))
                Log.d(TAG, "Unsubscribed from all channels")
            } catch (e: Exception) {
                Log.e(TAG, "Error unsubscribing from all channels", e)
            }
        }
    }

    /**
     * Check if manager is initialized
     */
    fun isInitialized(): Boolean = isInitialized
}
