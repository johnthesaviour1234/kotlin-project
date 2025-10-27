import { withAdminAuth } from '../../../../lib/adminMiddleware.js'
import { supabase } from '../../../../lib/supabase.js'
import { formatSuccessResponse, formatErrorResponse } from '../../../../lib/validation.js'

async function handler(req, res) {
  if (req.method !== 'GET') {
    return res.status(405).json(formatErrorResponse('Method not allowed'))
  }

  try {
    const { id } = req.query

    if (!id) {
      return res.status(400).json(formatErrorResponse('Order ID is required'))
    }

    // Fetch order with all related data
    const { data: order, error } = await supabase
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
          product_image_url
        ),
        delivery_assignments(
          id,
          delivery_personnel_id,
          status,
          assigned_at,
          accepted_at,
          completed_at
        )
      `)
      .eq('id', id)
      .single()

    if (error) {
      if (error.code === 'PGRST116') {
        return res.status(404).json(formatErrorResponse('Order not found'))
      }
      console.error('Error fetching order:', error)
      return res.status(500).json(formatErrorResponse('Failed to fetch order'))
    }

    res.status(200).json(formatSuccessResponse(order, 'Order fetched successfully'))
  } catch (error) {
    console.error('Get order error:', error)
    res.status(500).json(formatErrorResponse('Internal server error'))
  }
}

export default withAdminAuth(handler)
