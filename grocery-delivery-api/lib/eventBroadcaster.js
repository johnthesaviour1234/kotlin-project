/**
 * Event Broadcaster Module
 * Handles real-time event broadcasting using Supabase Realtime channels
 * 
 * Phase 1 of Event-Driven Architecture Refactoring
 * Date: January 29, 2025
 */

import { createClient } from '@supabase/supabase-js'

const supabaseUrl = process.env.SUPABASE_URL
const supabaseServiceKey = process.env.SUPABASE_SERVICE_ROLE_KEY

if (!supabaseUrl || !supabaseServiceKey) {
  throw new Error('Missing Supabase configuration')
}

const supabase = createClient(supabaseUrl, supabaseServiceKey)

/**
 * EventBroadcaster class - Manages real-time event broadcasting
 */
class EventBroadcaster {
  constructor() {
    this.channels = new Map()
    this.eventQueue = new Map() // Map<channelName, Array<{event, payload, timestamp}>>
    this.retryInterval = 5000 // Retry every 5 seconds
    this.maxRetries = 12 // Retry for up to 1 minute (12 * 5s)
    this.startRetryWorker()
  }

  /**
   * Get or create a channel (and subscribe if needed)
   * @param {string} channelName - Name of the channel
   * @returns {object} Supabase channel instance
   */
  getChannel(channelName) {
    if (!this.channels.has(channelName)) {
      const channel = supabase.channel(channelName)
      // Subscribe to channel immediately so broadcasts can be sent
      channel.subscribe((status) => {
        if (status === 'SUBSCRIBED') {
          console.log(`[EventBroadcaster] Subscribed to channel: ${channelName}`)
        } else if (status === 'CHANNEL_ERROR') {
          console.error(`[EventBroadcaster] Error subscribing to channel: ${channelName}`)
        }
      })
      this.channels.set(channelName, channel)
    }
    return this.channels.get(channelName)
  }

  /**
   * Start background worker to retry failed broadcasts
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
    const now = Date.now()
    
    for (const [channelName, events] of this.eventQueue.entries()) {
      if (events.length === 0) continue
      
      console.log(`[EventBroadcaster] Processing ${events.length} queued events for ${channelName}`)
      
      // Try to send each queued event
      const remainingEvents = []
      
      for (const queuedEvent of events) {
        const age = now - queuedEvent.timestamp
        const retryCount = queuedEvent.retryCount || 0
        
        // Skip if too old or max retries exceeded
        if (retryCount >= this.maxRetries) {
          console.warn(`[EventBroadcaster] Dropping event ${queuedEvent.event} for ${channelName} (max retries exceeded)`)
          continue
        }
        
        try {
          const channel = this.getChannel(channelName)
          await channel.send({
            type: 'broadcast',
            event: queuedEvent.event,
            payload: queuedEvent.payload
          })
          console.log(`[EventBroadcaster] ✅ Retry successful: ${queuedEvent.event} to ${channelName}`)
          // Successfully sent, don't add back to queue
        } catch (error) {
          console.error(`[EventBroadcaster] ❌ Retry failed: ${queuedEvent.event} to ${channelName}:`, error.message)
          // Add back to queue with incremented retry count
          remainingEvents.push({
            ...queuedEvent,
            retryCount: retryCount + 1
          })
        }
      }
      
      // Update queue with remaining events
      if (remainingEvents.length > 0) {
        this.eventQueue.set(channelName, remainingEvents)
      } else {
        this.eventQueue.delete(channelName)
      }
    }
  }

  /**
   * Add event to queue for retry
   * @param {string} channelName - Channel name
   * @param {string} event - Event type  
   * @param {object} payload - Event payload
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
    
    console.log(`[EventBroadcaster] Queued ${event} for ${channelName} (queue size: ${this.eventQueue.get(channelName).length})`)
  }

  /**
   * Broadcast event to a specific channel
   * @param {string} channelName - Target channel name
   * @param {string} event - Event type
   * @param {object} payload - Event data
   */
  async broadcastToChannel(channelName, event, payload) {
    try {
      const channel = this.getChannel(channelName)
      
      const fullPayload = {
        ...payload,
        timestamp: new Date().toISOString()
      }
      
      await channel.send({
        type: 'broadcast',
        event,
        payload: fullPayload
      })
      console.log(`[EventBroadcaster] ✅ Broadcasted ${event} to ${channelName}`)
    } catch (error) {
      console.error(`[EventBroadcaster] ❌ Broadcast failed for ${event} to ${channelName}:`, error.message)
      // Queue the event for retry
      this.queueEvent(channelName, event, {
        ...payload,
        timestamp: new Date().toISOString()
      })
    }
  }

  /**
   * Broadcast order status change
   * Grocery delivery statuses: pending, confirmed, out_for_delivery, arrived, delivered, cancelled
   * @param {string} orderId - Order ID
   * @param {string} oldStatus - Previous status
   * @param {string} newStatus - New order status
   * @param {string} userId - Customer user ID
   * @param {string} deliveryPersonnelId - Driver ID (optional)
   */
  async orderStatusChanged(orderId, oldStatus, newStatus, userId, deliveryPersonnelId = null) {
    try {
      const payload = { 
        order_id: orderId, 
        old_status: oldStatus,
        new_status: newStatus
      }

      // Broadcast to multiple channels
      const broadcasts = [
        // All admins
        this.broadcastToChannel('admin:orders', 'order_status_changed', payload),
        
        // Specific customer
        this.broadcastToChannel(`user:${userId}`, 'order_status_changed', payload),
        
        // Order-specific channel
        this.broadcastToChannel(`order:${orderId}`, 'status_changed', payload)
      ]

      // If delivery personnel assigned, notify them too
      if (deliveryPersonnelId) {
        broadcasts.push(
          this.broadcastToChannel(`driver:${deliveryPersonnelId}`, 'order_status_changed', payload)
        )
      }

      await Promise.all(broadcasts)
      console.log(`[EventBroadcaster] Order ${orderId} status: ${oldStatus} → ${newStatus}`)
    } catch (error) {
      console.error('[EventBroadcaster] Error broadcasting order status change:', error)
    }
  }

  /**
   * Broadcast new order created
   * @param {string} orderId - Order ID
   * @param {string} orderNumber - Order number (e.g. ORD001026)
   * @param {string} userId - Customer user ID
   * @param {number} totalAmount - Total order amount
   * @param {object} orderData - Full order details
   */
  async orderCreated(orderId, orderNumber, userId, totalAmount, orderData = null) {
    try {
      const payload = {
        order_id: orderId,
        order_number: orderNumber,
        user_id: userId,
        total_amount: totalAmount,
        status: 'pending'
      }
      
      await Promise.all([
        // Notify all admins
        this.broadcastToChannel('admin:orders', 'new_order', payload),
        
        // Notify customer (for multi-device sync)
        this.broadcastToChannel(`user:${userId}`, 'order_created', payload)
      ])
      
      console.log(`[EventBroadcaster] New order created: ${orderNumber} (${orderId})`)
    } catch (error) {
      console.error('[EventBroadcaster] Error broadcasting new order:', error)
    }
  }

  /**
   * Broadcast order assignment to driver
   * @param {string} assignmentId - Assignment ID
   * @param {string} orderId - Order ID
   * @param {string} orderNumber - Order number
   * @param {string} deliveryPersonnelId - Driver ID
   * @param {string} userId - Customer ID
   * @param {object} orderDetails - Order details (address, items, etc.)
   */
  async orderAssigned(assignmentId, orderId, orderNumber, deliveryPersonnelId, userId, orderDetails = null) {
    try {
      const payload = { 
        assignment_id: assignmentId, 
        order_id: orderId,
        order_number: orderNumber,
        delivery_personnel_id: deliveryPersonnelId
      }

      await Promise.all([
        // Notify specific driver
        this.broadcastToChannel(`driver:${deliveryPersonnelId}`, 'order_assigned', payload),
        
        // Notify customer
        this.broadcastToChannel(`user:${userId}`, 'order_assigned', payload),
        
        // Notify order-specific channel
        this.broadcastToChannel(`order:${orderId}`, 'driver_assigned', payload),
        
        // Notify all admins
        this.broadcastToChannel('admin:orders', 'order_assigned', payload)
      ])
      
      console.log(`[EventBroadcaster] Order ${orderNumber} assigned to driver ${deliveryPersonnelId}`)
    } catch (error) {
      console.error('[EventBroadcaster] Error broadcasting order assignment:', error)
    }
  }

  /**
   * Broadcast product stock update
   * @param {string} productId - Product ID
   * @param {number} newStock - New stock level
   */
  async productStockChanged(productId, newStock) {
    try {
      await this.broadcastToChannel('products', 'stock_updated', {
        productId,
        stock: newStock
      })
      console.log(`[EventBroadcaster] Product ${productId} stock updated to ${newStock}`)
    } catch (error) {
      console.error('[EventBroadcaster] Error broadcasting stock update:', error)
    }
  }

  /**
   * Broadcast low stock alert
   * @param {string} productId - Product ID
   * @param {number} stock - Current stock level
   * @param {number} threshold - Low stock threshold
   */
  async lowStockAlert(productId, stock, threshold) {
    try {
      await this.broadcastToChannel('admin:inventory', 'low_stock_alert', {
        productId,
        stock,
        threshold
      })
      console.log(`[EventBroadcaster] Low stock alert for product ${productId}: ${stock}`)
    } catch (error) {
      console.error('[EventBroadcaster] Error broadcasting low stock alert:', error)
    }
  }

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

  /**
   * Broadcast driver location update
   * @param {string} driverId - Driver ID
   * @param {number} latitude - Latitude
   * @param {number} longitude - Longitude
   * @param {string} orderId - Order ID (optional)
   */
  async driverLocationUpdated(driverId, latitude, longitude, orderId = null) {
    try {
      const payload = { driverId, latitude, longitude }

      const broadcasts = [
        // Broadcast to driver's channel
        this.broadcastToChannel(`driver:${driverId}`, 'location_updated', payload)
      ]

      // If order ID provided, notify customer tracking that order
      if (orderId) {
        broadcasts.push(
          this.broadcastToChannel(`order:${orderId}:tracking`, 'driver_location', payload)
        )
      }

      await Promise.all(broadcasts)
    } catch (error) {
      console.error('[EventBroadcaster] Error broadcasting driver location:', error)
    }
  }

  /**
   * Broadcast delivery status update
   * @param {string} assignmentId - Assignment ID
   * @param {string} status - New status
   * @param {string} orderId - Order ID
   * @param {string} userId - Customer ID
   */
  async deliveryStatusUpdated(assignmentId, status, orderId, userId) {
    try {
      const payload = { assignmentId, status, orderId }

      await Promise.all([
        // Notify customer
        this.broadcastToChannel(`user:${userId}`, 'delivery_status_updated', payload),
        
        // Notify admins
        this.broadcastToChannel('admin:orders', 'delivery_status_updated', payload)
      ])
      
      console.log(`[EventBroadcaster] Delivery ${assignmentId} status updated to ${status}`)
    } catch (error) {
      console.error('[EventBroadcaster] Error broadcasting delivery status:', error)
    }
  }

  /**
   * Broadcast product created/updated/deleted
   * @param {string} action - Action type (created, updated, deleted)
   * @param {string} productId - Product ID
   * @param {object} productData - Product data (optional for delete)
   */
  async productChanged(action, productId, productData = null) {
    try {
      await this.broadcastToChannel('products', action, {
        productId,
        product: productData
      })
      console.log(`[EventBroadcaster] Product ${action}: ${productId}`)
    } catch (error) {
      console.error('[EventBroadcaster] Error broadcasting product change:', error)
    }
  }

  /**
   * Close all channels
   */
  async closeAll() {
    for (const [name, channel] of this.channels.entries()) {
      await channel.unsubscribe()
      console.log(`[EventBroadcaster] Closed channel: ${name}`)
    }
    this.channels.clear()
  }
}

// Create singleton instance
const eventBroadcaster = new EventBroadcaster()

// Export singleton and class
export default eventBroadcaster
export { EventBroadcaster }
