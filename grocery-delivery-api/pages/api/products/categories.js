import { supabaseClient } from '../../../lib/supabase.js'
import { withCors } from '../../../lib/cors.js'
import { formatSuccessResponse, formatErrorResponse } from '../../../lib/validation.js'

export default withCors(async function handler(req, res) {
  if (req.method !== 'GET') {
    return res.status(405).json(formatErrorResponse('Method not allowed'))
  }

  try {
    const { data, error } = await supabaseClient
      .from('product_categories')
      .select('id,name,description,parent_id,created_at')
      .eq('is_active', true)
      .order('name', { ascending: true })

    if (error) {
      return res
        .status(500)
        .json(
          formatErrorResponse('Database error', [
            { field: 'database', message: error.message },
          ])
        )
    }

    return res.status(200).json(formatSuccessResponse({ items: data || [] }))
  } catch (e) {
    return res.status(500).json(formatErrorResponse('Internal server error'))
  }
})
