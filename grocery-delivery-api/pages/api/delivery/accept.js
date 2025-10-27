import { withAdminAuth } from '../../../lib/adminMiddleware.js'
import { supabase } from '../../../lib/supabase.js'
import { formatSuccessResponse, formatErrorResponse } from '../../../lib/validation.js'

async function handler(req, res) {
  if (req.method !== 'POST') {
    return res.status(405).json(formatErrorResponse('Method not allowed'))
  }

  try {
    const { assignment_id, notes = '' } = req.body

    if (!assignment_id) {
      return res.status(400).json(formatErrorResponse('Missing required field: assignment_id'))
    }

    // Use database function to accept assignment
    const { data, error } = await supabase
      .rpc('update_delivery_assignment_status', {
        p_assignment_id: assignment_id,
        p_personnel_id: req.user.id,
        p_new_status: 'accepted',
        p_notes: notes
      })

    if (error) {
      console.error('Error accepting order:', error)
      return res.status(500).json(formatErrorResponse(error.message || 'Failed to accept order'))
    }

    res.status(200).json(formatSuccessResponse({ accepted: true }, 'Order accepted successfully'))
  } catch (error) {
    console.error('Accept order error:', error)
    res.status(500).json(formatErrorResponse('Internal server error'))
  }
}

export default withAdminAuth(handler, ['delivery_driver'])
