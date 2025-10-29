import { supabase, supabaseClient, getAuthenticatedClient } from '../../../lib/supabase'
import { formatSuccessResponse, formatErrorResponse } from '../../../lib/validation'
import eventBroadcaster from '../../../lib/eventBroadcaster.js'

function getBearer(req) {
  const h = req.headers['authorization'] || req.headers['Authorization']
  if (!h || typeof h !== 'string') return null
  if (!h.toLowerCase().startsWith('bearer ')) return null
  return h.slice(7).trim()
}

export default async function handler(req, res) {
  const { productId } = req.query
  const token = getBearer(req)
  
  if (!token) {
    return res.status(401).json(formatErrorResponse('Authorization: Bearer token required'))
  }

  if (!productId || typeof productId !== 'string') {
    return res.status(400).json(formatErrorResponse('Product ID is required'))
  }

  try {
    const { data: userData, error: authError } = await supabaseClient.auth.getUser(token)
    if (authError || !userData?.user) {
      return res.status(401).json(formatErrorResponse('Invalid or expired token'))
    }
    const userId = userData.user.id

    if (req.method === 'PUT') {
      // Update cart item quantity by product ID
      const { quantity } = req.body || {}

      if (typeof quantity !== 'number' || quantity < 0) {
        return res.status(400).json(formatErrorResponse('Valid quantity is required'))
      }

      const client = getAuthenticatedClient(token)

      // Find the cart item by product_id
      const { data: cartItem, error: findError } = await client
        .from('cart')
        .select('id, quantity')
        .eq('user_id', userId)
        .eq('product_id', productId)
        .maybeSingle()

      if (findError) {
        return res.status(500).json(formatErrorResponse('Database error', [{ field: 'database', message: findError.message }]))
      }

      if (!cartItem) {
        return res.status(404).json(formatErrorResponse('Cart item not found'))
      }

      if (quantity === 0) {
        // Remove item if quantity is 0
        const { error } = await client
          .from('cart')
          .delete()
          .eq('id', cartItem.id)
          .eq('user_id', userId)

        if (error) {
          return res.status(500).json(formatErrorResponse('Failed to remove from cart', [{ field: 'database', message: error.message }]))
        }

        // ✅ Broadcast cart update event
        await eventBroadcaster.cartUpdated(userId, { action: 'item_removed', itemId: cartItem.id })

        return res.status(200).json(formatSuccessResponse({ message: 'Item removed from cart' }))
      } else {
        // Update quantity
        const { data, error } = await client
          .from('cart')
          .update({ 
            quantity,
            updated_at: new Date().toISOString()
          })
          .eq('id', cartItem.id)
          .eq('user_id', userId)
          .select(`
            id,
            product_id,
            quantity,
            price,
            created_at,
            updated_at,
            products:product_id (
              id,
              name,
              description,
              image_url,
              category_id,
              featured,
              is_active
            )
          `)
          .single()

        if (error) {
          return res.status(500).json(formatErrorResponse('Failed to update cart', [{ field: 'database', message: error.message }]))
        }

        // ✅ Broadcast cart update event
        await eventBroadcaster.cartUpdated(userId, { action: 'quantity_updated', itemId: cartItem.id, quantity })

        // Transform response to match expected format
        const enrichedItem = {
          id: data.id,
          product_id: data.product_id,
          product_name: data.products?.name || 'Unknown Product',
          product_description: data.products?.description || '',
          image_url: data.products?.image_url || '',
          quantity: data.quantity,
          price: data.price,
          total_price: data.quantity * data.price,
          created_at: data.created_at,
          updated_at: data.updated_at
        }

        return res.status(200).json(formatSuccessResponse(enrichedItem))
      }
    }

    if (req.method === 'DELETE') {
      // Remove cart item by product ID
      const client = getAuthenticatedClient(token)
      
      // Find and remove the cart item by product_id
      const { data: cartItem, error: findError } = await client
        .from('cart')
        .select('id')
        .eq('user_id', userId)
        .eq('product_id', productId)
        .maybeSingle()

      if (findError) {
        return res.status(500).json(formatErrorResponse('Database error', [{ field: 'database', message: findError.message }]))
      }

      if (!cartItem) {
        return res.status(404).json(formatErrorResponse('Cart item not found'))
      }

      const { error } = await client
        .from('cart')
        .delete()
        .eq('id', cartItem.id)
        .eq('user_id', userId)

      if (error) {
        return res.status(500).json(formatErrorResponse('Failed to remove from cart', [{ field: 'database', message: error.message }]))
      }

      // ✅ Broadcast cart update event
      await eventBroadcaster.cartUpdated(userId, { action: 'item_removed', itemId: cartItem.id })

      return res.status(200).json(formatSuccessResponse({ message: 'Item removed from cart' }))
    }

    return res.status(405).json(formatErrorResponse('Method not allowed'))
  } catch (e) {
    console.error('Cart operation error:', e)
    return res.status(500).json(formatErrorResponse('Internal server error'))
  }
}