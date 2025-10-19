import { supabase, supabaseClient, getAuthenticatedClient } from '../../../lib/supabase'
import { formatSuccessResponse, formatErrorResponse } from '../../../lib/validation'

function getBearer(req) {
  const h = req.headers['authorization'] || req.headers['Authorization']
  if (!h || typeof h !== 'string') return null
  if (!h.toLowerCase().startsWith('bearer ')) return null
  return h.slice(7).trim()
}

export default async function handler(req, res) {
  const token = getBearer(req)
  if (!token) {
    return res.status(401).json(formatErrorResponse('Authorization: Bearer token required'))
  }

  try {
    const { data: userData, error: authError } = await supabaseClient.auth.getUser(token)
    if (authError || !userData?.user) {
      return res.status(401).json(formatErrorResponse('Invalid or expired token'))
    }
    const userId = userData.user.id

    if (req.method === 'GET') {
      const client = getAuthenticatedClient(token)
      const { data, error } = await client
        .from('user_profiles')
        .select('id,email,full_name,phone,user_type,avatar_url,created_at,updated_at')
        .eq('id', userId)
        .maybeSingle()

      if (error) {
        return res.status(500).json(formatErrorResponse('Database error', [{ field: 'database', message: error.message }]))
      }
      return res.status(200).json(formatSuccessResponse(data || { id: userId }))
    }

    if (req.method === 'PUT') {
      const body = req.body || {}
      const updates = {}
      if (typeof body.full_name === 'string') updates.full_name = body.full_name.trim().slice(0, 120)
      if (typeof body.phone === 'string') updates.phone = body.phone.trim().slice(0, 30)
      if (Object.keys(updates).length === 0) {
        return res.status(400).json(formatErrorResponse('At least one field (full_name or phone) must be provided'))
      }

      const { data, error } = await supabase
        .from('user_profiles')
        .upsert({ id: userId, ...updates, updated_at: new Date().toISOString() }, { onConflict: 'id' })
        .select('id,email,full_name,phone,user_type,avatar_url,created_at,updated_at')
        .maybeSingle()

      if (error) {
        return res.status(500).json(formatErrorResponse('Database error', [{ field: 'database', message: error.message }]))
      }
      return res.status(200).json(formatSuccessResponse(data))
    }

    return res.status(405).json(formatErrorResponse('Method not allowed'))
  } catch (e) {
    return res.status(500).json(formatErrorResponse('Internal server error'))
  }
}