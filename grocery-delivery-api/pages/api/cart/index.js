import { supabase, supabaseClient, getAuthenticatedClient } from '../../../lib/supabase'
import { formatSuccessResponse, formatErrorResponse } from '../../../lib/validation'

function getBearer(req) {
  const h = req.headers['authorization'] || req.headers['Authorization']
  if (!h || typeof h !== 'string') return null
  if (!h.toLowerCase().startsWith('bearer ')) return null
  return h.slice(7).trim()
}

export default async function handler(req, res) {
  const token = getBearer(req)
  if (!token) {
    return res.status(401).json(formatErrorResponse('Authorization: Bearer token required'))
  }

  try {
    const { data: userData, error: authError } = await supabaseClient.auth.getUser(token)
    if (authError || !userData?.user) {
      return res.status(401).json(formatErrorResponse('Invalid or expired token'))
    }
    const userId = userData.user.id

    if (req.method === 'GET') {
      // Get user's cart
      const client = getAuthenticatedClient(token)
      const { data, error } = await client
        .from('cart')
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
        .eq('user_id', userId)
        .order('created_at', { ascending: false })

      if (error) {
        return res.status(500).json(formatErrorResponse('Database error', [{ field: 'database', message: error.message }]))
      }

      // Transform response to include product details
      const enrichedItems = data?.map(item => ({
        id: item.id,
        product_id: item.product_id,
        product_name: item.products?.name || 'Unknown Product',
        product_description: item.products?.description || '',
        image_url: item.products?.image_url || '',
        quantity: item.quantity,
        price: item.price,
        total_price: item.quantity * item.price,
        created_at: item.created_at,
        updated_at: item.updated_at
      })) || []

      return res.status(200).json(formatSuccessResponse({ 
        items: enrichedItems,
        total_items: enrichedItems.reduce((sum, item) => sum + item.quantity, 0),
        total_price: enrichedItems.reduce((sum, item) => sum + item.total_price, 0)
      }))
    }

    if (req.method === 'POST') {
      // Add item to cart
      const { product_id, quantity = 1, price } = req.body || {}

      if (!product_id || typeof product_id !== 'string') {
        return res.status(400).json(formatErrorResponse('Product ID is required'))
      }
      if (typeof quantity !== 'number' || quantity < 1) {
        return res.status(400).json(formatErrorResponse('Valid quantity is required'))
      }
      if (typeof price !== 'number' || price < 0) {
        return res.status(400).json(formatErrorResponse('Valid price is required'))
      }

      const client = getAuthenticatedClient(token)
      
      // Check if item already exists in cart
      const { data: existingItem, error: checkError } = await client
        .from('cart')
        .select('id, quantity')
        .eq('user_id', userId)
        .eq('product_id', product_id)
        .maybeSingle()

      if (checkError) {
        return res.status(500).json(formatErrorResponse('Database error', [{ field: 'database', message: checkError.message }]))
      }

      let result
      if (existingItem) {
        // Update existing item quantity
        result = await client
          .from('cart')
          .update({ 
            quantity: existingItem.quantity + quantity,
            updated_at: new Date().toISOString()
          })
          .eq('id', existingItem.id)
          .select()
          .single()
      } else {
        // Add new item
        result = await client
          .from('cart')
          .insert({
            user_id: userId,
            product_id,
            quantity,
            price
          })
          .select()
          .single()
      }

      if (result.error) {
        return res.status(500).json(formatErrorResponse('Failed to add to cart', [{ field: 'database', message: result.error.message }]))
      }

      return res.status(200).json(formatSuccessResponse(result.data))
    }

    if (req.method === 'PUT') {
      // Update cart item quantity
      const { id, quantity } = req.body || {}

      if (!id || typeof id !== 'string') {
        return res.status(400).json(formatErrorResponse('Cart item ID is required'))
      }
      if (typeof quantity !== 'number' || quantity < 0) {
        return res.status(400).json(formatErrorResponse('Valid quantity is required'))
      }

      const client = getAuthenticatedClient(token)

      if (quantity === 0) {
        // Remove item if quantity is 0
        const { error } = await client
          .from('cart')
          .delete()
          .eq('id', id)
          .eq('user_id', userId)

        if (error) {
          return res.status(500).json(formatErrorResponse('Failed to remove from cart', [{ field: 'database', message: error.message }]))
        }

        return res.status(200).json(formatSuccessResponse({ message: 'Item removed from cart' }))
      } else {
        // Update quantity
        const { data, error } = await client
          .from('cart')
          .update({ 
            quantity,
            updated_at: new Date().toISOString()
          })
          .eq('id', id)
          .eq('user_id', userId)
          .select()
          .single()

        if (error) {
          return res.status(500).json(formatErrorResponse('Failed to update cart', [{ field: 'database', message: error.message }]))
        }

        return res.status(200).json(formatSuccessResponse(data))
      }
    }

    if (req.method === 'DELETE') {
      // Clear entire cart
      const client = getAuthenticatedClient(token)
      const { error } = await client
        .from('cart')
        .delete()
        .eq('user_id', userId)

      if (error) {
        return res.status(500).json(formatErrorResponse('Failed to clear cart', [{ field: 'database', message: error.message }]))
      }

      return res.status(200).json(formatSuccessResponse({ message: 'Cart cleared successfully' }))
    }

    return res.status(405).json(formatErrorResponse('Method not allowed'))
  } catch (e) {
    return res.status(500).json(formatErrorResponse('Internal server error'))
  }
}