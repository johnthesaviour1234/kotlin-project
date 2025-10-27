import { withAdminAuth } from '../../../lib/adminMiddleware.js'
import { supabase } from '../../../lib/supabase.js'
import { formatSuccessResponse, formatErrorResponse } from '../../../lib/validation.js'

async function handler(req, res) {
  if (req.method !== 'GET') {
    return res.status(405).json(formatErrorResponse('Method not allowed'))
  }

  try {
    // Get orders assigned to this delivery person
    // Include: pending (not yet accepted), accepted, in_transit, and arrived
    const { data, error } = await supabase
      .from('delivery_assignments')
      .select(`
        id,
        order_id,
        status,
        assigned_at,
        estimated_delivery_minutes,
        notes,
        orders(
          id,
          order_number,
          total_amount,
          delivery_address,
          customer_info,
          notes,
          created_at
        )
      `)
      .eq('delivery_personnel_id', req.user.id)
      .in('status', ['pending', 'accepted', 'in_transit', 'arrived'])
      .order('assigned_at', { ascending: true })

    if (error) {
      console.error('Error fetching available orders:', error)
      return res.status(500).json(formatErrorResponse('Failed to fetch available orders'))
    }

    res.status(200).json(formatSuccessResponse({
      items: data || [],
      count: data?.length || 0
    }))
  } catch (error) {
    console.error('Available orders error:', error)
    res.status(500).json(formatErrorResponse('Internal server error'))
  }
}

export default withAdminAuth(handler, ['delivery_driver'])
