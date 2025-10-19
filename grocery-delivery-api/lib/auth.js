import { supabaseClient } from './supabase.js'

export async function getAuthUser(req) {
  const h = req.headers['authorization'] || req.headers['Authorization']
  if (!h || typeof h !== 'string' || !h.toLowerCase().startsWith('bearer ')) {
    return { error: { code: 'MISSING_AUTH', message: 'Authorization: Bearer token required' } }
  }
  const token = h.slice(7).trim()
  const { data, error } = await supabaseClient.auth.getUser(token)
  if (error || !data?.user) {
    return { error: { code: 'INVALID_TOKEN', message: 'Invalid or expired token' } }
  }
  return { user: data.user, token }
}
