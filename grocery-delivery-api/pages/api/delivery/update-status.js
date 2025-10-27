import { withAdminAuth } from '../../../lib/adminMiddleware.js'
import { supabase } from '../../../lib/supabase.js'
import { formatSuccessResponse, formatErrorResponse } from '../../../lib/validation.js'

async function handler(req, res) {
  if (req.method !== 'PUT') {
    return res.status(405).json(formatErrorResponse('Method not allowed'))
  }

  try {
    const { assignment_id, status, notes = '', proof_of_delivery = null } = req.body

    const validStatuses = ['in_transit', 'completed']
    
    if (!assignment_id || !status) {
      return res.status(400).json(formatErrorResponse('Missing required fields: assignment_id, status'))
    }

    if (!validStatuses.includes(status)) {
      return res.status(400).json(formatErrorResponse(`Invalid status. Must be one of: ${validStatuses.join(', ')}`))
    }

    // Use database function
    const { data, error } = await supabase
      .rpc('update_delivery_assignment_status', {
        p_assignment_id: assignment_id,
        p_personnel_id: req.user.id,
        p_new_status: status,
        p_notes: notes
      })

    if (error) {
      console.error('Error updating delivery status:', error)
      return res.status(500).json(formatErrorResponse(error.message || 'Failed to update delivery status'))
    }

    res.status(200).json(formatSuccessResponse({ status }, 'Delivery status updated successfully'))
  } catch (error) {
    console.error('Update delivery status error:', error)
    res.status(500).json(formatErrorResponse('Internal server error'))
  }
}

export default withAdminAuth(handler, ['delivery_driver'])
