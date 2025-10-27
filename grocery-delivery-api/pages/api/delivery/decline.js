import { withAdminAuth } from '../../../lib/adminMiddleware.js'
import { supabase } from '../../../lib/supabase.js'
import { formatSuccessResponse, formatErrorResponse } from '../../../lib/validation.js'

async function handler(req, res) {
  if (req.method !== 'POST') {
    return res.status(405).json(formatErrorResponse('Method not allowed'))
  }

  try {
    const { assignment_id, reason = '' } = req.body

    if (!assignment_id) {
      return res.status(400).json(formatErrorResponse('Missing required field: assignment_id'))
    }

    // Use database function to decline assignment
    const { data, error } = await supabase
      .rpc('update_delivery_assignment_status', {
        p_assignment_id: assignment_id,
        p_personnel_id: req.user.id,
        p_new_status: 'declined',
        p_notes: reason
      })

    if (error) {
      console.error('Error declining order:', error)
      return res.status(500).json(formatErrorResponse(error.message || 'Failed to decline order'))
    }

    res.status(200).json(formatSuccessResponse({ declined: true }, 'Order declined successfully'))
  } catch (error) {
    console.error('Decline order error:', error)
    res.status(500).json(formatErrorResponse('Internal server error'))
  }
}

export default withAdminAuth(handler, ['delivery_driver'])
