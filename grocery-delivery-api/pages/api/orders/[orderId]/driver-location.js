import { createClient } from '@supabase/supabase-js'

const supabaseUrl = process.env.NEXT_PUBLIC_SUPABASE_URL
const supabaseServiceKey = process.env.SUPABASE_SERVICE_ROLE_KEY

const supabase = createClient(supabaseUrl, supabaseServiceKey)

/**
 * GET /api/orders/[orderId]/driver-location
 * Fetch the latest location of the delivery driver for an order
 * Used by customer app for real-time tracking
 */
export default async function handler(req, res) {
  if (req.method !== 'GET') {
    return res.status(405).json({
      success: false,
      error: 'Method not allowed'
    })
  }

  try {
    const { orderId } = req.query

    if (!orderId) {
      return res.status(400).json({
        success: false,
        error: 'Order ID is required'
      })
    }

    // Get the delivery assignment for this order
    const { data: assignment, error: assignmentError } = await supabase
      .from('delivery_assignments')
      .select(`
        id,
        status,
        delivery_personnel_id,
        estimated_delivery_minutes,
        accepted_at
      `)
      .eq('order_id', orderId)
      .single()

    if (assignmentError || !assignment) {
      return res.status(404).json({
        success: false,
        error: 'No delivery assignment found for this order'
      })
    }

    // Check if delivery is in progress
    if (!['accepted', 'in_transit', 'arrived'].includes(assignment.status)) {
      return res.status(400).json({
        success: false,
        error: 'Delivery is not currently in progress',
        data: {
          status: assignment.status
        }
      })
    }

    // Get the latest location update from the driver
    const { data: location, error: locationError } = await supabase
      .from('delivery_locations')
      .select('*')
      .eq('delivery_personnel_id', assignment.delivery_personnel_id)
      .eq('order_id', orderId)
      .order('timestamp', { ascending: false })
      .limit(1)
      .single()

    if (locationError && locationError.code !== 'PGRST116') {
      console.error('Error fetching location:', locationError)
      return res.status(500).json({
        success: false,
        error: 'Failed to fetch driver location'
      })
    }

    // If no location found, return assignment info without location
    if (!location || locationError) {
      return res.status(200).json({
        success: true,
        data: {
          has_location: false,
          assignment_status: assignment.status,
          message: 'Driver location not yet available'
        }
      })
    }

    // Get driver profile info
    const { data: driverProfile } = await supabase
      .from('user_profiles')
      .select('full_name, phone')
      .eq('id', assignment.delivery_personnel_id)
      .single()

    // Calculate estimated time remaining
    let estimatedMinutesRemaining = null
    if (assignment.accepted_at && assignment.estimated_delivery_minutes) {
      const acceptedTime = new Date(assignment.accepted_at)
      const now = new Date()
      const elapsedMinutes = (now - acceptedTime) / (1000 * 60)
      estimatedMinutesRemaining = Math.max(0, Math.round(assignment.estimated_delivery_minutes - elapsedMinutes))
    }

    return res.status(200).json({
      success: true,
      data: {
        has_location: true,
        location: {
          latitude: parseFloat(location.latitude),
          longitude: parseFloat(location.longitude),
          accuracy: location.accuracy ? parseFloat(location.accuracy) : null,
          speed: location.speed ? parseFloat(location.speed) : null,
          heading: location.heading ? parseFloat(location.heading) : null,
          timestamp: location.timestamp,
          updated_at: location.created_at
        },
        driver: {
          name: driverProfile?.full_name || 'Driver',
          phone: driverProfile?.phone
        },
        delivery_status: assignment.status,
        estimated_minutes_remaining: estimatedMinutesRemaining
      },
      timestamp: new Date().toISOString()
    })

  } catch (error) {
    console.error('Error in driver-location endpoint:', error)
    return res.status(500).json({
      success: false,
      error: 'Internal server error',
      message: error.message
    })
  }
}
