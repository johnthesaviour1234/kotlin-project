import { supabase, supabaseClient, getAuthenticatedClient } from '../../../lib/supabase.js'
import { withCors } from '../../../lib/cors.js'
import { formatSuccessResponse, formatErrorResponse } from '../../../lib/validation.js'
import { getAuthUser } from '../../../lib/auth.js'

export default withCors(async function handler(req, res) {
  const auth = await getAuthUser(req)
  if (auth.error) {
    return res.status(401).json(formatErrorResponse(auth.error.message))
  }
  const userId = auth.user.id

  if (req.method === 'GET') {
    try {
      const client = getAuthenticatedClient(auth.token)
      const { data, error } = await client
        .from('user_profiles')
        .select('id,email,full_name,phone,user_type,avatar_url,created_at,updated_at')
        .eq('id', userId)
        .maybeSingle()
      if (error) {
        return res.status(500).json(formatErrorResponse('Database error', [{ field: 'database', message: error.message }]))
      }
      return res.status(200).json(formatSuccessResponse(data || { id: userId }))
    } catch (e) {
      return res.status(500).json(formatErrorResponse('Internal server error'))
    }
  }

  if (req.method === 'PUT') {
    try {
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
    } catch (e) {
      return res.status(500).json(formatErrorResponse('Internal server error'))
    }
  }

  return res.status(405).json(formatErrorResponse('Method not allowed'))
})
