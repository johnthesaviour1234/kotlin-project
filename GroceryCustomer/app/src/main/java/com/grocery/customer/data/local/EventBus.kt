package com.grocery.customer.data.local

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Local Event Bus for in-app real-time event communication
 * Uses Kotlin SharedFlow for reactive event streaming
 *
 * Phase 2 of Event-Driven Architecture Refactoring
 * Date: January 29, 2025
 */
@Singleton
class EventBus @Inject constructor() {

    // Internal mutable flow
    private val _events = MutableSharedFlow<Event>(
        replay = 0, // Don't replay past events
        extraBufferCapacity = 64 // Buffer for burst events
    )

    // Public immutable flow for subscribers
    val events: SharedFlow<Event> = _events.asSharedFlow()

    /**
     * Publish an event to all subscribers
     */
    suspend fun publish(event: Event) {
        _events.emit(event)
    }

    /**
     * Subscribe to specific event types
     * Usage: eventBus.subscribe<Event.OrderStatusChanged> { event -> ... }
     */
    inline fun <reified T : Event> subscribe(): Flow<T> {
        return events.filter { it is T }.map { it as T }
    }
}

/**
 * Base sealed class for all events
 * All event types must extend this class
 */
sealed class Event {

    /**
     * Order Events
     */
    data class OrderStatusChanged(
        val orderId: String,
        val newStatus: String,
        val timestamp: Long = System.currentTimeMillis()
    ) : Event()

    data class OrderCreated(
        val orderId: String,
        val timestamp: Long = System.currentTimeMillis()
    ) : Event()

    data class OrderAssigned(
        val orderId: String,
        val assignmentId: String,
        val deliveryPersonnelId: String,
        val timestamp: Long = System.currentTimeMillis()
    ) : Event()

    /**
     * Cart Events
     */
    // Generic cart updated event (refresh required)
    object CartUpdated : Event()

    // Specific cart events from realtime updates
    data class CartItemAdded(
        val productId: String,
        val quantity: Int,
        val timestamp: Long = System.currentTimeMillis()
    ) : Event()

    data class CartItemQuantityChanged(
        val itemId: String,
        val newQuantity: Int,
        val timestamp: Long = System.currentTimeMillis()
    ) : Event()

    data class CartItemRemoved(
        val itemId: String,
        val timestamp: Long = System.currentTimeMillis()
    ) : Event()

    object CartCleared : Event()

    // Legacy events for local actions
    data class ItemAddedToCart(
        val productId: String,
        val productName: String,
        val quantity: Int,
        val timestamp: Long = System.currentTimeMillis()
    ) : Event()

    data class ItemRemovedFromCart(
        val productId: String,
        val timestamp: Long = System.currentTimeMillis()
    ) : Event()

    /**
     * Product Events
     */
    data class ProductStockChanged(
        val productId: String,
        val newStock: Int,
        val timestamp: Long = System.currentTimeMillis()
    ) : Event()

    data class ProductPriceChanged(
        val productId: String,
        val newPrice: Double,
        val timestamp: Long = System.currentTimeMillis()
    ) : Event()

    data class ProductOutOfStock(
        val productId: String,
        val timestamp: Long = System.currentTimeMillis()
    ) : Event()

    data class ProductUpdated(
        val productId: String,
        val timestamp: Long = System.currentTimeMillis()
    ) : Event()

    data class ProductDeleted(
        val productId: String,
        val timestamp: Long = System.currentTimeMillis()
    ) : Event()

    /**
     * Delivery & Location Events
     */
    data class LocationUpdated(
        val driverId: String,
        val latitude: Double,
        val longitude: Double,
        val timestamp: Long = System.currentTimeMillis()
    ) : Event()

    data class DeliveryStatusUpdated(
        val assignmentId: String,
        val status: String,
        val orderId: String,
        val timestamp: Long = System.currentTimeMillis()
    ) : Event()

    /**
     * Inventory Events
     */
    data class LowStockAlert(
        val productId: String,
        val currentStock: Int,
        val threshold: Int,
        val timestamp: Long = System.currentTimeMillis()
    ) : Event()

    /**
     * Generic Events
     */
    data class ErrorOccurred(
        val message: String,
        val code: String? = null,
        val timestamp: Long = System.currentTimeMillis()
    ) : Event()

    data class NetworkStatusChanged(
        val isConnected: Boolean,
        val timestamp: Long = System.currentTimeMillis()
    ) : Event()

    data class RealtimeConnectionChanged(
        val isConnected: Boolean,
        val timestamp: Long = System.currentTimeMillis()
    ) : Event()
}
