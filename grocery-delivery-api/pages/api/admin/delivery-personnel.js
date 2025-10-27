import { withAdminAuth } from '../../../lib/adminMiddleware.js'
import { supabase } from '../../../lib/supabase.js'
import { formatSuccessResponse, formatErrorResponse } from '../../../lib/validation.js'

async function handler(req, res) {
  if (req.method !== 'GET') {
    return res.status(405).json(formatErrorResponse('Method not allowed'))
  }

  try {
    const { active_only = 'true' } = req.query

    let query = supabase
      .from('user_profiles')
      .select('id, email, full_name, phone, is_active, created_at')
      .eq('user_type', 'delivery_driver')
      .order('full_name', { ascending: true })

    // Filter by active status if requested
    if (active_only === 'true') {
      query = query.eq('is_active', true)
    }

    const { data, error } = await query

    if (error) {
      console.error('Error fetching delivery personnel:', error)
      return res.status(500).json(formatErrorResponse(error.message || 'Failed to fetch delivery personnel'))
    }

    // Format response
    const formattedData = {
      items: data.map(personnel => ({
        id: personnel.id,
        full_name: personnel.full_name,
        email: personnel.email,
        phone: personnel.phone,
        is_active: personnel.is_active,
        created_at: personnel.created_at
      })),
      total: data.length
    }

    res.status(200).json(formatSuccessResponse(formattedData, 'Delivery personnel fetched successfully'))
  } catch (error) {
    console.error('Delivery personnel fetch error:', error)
    res.status(500).json(formatErrorResponse('Internal server error'))
  }
}

export default withAdminAuth(handler)
