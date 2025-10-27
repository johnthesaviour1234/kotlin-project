import { withAdminAuth } from '../../../../lib/adminMiddleware.js'
import { supabase } from '../../../../lib/supabase.js'
import { formatSuccessResponse, formatErrorResponse } from '../../../../lib/validation.js'

async function handler(req, res) {
  if (req.method !== 'GET') {
    return res.status(405).json(formatErrorResponse('Method not allowed'))
  }

  try {
    const {
      page = '1',
      limit = '20',
      status = '',
      search = '',
      date_from = '',
      date_to = '',
      sort_by = 'created_at',
      sort_order = 'desc'
    } = req.query

    const pageNum = parseInt(page)
    const limitNum = Math.min(parseInt(limit), 100)
    const from = (pageNum - 1) * limitNum
    const to = from + limitNum - 1

    let query = supabase
      .from('orders')
      .select(`
        *,
        order_items(
          id,
          product_name,
          quantity,
          unit_price,
          total_price
        )
      `, { count: 'exact' })

    // Apply filters
    if (status) {
      query = query.eq('status', status)
    }
    if (search) {
      query = query.or(`order_number.ilike.%${search}%,customer_info->>email.ilike.%${search}%`)
    }
    if (date_from) {
      query = query.gte('created_at', date_from)
    }
    if (date_to) {
      query = query.lte('created_at', date_to)
    }

    // Apply sorting
    query = query.order(sort_by, { ascending: sort_order === 'asc' })
    query = query.range(from, to)

    const { data, error, count } = await query

    if (error) {
      console.error('Error listing orders:', error)
      return res.status(500).json(formatErrorResponse('Failed to fetch orders'))
    }

    res.status(200).json(formatSuccessResponse({
      items: data || [],
      pagination: {
        page: pageNum,
        limit: limitNum,
        total: count || 0,
        total_pages: Math.ceil((count || 0) / limitNum),
        has_more: to + 1 < (count || 0)
      }
    }))
  } catch (error) {
    console.error('List orders error:', error)
    res.status(500).json(formatErrorResponse('Internal server error'))
  }
}

export default withAdminAuth(handler)
