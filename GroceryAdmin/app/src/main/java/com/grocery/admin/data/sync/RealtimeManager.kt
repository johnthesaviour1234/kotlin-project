package com.grocery.admin.data.sync

import android.util.Log
import com.grocery.admin.domain.repository.OrdersRepository
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.realtime.RealtimeChannel
import io.github.jan.supabase.realtime.channel
import io.github.jan.supabase.realtime.realtime
import io.github.jan.supabase.realtime.broadcastFlow
import kotlinx.serialization.json.JsonObject
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
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Realtime Manager for Admin App - WebSocket-based synchronization
 * 
 * Manages real-time subscriptions for admin operations.
 * Admin sees ALL orders and delivery assignments (not filtered by user).
 * 
 * Channels subscribed to:
 * - admin_orders_all - All order changes across the system
 * - admin_delivery_assignments - All delivery assignment changes
 * 
 * Events handled:
 * - ORDER_PLACED (new customer order)
 * - ORDER_STATUS_CHANGED (status updates)
 * - DELIVERY_ASSIGNED (driver assigned to order)
 * - DELIVERY_ACCEPTED/DECLINED (driver response)
 * - DELIVERY_STATUS_CHANGED (in_transit, arrived, completed)
 */
@Singleton
class RealtimeManager @Inject constructor(
    private val supabaseClient: SupabaseClient,
    private val ordersRepository: OrdersRepository
) {
    private companion object {
        const val TAG = "Admin_RealtimeManager"
    }
    
    private var ordersChannel: RealtimeChannel? = null
    private var deliveryAssignmentsChannel: RealtimeChannel? = null
    private var ordersJob: Job? = null
    private var deliveryAssignmentsJob: Job? = null
    private val scope = CoroutineScope(Dispatchers.IO)
    
    private val _connectionState = MutableStateFlow<RealtimeConnectionState>(
        RealtimeConnectionState.Disconnected
    )
    val connectionState: StateFlow<RealtimeConnectionState> = _connectionState
    
    // SharedFlow to broadcast when orders need to be refreshed
    private val _orderRefreshTrigger = MutableSharedFlow<Unit>(replay = 0)
    val orderRefreshTrigger: SharedFlow<Unit> = _orderRefreshTrigger.asSharedFlow()
    
    // SharedFlow to broadcast when delivery assignments need to be refreshed
    private val _assignmentRefreshTrigger = MutableSharedFlow<Unit>(replay = 0)
    val assignmentRefreshTrigger: SharedFlow<Unit> = _assignmentRefreshTrigger.asSharedFlow()
    
    /**
     * Subscribe to ALL order changes (admin sees everything)
     * Channel: admin_orders_all
     * Table: orders
     * Filter: NONE (admin sees all orders)
     */
    suspend fun subscribeToAllOrders() {
        try {
            Log.d(TAG, "Subscribing to ALL orders (admin view)")
            
            // Unsubscribe from existing channel if any
            unsubscribeFromOrders()
            
            // Create new channel for ALL orders (private channel for broadcast)
            val channelId = "admin_orders_all"
            ordersChannel = supabaseClient.realtime.channel(channelId) {
                // Mark as private channel for Realtime Authorization
            }
            
            // Subscribe to Broadcast messages for orders
            Log.d(TAG, "Setting up broadcastFlow for orders")
            ordersJob = ordersChannel!!.broadcastFlow<JsonObject>("INSERT")
                .onEach { message ->
                    Log.d(TAG, "🆕 New order broadcast received: $message")
                    handleOrderChange()
                }
                .launchIn(scope)
            
            // Also listen for UPDATE events
            ordersChannel!!.broadcastFlow<JsonObject>("UPDATE")
                .onEach { message ->
                    Log.d(TAG, "📝 Order update broadcast received: $message")
                    handleOrderChange()
                }
                .launchIn(scope)
            
            // Also listen for DELETE events
            ordersChannel!!.broadcastFlow<JsonObject>("DELETE")
                .onEach { message ->
                    Log.d(TAG, "🗑️ Order delete broadcast received: $message")
                    handleOrderChange()
                }
                .launchIn(scope)
            
            // Subscribe to the channel
            ordersChannel?.subscribe()
            
            _connectionState.value = RealtimeConnectionState.Connected
            Log.d(TAG, "✅ Successfully subscribed to all orders")
            
        } catch (e: Exception) {
            Log.e(TAG, "❌ Failed to subscribe to orders", e)
            _connectionState.value = RealtimeConnectionState.Error(e.message ?: "Unknown error")
        }
    }
    
    /**
     * Subscribe to ALL delivery assignments (admin sees everything)
     * Channel: admin_delivery_assignments
     * Table: delivery_assignments
     * Filter: NONE (admin sees all assignments)
     */
    suspend fun subscribeToDeliveryAssignments() {
        try {
            Log.d(TAG, "Subscribing to ALL delivery assignments (admin view)")
            
            // Unsubscribe from existing channel if any
            unsubscribeFromDeliveryAssignments()
            
            // Create new channel for ALL delivery assignments (private channel)
            val channelId = "admin_delivery_assignments"
            deliveryAssignmentsChannel = supabaseClient.realtime.channel(channelId) {
                // Mark as private channel for Realtime Authorization
            }
            
            // Subscribe to Broadcast messages for delivery assignments
            deliveryAssignmentsJob = deliveryAssignmentsChannel!!.broadcastFlow<JsonObject>("INSERT")
                .onEach { message ->
                    Log.d(TAG, "🚚 Assignment broadcast received: $message")
                    handleAssignmentChange()
                }
                .launchIn(scope)
            
            deliveryAssignmentsChannel!!.broadcastFlow<JsonObject>("UPDATE")
                .onEach { message ->
                    Log.d(TAG, "🔄 Assignment update broadcast received: $message")
                    handleAssignmentChange()
                }
                .launchIn(scope)
            
            deliveryAssignmentsChannel!!.broadcastFlow<JsonObject>("DELETE")
                .onEach { message ->
                    Log.d(TAG, "❌ Assignment delete broadcast received: $message")
                    handleAssignmentChange()
                }
                .launchIn(scope)
            
            // Subscribe to the channel
            deliveryAssignmentsChannel?.subscribe()
            
            Log.d(TAG, "✅ Successfully subscribed to delivery assignments")
            
        } catch (e: Exception) {
            Log.e(TAG, "❌ Failed to subscribe to delivery assignments", e)
        }
    }
    
    /**
     * Unsubscribe from orders channel
     */
    suspend fun unsubscribeFromOrders() {
        try {
            ordersJob?.cancel()
            ordersChannel?.unsubscribe()
            ordersChannel = null
            ordersJob = null
            Log.d(TAG, "Unsubscribed from orders")
        } catch (e: Exception) {
            Log.e(TAG, "Error unsubscribing from orders", e)
        }
    }
    
    /**
     * Unsubscribe from delivery assignments channel
     */
    suspend fun unsubscribeFromDeliveryAssignments() {
        try {
            deliveryAssignmentsJob?.cancel()
            deliveryAssignmentsChannel?.unsubscribe()
            deliveryAssignmentsChannel = null
            deliveryAssignmentsJob = null
            Log.d(TAG, "Unsubscribed from delivery assignments")
        } catch (e: Exception) {
            Log.e(TAG, "Error unsubscribing from delivery assignments", e)
        }
    }
    
    /**
     * Unsubscribe from all channels
     * Call this when admin logs out or app goes to background
     */
    suspend fun unsubscribeAll() {
        Log.d(TAG, "Unsubscribing from all realtime channels")
        unsubscribeFromOrders()
        unsubscribeFromDeliveryAssignments()
        _connectionState.value = RealtimeConnectionState.Disconnected
    }
    
    /**
     * Handle order change by triggering refresh
     * Emits event that ViewModels/Fragments can observe
     */
    private fun handleOrderChange() {
        Log.d(TAG, "📱 Order change detected - broadcasting refresh trigger")
        scope.launch {
            _orderRefreshTrigger.emit(Unit)
        }
    }
    
    /**
     * Handle delivery assignment change
     * Triggers refresh of orders and assignments
     */
    private fun handleAssignmentChange() {
        Log.d(TAG, "📱 Assignment change detected - broadcasting refresh trigger")
        scope.launch {
            _assignmentRefreshTrigger.emit(Unit)
            // Also refresh orders since assignments affect order display
            _orderRefreshTrigger.emit(Unit)
        }
    }
}
