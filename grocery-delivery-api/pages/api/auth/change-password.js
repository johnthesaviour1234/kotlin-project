import { supabaseClient } from '../../../lib/supabase'
import { formatSuccessResponse, formatErrorResponse } from '../../../lib/validation'

function getBearer(req) {
  const h = req.headers['authorization'] || req.headers['Authorization']
  if (!h || typeof h !== 'string') return null
  if (!h.toLowerCase().startsWith('bearer ')) return null
  return h.slice(7).trim()
}

export default async function handler(req, res) {
  if (req.method !== 'PUT') {
    return res.status(405).json(formatErrorResponse('Method not allowed'))
  }

  const token = getBearer(req)
  if (!token) {
    return res.status(401).json(formatErrorResponse('Authorization: Bearer token required'))
  }

  const { currentPassword, newPassword } = req.body || {}

  // Validate input
  if (!currentPassword || typeof currentPassword !== 'string') {
    return res.status(400).json(formatErrorResponse('Current password is required'))
  }
  
  if (!newPassword || typeof newPassword !== 'string' || newPassword.length < 8) {
    return res.status(400).json(formatErrorResponse('New password must be at least 8 characters long'))
  }

  try {
    // Verify current token and get user
    const { data: userData, error: authError } = await supabaseClient.auth.getUser(token)
    if (authError || !userData?.user) {
      return res.status(401).json(formatErrorResponse('Invalid or expired token'))
    }

    // Verify current password by attempting to sign in
    const { data: signInData, error: signInError } = await supabaseClient.auth.signInWithPassword({
      email: userData.user.email,
      password: currentPassword
    })

    if (signInError) {
      return res.status(400).json(formatErrorResponse('Current password is incorrect'))
    }

    // Update password
    const { data: updateData, error: updateError } = await supabaseClient.auth.updateUser({
      password: newPassword
    })

    if (updateError) {
      return res.status(500).json(formatErrorResponse('Failed to update password', [{ field: 'password', message: updateError.message }]))
    }

    return res.status(200).json(formatSuccessResponse({ message: 'Password updated successfully' }))
  } catch (e) {
    return res.status(500).json(formatErrorResponse('Internal server error'))
  }
}