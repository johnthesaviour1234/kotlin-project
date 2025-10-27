import { withAdminAuth, logAdminActivity } from '../../../../../lib/adminMiddleware.js'
import { supabase } from '../../../../../lib/supabase.js'
import { formatSuccessResponse, formatErrorResponse } from '../../../../../lib/validation.js'

async function handler(req, res) {
  if (req.method !== 'PUT') {
    return res.status(405).json(formatErrorResponse('Method not allowed'))
  }

  try {
    const { id } = req.query
    const { status, notes = '' } = req.body

    const validStatuses = ['pending', 'confirmed', 'preparing', 'ready', 'out_for_delivery', 'delivered', 'cancelled']
    
    if (!status || !validStatuses.includes(status)) {
      return res.status(400).json(formatErrorResponse(`Invalid status. Must be one of: ${validStatuses.join(', ')}`))
    }

    // Fetch current order
    const { data: currentOrder, error: fetchError } = await supabase
      .from('orders')
      .select('id, status, order_number')
      .eq('id', id)
      .single()

    if (fetchError || !currentOrder) {
      return res.status(404).json(formatErrorResponse('Order not found'))
    }

    // Update order status
    const updateData = {
      status,
      updated_at: new Date().toISOString()
    }

    if (status === 'delivered') {
      updateData.delivered_at = new Date().toISOString()
    }

    if (notes) {
      updateData.notes = notes
    }

    const { data: updatedOrder, error: updateError } = await supabase
      .from('orders')
      .update(updateData)
      .eq('id', id)
      .select()
      .single()

    if (updateError) {
      console.error('Error updating order status:', updateError)
      return res.status(500).json(formatErrorResponse('Failed to update order status'))
    }

    // Log activity
    await logAdminActivity(
      req.user.id,
      'order_status_updated',
      'order',
      id,
      {
        old_status: currentOrder.status,
        new_status: status,
        order_number: currentOrder.order_number
      }
    )

    res.status(200).json(formatSuccessResponse(updatedOrder, 'Order status updated successfully'))
  } catch (error) {
    console.error('Update order status error:', error)
    res.status(500).json(formatErrorResponse('Internal server error'))
  }
}

export default withAdminAuth(handler)
