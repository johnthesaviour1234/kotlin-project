package com.grocery.customer.data.sync

import android.util.Log
import com.grocery.customer.domain.repository.CartRepository
import com.grocery.customer.domain.repository.OrderRepository
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.realtime.PostgresAction
import io.github.jan.supabase.realtime.RealtimeChannel
import io.github.jan.supabase.realtime.channel
import io.github.jan.supabase.realtime.postgresChangeFlow
import io.github.jan.supabase.realtime.realtime
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonPrimitive
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Realtime Manager for WebSocket-based synchronization
 * 
 * Manages real-time subscriptions to cart and order changes using Supabase Realtime.
 * Automatically refreshes local data when server events are received.
 * 
 * Topics subscribed to:
 * - cart:{user_id} - Cart item changes for current user
 * - orders:{user_id} - Order updates for current user
 * 
 * Events handled:
 * - CART_ITEM_ADDED
 * - CART_ITEM_UPDATED
 * - CART_ITEM_REMOVED
 * - ORDER_PLACED
 * - ORDER_CONFIRMED
 * - ORDER_ASSIGNED
 * - ORDER_STATUS_CHANGED
 * - DELIVERY_LOCATION_UPDATE
 */
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
    private var cartJob: Job? = null
    private var ordersJob: Job? = null
    private val scope = CoroutineScope(Dispatchers.IO)
    
    private val _connectionState = MutableStateFlow<RealtimeConnectionState>(
        RealtimeConnectionState.Disconnected
    )
    val connectionState: StateFlow<RealtimeConnectionState> = _connectionState
    
    /**
     * Subscribe to cart changes for the current user
     * Topic: cart:{user_id}
     * 
     * @param userId The authenticated user's ID
     */
    suspend fun subscribeToCartChanges(userId: String) {
        try {
            Log.d(TAG, "Subscribing to cart changes for user: $userId")
            
            // Unsubscribe from existing channel if any
            unsubscribeFromCart()
            
            // Create new channel for cart updates
            val channelId = "cart_changes_$userId"
            cartChannel = supabaseClient.realtime.channel(channelId)
            
            // Subscribe to Postgres changes on cart table
            cartJob = cartChannel!!.postgresChangeFlow<PostgresAction>(schema = "public") {
                table = "cart"
                filter = "user_id=eq.$userId"
            }.onEach { action ->
                when (action) {
                    is PostgresAction.Insert -> {
                        Log.d(TAG, "Cart item added")
                        handleCartChange()
                    }
                    is PostgresAction.Update -> {
                        Log.d(TAG, "Cart item updated")
                        handleCartChange()
                    }
                    is PostgresAction.Delete -> {
                        Log.d(TAG, "Cart item removed")
                        handleCartChange()
                    }
                    else -> {}
                }
            }.launchIn(scope)
            
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
     * 
     * @param userId The authenticated user's ID
     */
    suspend fun subscribeToOrderChanges(userId: String) {
        try {
            Log.d(TAG, "Subscribing to order changes for user: $userId")
            
            // Unsubscribe from existing channel if any
            unsubscribeFromOrders()
            
            // Create new channel for order updates
            val channelId = "order_changes_$userId"
            ordersChannel = supabaseClient.realtime.channel(channelId)
            
            // Subscribe to Postgres changes on orders table
            ordersJob = ordersChannel!!.postgresChangeFlow<PostgresAction>(schema = "public") {
                table = "orders"
                filter = "customer_id=eq.$userId"
            }.onEach { action ->
                when (action) {
                    is PostgresAction.Insert -> {
                        Log.d(TAG, "Order placed")
                        handleOrderChange()
                    }
                    is PostgresAction.Update -> {
                        Log.d(TAG, "Order updated (status or assignment change)")
                        handleOrderChange()
                    }
                    is PostgresAction.Delete -> {
                        Log.d(TAG, "Order deleted")
                        handleOrderChange()
                    }
                    else -> {}
                }
            }.launchIn(scope)
            
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
            cartJob?.cancel()
            cartChannel?.unsubscribe()
            cartChannel = null
            cartJob = null
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
            ordersJob?.cancel()
            ordersChannel?.unsubscribe()
            ordersChannel = null
            ordersJob = null
            Log.d(TAG, "Unsubscribed from order changes")
        } catch (e: Exception) {
            Log.e(TAG, "Error unsubscribing from orders", e)
        }
    }
    
    /**
     * Unsubscribe from all channels
     * Call this when user logs out or app goes to background
     */
    suspend fun unsubscribeAll() {
        unsubscribeFromCart()
        unsubscribeFromOrders()
        _connectionState.value = RealtimeConnectionState.Disconnected
        Log.d(TAG, "Unsubscribed from all channels")
    }
    
    /**
     * Handle cart change by refreshing cart from API
     */
    private suspend fun handleCartChange() {
        try {
            cartRepository.refreshCart()
            Log.d(TAG, "Cart refreshed after realtime update")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to refresh cart after realtime update", e)
        }
    }
    
    /**
     * Handle order change by refreshing orders from API
     */
    private suspend fun handleOrderChange() {
        try {
            orderRepository.refreshOrders()
            Log.d(TAG, "Orders refreshed after realtime update")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to refresh orders after realtime update", e)
        }
    }
    
    /**
     * Show notification when order is assigned to delivery person
     * TODO: Implement using Android NotificationManager
     */
    private fun showOrderAssignedNotification(payload: JsonObject) {
        // Extract order details from payload
        val orderId = payload["order_id"]?.jsonPrimitive?.content
        Log.d(TAG, "Order $orderId has been assigned to a delivery person")
        
        // TODO: Show Android notification
        // "Your order has been assigned to a delivery person"
    }
    
    /**
     * Show notification when order status changes
     * TODO: Implement using Android NotificationManager
     */
    private fun showOrderStatusNotification(payload: JsonObject) {
        // Extract status from payload
        val newStatus = payload["new_status"]?.jsonPrimitive?.content
        val orderId = payload["order_id"]?.jsonPrimitive?.content
        
        Log.d(TAG, "Order $orderId status changed to: $newStatus")
        
        // TODO: Show Android notification based on status
        // - "confirmed": "Your order has been confirmed"
        // - "out_for_delivery": "Your order is out for delivery"
        // - "delivered": "Your order has been delivered"
    }
}

/**
 * Connection state for Realtime WebSocket
 */
sealed class RealtimeConnectionState {
    object Disconnected : RealtimeConnectionState()
    object Connecting : RealtimeConnectionState()
    object Connected : RealtimeConnectionState()
    data class Error(val message: String) : RealtimeConnectionState()
}
