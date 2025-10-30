import { withAdminAuth, logAdminActivity } from '../../../../lib/adminMiddleware.js'
import { supabase } from '../../../../lib/supabase.js'
import { formatSuccessResponse, formatErrorResponse } from '../../../../lib/validation.js'

async function handler(req, res) {
  if (req.method !== 'POST') {
    return res.status(405).json(formatErrorResponse('Method not allowed'))
  }

  try {
    const { order_id, delivery_personnel_id, estimated_minutes = 30 } = req.body

    if (!order_id || !delivery_personnel_id) {
      return res.status(400).json(formatErrorResponse('Missing required fields: order_id, delivery_personnel_id'))
    }

    // Use database function to assign order
    const { data, error } = await supabase
      .rpc('assign_order_to_delivery', {
        p_order_id: order_id,
        p_delivery_personnel_id: delivery_personnel_id,
        p_assigned_by: req.user.id,
        p_estimated_minutes: parseInt(estimated_minutes)
      })

    if (error) {
      console.error('Error assigning order:', error)
      return res.status(500).json(formatErrorResponse(error.message || 'Failed to assign order'))
    }

    // Log activity
    await logAdminActivity(
      req.user.id,
      'order_assigned',
      'order',
      order_id,
      {
        delivery_personnel_id,
        estimated_minutes
      }
    )

    res.status(200).json(formatSuccessResponse(data[0], 'Order assigned successfully'))
  } catch (error) {
    console.error('Assign order error:', error)
    res.status(500).json(formatErrorResponse('Internal server error'))
  }
}

export default withAdminAuth(handler)
