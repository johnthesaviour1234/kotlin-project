import { withAdminAuth } from '../../../lib/adminMiddleware.js'
import { supabase } from '../../../lib/supabase.js'
import { formatSuccessResponse, formatErrorResponse } from '../../../lib/validation.js'

async function handler(req, res) {
  if (req.method === 'GET') {
    return handleGetProfile(req, res)
  } else if (req.method === 'PUT') {
    return handleUpdateProfile(req, res)
  } else {
    return res.status(405).json(formatErrorResponse('Method not allowed'))
  }
}

async function handleGetProfile(req, res) {
  try {
    // Get driver profile information
    const { data, error } = await supabase
      .from('user_profiles')
      .select(`
        id,
        email,
        full_name,
        phone,
        user_type,
        is_active,
        avatar_url,
        preferences,
        created_at,
        updated_at
      `)
      .eq('id', req.user.id)
      .eq('user_type', 'delivery_driver')
      .single()

    if (error) {
      console.error('Error fetching profile:', error)
      return res.status(500).json(formatErrorResponse('Failed to fetch profile'))
    }

    if (!data) {
      return res.status(404).json(formatErrorResponse('Profile not found'))
    }

    // Get delivery statistics
    const { data: stats, error: statsError } = await supabase
      .from('delivery_assignments')
      .select('status, created_at', { count: 'exact' })
      .eq('delivery_personnel_id', req.user.id)

    let deliveryStats = {
      total_deliveries: 0,
      completed_deliveries: 0,
      active_deliveries: 0
    }

    if (!statsError && stats) {
      deliveryStats = {
        total_deliveries: stats.length,
        completed_deliveries: stats.filter(s => s.status === 'completed').length,
        active_deliveries: stats.filter(s => ['accepted', 'in_transit', 'arrived'].includes(s.status)).length
      }
    }

    res.status(200).json(formatSuccessResponse({
      profile: data,
      stats: deliveryStats
    }))
  } catch (error) {
    console.error('Get profile error:', error)
    res.status(500).json(formatErrorResponse('Internal server error'))
  }
}

async function handleUpdateProfile(req, res) {
  try {
    const { full_name, phone, avatar_url, preferences } = req.body

    const updates = {}
    if (full_name !== undefined) updates.full_name = full_name
    if (phone !== undefined) updates.phone = phone
    if (avatar_url !== undefined) updates.avatar_url = avatar_url
    if (preferences !== undefined) updates.preferences = preferences

    if (Object.keys(updates).length === 0) {
      return res.status(400).json(formatErrorResponse('No fields to update'))
    }

    updates.updated_at = new Date().toISOString()

    const { data, error } = await supabase
      .from('user_profiles')
      .update(updates)
      .eq('id', req.user.id)
      .eq('user_type', 'delivery_driver')
      .select()
      .single()

    if (error) {
      console.error('Error updating profile:', error)
      return res.status(500).json(formatErrorResponse('Failed to update profile'))
    }

    res.status(200).json(formatSuccessResponse({
      profile: data,
      message: 'Profile updated successfully'
    }))
  } catch (error) {
    console.error('Update profile error:', error)
    res.status(500).json(formatErrorResponse('Internal server error'))
  }
}

export default withAdminAuth(handler, ['delivery_driver'])
