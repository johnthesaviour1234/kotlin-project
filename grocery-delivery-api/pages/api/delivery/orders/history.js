import { withAdminAuth } from '../../../../lib/adminMiddleware.js'
import { supabase } from '../../../../lib/supabase.js'
import { formatSuccessResponse, formatErrorResponse } from '../../../../lib/validation.js'

async function handler(req, res) {
  if (req.method !== 'GET') {
    return res.status(405).json(formatErrorResponse('Method not allowed'))
  }

  try {
    const { limit = 50, offset = 0 } = req.query
    
    // Get completed deliveries for this driver
    const { data, error, count } = await supabase
      .from('delivery_assignments')
      .select(`
        id,
        order_id,
        status,
        assigned_at,
        accepted_at,
        estimated_delivery_minutes,
        notes,
        created_at,
        updated_at,
        orders(
          id,
          order_number,
          total_amount,
          delivery_address,
          customer_info,
          notes,
          delivered_at,
          created_at
        )
      `, { count: 'exact' })
      .eq('delivery_personnel_id', req.user.id)
      .eq('status', 'completed')
      .order('updated_at', { ascending: false })
      .range(parseInt(offset), parseInt(offset) + parseInt(limit) - 1)

    if (error) {
      console.error('Error fetching delivery history:', error)
      return res.status(500).json(formatErrorResponse('Failed to fetch delivery history'))
    }

    res.status(200).json(formatSuccessResponse({
      items: data || [],
      count: count || 0,
      limit: parseInt(limit),
      offset: parseInt(offset)
    }))
  } catch (error) {
    console.error('Delivery history error:', error)
    res.status(500).json(formatErrorResponse('Internal server error'))
  }
}

export default withAdminAuth(handler, ['delivery_driver'])
