package com.grocery.admin.data.remote

import android.util.Log
import com.grocery.admin.data.local.Event
import com.grocery.admin.data.local.EventBus
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.realtime.PostgresAction
import io.github.jan.supabase.realtime.RealtimeChannel
import io.github.jan.supabase.realtime.channel
import io.github.jan.supabase.realtime.postgresChangeFlow
import io.github.jan.supabase.realtime.realtime
import io.github.jan.supabase.realtime.broadcastFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Serializable data classes for Realtime broadcast events
 */
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
 * Supabase Realtime Manager for Admin App
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
     * Call this after admin logs in
     */
    fun initialize() {
        if (isInitialized) {
            Log.d(TAG, "Already initialized")
            return
        }

        Log.d(TAG, "Initializing RealtimeManager for Admin")
        isInitialized = true

        scope.launch {
            try {
                // Check if already connected, if so disconnect first
                try {
                    if (supabaseClient.realtime.status.value.name == "CONNECTED") {
                        Log.d(TAG, "WebSocket already connected, disconnecting first")
                        supabaseClient.realtime.disconnect()
                        kotlinx.coroutines.delay(500) // Wait for disconnect
                    }
                } catch (e: Exception) {
                    Log.w(TAG, "Error checking/disconnecting WebSocket", e)
                }
                
                // Connect to Realtime once at the client level
                supabaseClient.realtime.connect()
                Log.d(TAG, "Realtime WebSocket connected")

                // Subscribe to admin order events
                subscribeToAdminOrders()

                // Subscribe to product events
                subscribeToProductEvents()

                // Publish connection status
                eventBus.publish(Event.RealtimeConnectionChanged(isConnected = true))
            } catch (e: Exception) {
                Log.e(TAG, "Error connecting to Realtime", e)
                isInitialized = false
            }
        }
    }

    /**
     * Subscribe to all admin order events
     * This includes new orders, status changes, assignments
     */
    private fun subscribeToAdminOrders() {
        scope.launch {
            try {
                val channelName = "admin:orders"
                if (channels.containsKey(channelName)) {
                    Log.d(TAG, "Already subscribed to $channelName")
                    return@launch
                }
                
                val channel = supabaseClient.realtime.channel(channelName)

                // Listen to Broadcast events for order creation (bypasses RLS)
                // Backend sends "new_order" event to "admin:orders" channel
                channel.broadcastFlow<OrderCreatedPayload>("new_order")
                    .onEach { payload ->
                        try {
                            Log.d(TAG, "New order from broadcast: order_id=${payload.order_id}, order_number=${payload.order_number}, status=${payload.status}")
                            eventBus.publish(Event.OrderCreated(payload.order_id))
                        } catch (e: Exception) {
                            Log.e(TAG, "Error processing new_order broadcast", e)
                        }
                    }
                    .launchIn(scope)

                // Listen to Broadcast events for order status changes (bypasses RLS)
                channel.broadcastFlow<OrderStatusPayload>("order_status_changed")
                    .onEach { payload ->
                        try {
                            Log.d(TAG, "Order status changed from broadcast: order_id=${payload.order_id}, old=${payload.old_status}, new=${payload.new_status}")
                            eventBus.publish(Event.OrderStatusChanged(payload.order_id, payload.new_status))
                        } catch (e: Exception) {
                            Log.e(TAG, "Error processing order_status_changed broadcast", e)
                        }
                    }
                    .launchIn(scope)

                // Subscribe to channel AFTER setting up listeners
                channel.subscribe(blockUntilSubscribed = false)
                channels[channelName] = channel
                Log.d(TAG, "Subscribed to $channelName with Broadcast events")
            } catch (e: Exception) {
                Log.e(TAG, "Error subscribing to admin orders", e)
            }
        }
    }

    /**
     * Subscribe to product stock changes
     * This allows real-time stock updates across all products
     */
    private fun subscribeToProductEvents() {
        scope.launch {
            try {
                val channelName = "products"
                if (channels.containsKey(channelName)) {
                    Log.d(TAG, "Already subscribed to $channelName")
                    return@launch
                }
                val channel = supabaseClient.realtime.channel(channelName)

                // Set up broadcastFlow listeners BEFORE subscribing
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

                // Subscribe to channel AFTER setting up listeners
                channel.subscribe(blockUntilSubscribed = false)
                channels[channelName] = channel
                Log.d(TAG, "Subscribed to $channelName")
            } catch (e: Exception) {
                Log.e(TAG, "Error subscribing to product events", e)
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
