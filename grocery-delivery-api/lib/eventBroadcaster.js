/**
 * Event Broadcaster Module
 * Handles real-time event broadcasting using Supabase Realtime channels
 * 
 * Phase 1 of Event-Driven Architecture Refactoring
 * Date: January 29, 2025
 */

import { createClient } from '@supabase/supabase-js'

const supabaseUrl = process.env.NEXT_PUBLIC_SUPABASE_URL
const supabaseServiceKey = process.env.SUPABASE_SERVICE_KEY

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
  }

  /**
   * Get or create a channel
   * @param {string} channelName - Name of the channel
   * @returns {object} Supabase channel instance
   */
  getChannel(channelName) {
    if (!this.channels.has(channelName)) {
      const channel = supabase.channel(channelName)
      this.channels.set(channelName, channel)
    }
    return this.channels.get(channelName)
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
      await channel.send({
        type: 'broadcast',
        event,
        payload: {
          ...payload,
          timestamp: new Date().toISOString()
        }
      })
      console.log(`[EventBroadcaster] Broadcasted ${event} to ${channelName}`)
    } catch (error) {
      console.error(`[EventBroadcaster] Error broadcasting to ${channelName}:`, error)
      throw error
    }
  }

  /**
   * Broadcast order status change
   * @param {string} orderId - Order ID
   * @param {string} newStatus - New order status
   * @param {string} userId - Customer user ID
   * @param {string} deliveryPersonnelId - Driver ID (optional)
   */
  async orderStatusChanged(orderId, newStatus, userId, deliveryPersonnelId = null) {
    try {
      const payload = { orderId, status: newStatus }

      // Broadcast to multiple channels
      const broadcasts = [
        // All admins
        this.broadcastToChannel('admin:orders', 'status_changed', payload),
        
        // Specific customer
        this.broadcastToChannel(`user:${userId}`, 'order_updated', payload)
      ]

      // If delivery personnel assigned, notify them too
      if (deliveryPersonnelId) {
        broadcasts.push(
          this.broadcastToChannel('delivery:assignments', 'order_status_changed', payload)
        )
      }

      await Promise.all(broadcasts)
      console.log(`[EventBroadcaster] Order ${orderId} status changed to ${newStatus}`)
    } catch (error) {
      console.error('[EventBroadcaster] Error broadcasting order status change:', error)
    }
  }

  /**
   * Broadcast new order created
   * @param {string} orderId - Order ID
   * @param {object} orderData - Order details
   */
  async orderCreated(orderId, orderData) {
    try {
      await this.broadcastToChannel('admin:orders', 'new_order', {
        orderId,
        order: orderData
      })
      console.log(`[EventBroadcaster] New order created: ${orderId}`)
    } catch (error) {
      console.error('[EventBroadcaster] Error broadcasting new order:', error)
    }
  }

  /**
   * Broadcast order assignment to driver
   * @param {string} assignmentId - Assignment ID
   * @param {string} orderId - Order ID
   * @param {string} deliveryPersonnelId - Driver ID
   * @param {string} userId - Customer ID
   */
  async orderAssigned(assignmentId, orderId, deliveryPersonnelId, userId) {
    try {
      const payload = { assignmentId, orderId, deliveryPersonnelId }

      await Promise.all([
        // Notify driver
        this.broadcastToChannel(`driver:${deliveryPersonnelId}`, 'new_assignment', payload),
        
        // Notify customer
        this.broadcastToChannel(`user:${userId}`, 'order_assigned', payload),
        
        // Notify all delivery personnel
        this.broadcastToChannel('delivery:assignments', 'new_assignment', payload)
      ])
      
      console.log(`[EventBroadcaster] Order ${orderId} assigned to driver ${deliveryPersonnelId}`)
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
