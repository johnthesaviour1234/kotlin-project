import { withAdminAuth } from '../../../../../lib/adminMiddleware.js'
import { supabase } from '../../../../../lib/supabase.js'
import { formatSuccessResponse, formatErrorResponse } from '../../../../../lib/validation.js'

async function handler(req, res) {
  if (req.method !== 'GET') {
    return res.status(405).json(formatErrorResponse('Method not allowed'))
  }

  try {
    const { id } = req.query

    if (!id) {
      return res.status(400).json(formatErrorResponse('Order ID is required'))
    }

    const { data, error } = await supabase
      .from('orders')
      .select(`
        *,
        order_items(
          id,
          product_id,
          product_name,
          quantity,
          unit_price,
          total_price,
          products(
            image_url,
            description
          )
        ),
        delivery_assignments(
          id,
          delivery_personnel_id,
          status,
          assigned_at
        )
      `)
      .eq('id', id)
      .single()

    if (error) {
      console.error('Error fetching order:', error)
      if (error.code === 'PGRST116') {
        return res.status(404).json(formatErrorResponse('Order not found'))
      }
      return res.status(500).json(formatErrorResponse('Failed to fetch order'))
    }

    // Normalize delivery_assignments to always be an array
    // and flatten products data into order_items
    const normalizedData = {
      ...data,
      delivery_assignments: data.delivery_assignments 
        ? (Array.isArray(data.delivery_assignments) 
            ? data.delivery_assignments 
            : [data.delivery_assignments])
        : [],
      order_items: data.order_items?.map(item => ({
        ...item,
        image_url: item.products?.image_url || null,
        description: item.products?.description || null,
        products: undefined  // Remove nested products object
      })) || []
    }

    res.status(200).json(formatSuccessResponse(normalizedData))
  } catch (error) {
    console.error('Get order error:', error)
    res.status(500).json(formatErrorResponse('Internal server error'))
  }
}

export default withAdminAuth(handler)