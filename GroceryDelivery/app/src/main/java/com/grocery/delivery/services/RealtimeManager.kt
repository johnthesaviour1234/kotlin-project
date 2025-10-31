package com.grocery.delivery.services

import android.util.Log
import com.grocery.delivery.data.local.PreferencesManager
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.realtime.PostgresAction
import io.github.jan.supabase.realtime.RealtimeChannel
import io.github.jan.supabase.realtime.channel
import io.github.jan.supabase.realtime.realtime
import io.github.jan.supabase.realtime.postgresChangeFlow
import io.github.jan.supabase.realtime.broadcastFlow
import io.github.jan.supabase.postgrest.query.filter.FilterOperator
import kotlinx.serialization.json.JsonObject
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Realtime Manager for Delivery App - WebSocket-based synchronization
 * 
 * Manages real-time subscriptions for delivery driver operations.
 * Driver sees orders assigned to them.
 * 
 * Events handled:
 * - ORDER_ASSIGNED (driver assigned to order)
 * - ORDER_STATUS_CHANGED (status updates)
 */
@Singleton
class RealtimeManager @Inject constructor(
    private val supabaseClient: SupabaseClient,
    private val preferencesManager: PreferencesManager
) {
    private companion object {
        const val TAG = "Delivery_RealtimeManager"
    }
    
    private var ordersChannel: RealtimeChannel? = null
    private var ordersJob: Job? = null
    private val scope = CoroutineScope(Dispatchers.IO)
    
    private val _connectionState = MutableStateFlow<ConnectionState>(
        ConnectionState.Disconnected
    )
    val connectionState: StateFlow<ConnectionState> = _connectionState
    
    // SharedFlow to broadcast when orders need to be refreshed
    private val _orderRefreshTrigger = MutableSharedFlow<Unit>(replay = 0)
    val orderRefreshTrigger: SharedFlow<Unit> = _orderRefreshTrigger.asSharedFlow()
    
    /**
     * Subscribe to order changes for this driver
     * Channel: delivery_orders_{driver_id}
     */
    suspend fun connect() {
        try {
            val driverId = preferencesManager.getUserId()
            if (driverId.isNullOrEmpty()) {
                Log.w(TAG, "Cannot connect: User ID not found")
                _connectionState.value = ConnectionState.Error("User not authenticated")
                return
            }
            
            Log.d(TAG, "Subscribing to orders for driver: $driverId")
            
            // Unsubscribe from existing channel if any
            disconnect()
            
            // Create new channel for driver's orders
            // Use driver-specific channel that matches the database trigger
            val channelId = "delivery_orders_$driverId"
            ordersChannel = supabaseClient.realtime.channel(channelId) {
                // Mark as private channel for Realtime Authorization
            }
            
            // Subscribe to broadcast messages on driver-specific channel
            // Listen for INSERT events (new assignments)
            Log.d(TAG, "Setting up broadcastFlow for delivery orders")
            ordersJob = ordersChannel!!.broadcastFlow<JsonObject>("INSERT")
                .onEach { message ->
                    Log.d(TAG, "🆕 New delivery assignment broadcast received")
                    handleOrderChange()
                }
                .launchIn(scope)
            
            // Also listen for UPDATE events
            ordersChannel!!.broadcastFlow<JsonObject>("UPDATE")
                .onEach { message ->
                    Log.d(TAG, "📝 Delivery assignment update broadcast received")
                    handleOrderChange()
                }
                .launchIn(scope)
            
            // Also listen for DELETE events
            ordersChannel!!.broadcastFlow<JsonObject>("DELETE")
                .onEach { message ->
                    Log.d(TAG, "🗑️ Delivery assignment delete broadcast received")
                    handleOrderChange()
                }
                .launchIn(scope)
            
            // Subscribe to the channel
            ordersChannel?.subscribe()
            
            _connectionState.value = ConnectionState.Connected
            Log.d(TAG, "✅ Successfully subscribed to orders")
            
        } catch (e: Exception) {
            Log.e(TAG, "❌ Failed to subscribe to orders", e)
            _connectionState.value = ConnectionState.Error(e.message ?: "Unknown error")
        }
    }
    
    /**
     * Disconnect from real-time channels.
     */
    suspend fun disconnect() {
        try {
            ordersJob?.cancel()
            ordersChannel?.unsubscribe()
            ordersChannel = null
            ordersJob = null
            _connectionState.value = ConnectionState.Disconnected
            Log.d(TAG, "Unsubscribed from orders")
        } catch (e: Exception) {
            Log.e(TAG, "Error unsubscribing from orders", e)
        }
    }
    
    /**
     * Check if currently connected to realtime.
     */
    fun isConnected(): Boolean {
        return ordersChannel != null
    }
    
    /**
     * Handle order change by triggering refresh
     * Emits event that ViewModels can observe
     */
    private fun handleOrderChange() {
        Log.d(TAG, "📱 Order change detected - broadcasting refresh trigger")
        scope.launch {
            _orderRefreshTrigger.emit(Unit)
        }
    }
}

/**
 * Connection state for realtime subscriptions.
 */
sealed class ConnectionState {
    object Connecting : ConnectionState()
    object Connected : ConnectionState()
    object Disconnected : ConnectionState()
    data class Error(val message: String) : ConnectionState()
}
