import { supabaseClient } from '../../../lib/supabase'
import { formatSuccessResponse, formatErrorResponse } from '../../../lib/validation'

function toBool(val) {
  if (val === 'true') return true
  if (val === 'false') return false
  return undefined
}

export default async function handler(req, res) {
  if (req.method !== 'GET') {
    return res.status(405).json(formatErrorResponse('Method not allowed'))
  }

  try {
    const featured = toBool(req.query.featured)
    const category = req.query.category
    const q = typeof req.query.q === 'string' ? req.query.q.trim().slice(0, 100) : undefined

    let page = parseInt(req.query.page || '1', 10)
    let limit = parseInt(req.query.limit || '20', 10)
    if (Number.isNaN(page) || page < 1) page = 1
    if (Number.isNaN(limit) || limit < 1) limit = 20
    if (limit > 50) limit = 50

    const from = (page - 1) * limit
    const to = from + limit - 1

    let query = supabaseClient
      .from('products')
      .select('id,name,price,image_url,featured,created_at,category_id', { count: 'exact' })
      .eq('is_active', true)

    if (typeof featured === 'boolean') query = query.eq('featured', featured)
    if (category) query = query.eq('category_id', category)
    if (q) {
      const safe = q.replace(/[,]/g, '')
      query = query.or(`name.ilike.%${safe}%,description.ilike.%${safe}%`)
    }

    const { data, error, count } = await query.order('created_at', { ascending: false }).range(from, to)

    if (error) {
      return res.status(500).json(formatErrorResponse('Database error', [{ field: 'database', message: error.message }]))
    }

    return res.status(200).json(formatSuccessResponse({
      items: data || [],
      pagination: {
        page,
        limit,
        total: typeof count === 'number' ? count : null,
        hasMore: typeof count === 'number' ? to + 1 < count : (data || []).length === limit
      }
    }))
  } catch (e) {
    return res.status(500).json(formatErrorResponse('Internal server error'))
  }
}