import { withAdminAuth } from '../../../lib/adminMiddleware.js'
import { supabase } from '../../../lib/supabase.js'
import { formatSuccessResponse, formatErrorResponse } from '../../../lib/validation.js'
import eventBroadcaster from '../../../lib/eventBroadcaster.js'

async function handler(req, res) {
  if (req.method !== 'POST') {
    return res.status(405).json(formatErrorResponse('Method not allowed'))
  }

  try {
    const { latitude, longitude, accuracy, speed, heading, order_id = null } = req.body

    if (latitude === undefined || longitude === undefined) {
      return res.status(400).json(formatErrorResponse('Missing required fields: latitude, longitude'))
    }

    // Validate coordinates
    if (latitude < -90 || latitude > 90 || longitude < -180 || longitude > 180) {
      return res.status(400).json(formatErrorResponse('Invalid coordinates'))
    }

    // Insert location
    const { data, error } = await supabase
      .from('delivery_locations')
      .insert({
        delivery_personnel_id: req.user.id,
        order_id,
        latitude: parseFloat(latitude),
        longitude: parseFloat(longitude),
        accuracy: accuracy ? parseFloat(accuracy) : null,
        speed: speed ? parseFloat(speed) : null,
        heading: heading ? parseFloat(heading) : null
      })
      .select()
      .single()

    if (error) {
      console.error('Error updating location:', error)
      return res.status(500).json(formatErrorResponse('Failed to update location'))
    }

    // ✅ NEW: Broadcast driver location update
    await eventBroadcaster.driverLocationUpdated(
      req.user.id,
      parseFloat(latitude),
      parseFloat(longitude),
      order_id
    )

    res.status(201).json(formatSuccessResponse(data, 'Location updated successfully'))
  } catch (error) {
    console.error('Update location error:', error)
    res.status(500).json(formatErrorResponse('Internal server error'))
  }
}

export default withAdminAuth(handler, ['delivery_driver'])
