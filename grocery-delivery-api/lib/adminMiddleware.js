import { supabase } from './supabase.js'

/**
 * Middleware to verify admin authentication and authorization
 * @param {Object} req - Request object
 * @param {Array} allowedRoles - Optional array of allowed roles
 * @returns {Object} - User object with profile
 */
export async function adminMiddleware(req, allowedRoles = ['admin']) {
  const authHeader = req.headers.authorization || req.headers.Authorization
  
  if (!authHeader || typeof authHeader !== 'string') {
    throw new Error('Missing authorization header')
  }

  if (!authHeader.toLowerCase().startsWith('bearer ')) {
    throw new Error('Invalid authorization format')
  }

  const token = authHeader.slice(7).trim()
  
  if (!token) {
    throw new Error('Missing access token')
  }

  // Verify token with Supabase
  const { data: { user }, error: authError } = await supabase.auth.getUser(token)
  
  if (authError || !user) {
    throw new Error('Invalid or expired token')
  }

  // Get user profile with role
  const { data: profile, error: profileError } = await supabase
    .from('user_profiles')
    .select('id, email, full_name, user_type, is_active')
    .eq('id', user.id)
    .single()

  if (profileError || !profile) {
    throw new Error('User profile not found')
  }

  // Check if user is active
  if (!profile.is_active) {
    throw new Error('User account is inactive')
  }

  // Check if user has required role
  if (!allowedRoles.includes(profile.user_type)) {
    throw new Error(`Insufficient permissions. Required: ${allowedRoles.join(' or ')}`)
  }

  return {
    ...user,
    profile
  }
}

/**
 * Wrapper for API handlers with admin auth
 */
export function withAdminAuth(handler, allowedRoles = ['admin']) {
  return async (req, res) => {
    try {
      const user = await adminMiddleware(req, allowedRoles)
      req.user = user
      return await handler(req, res)
    } catch (error) {
      return res.status(401).json({
        success: false,
        message: error.message || 'Unauthorized'
      })
    }
  }
}

/**
 * Log admin activity
 */
export async function logAdminActivity(adminId, action, entityType, entityId, changes = null) {
  try {
    await supabase
      .from('admin_activity_logs')
      .insert({
        admin_id: adminId,
        action,
        entity_type: entityType,
        entity_id: entityId,
        changes
      })
  } catch (error) {
    console.error('Failed to log admin activity:', error)
  }
}
