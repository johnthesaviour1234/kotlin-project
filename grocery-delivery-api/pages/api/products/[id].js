import { supabaseClient } from '../../../lib/supabase'
import { formatSuccessResponse, formatErrorResponse } from '../../../lib/validation'

function isUuid(v) {
  return /^[0-9a-f]{8}-[0-9a-f]{4}-[1-5][0-9a-f]{3}-[89ab][0-9a-f]{3}-[0-9a-f]{12}$/i.test(v)
}

export default async function handler(req, res) {
  if (req.method !== 'GET') {
    return res.status(405).json(formatErrorResponse('Method not allowed'))
  }

  try {
    const { id } = req.query
    if (!isUuid(id)) {
      return res.status(400).json(formatErrorResponse('Product id must be a valid UUID'))
    }

    const { data: product, error: pErr } = await supabaseClient
      .from('products')
      .select('id,name,category_id,price,description,image_url,featured,is_active,created_at,updated_at')
      .eq('is_active', true)
      .eq('id', id)
      .maybeSingle()

    if (pErr) {
      return res.status(500).json(formatErrorResponse('Database error', [{ field: 'database', message: pErr.message }]))
    }
    if (!product) {
      return res.status(404).json(formatErrorResponse('Product not found'))
    }

    const { data: inv, error: iErr } = await supabaseClient
      .from('inventory')
      .select('stock,updated_at')
      .eq('product_id', id)
      .maybeSingle()

    if (iErr) {
      return res.status(500).json(formatErrorResponse('Database error', [{ field: 'database', message: iErr.message }]))
    }

    return res.status(200).json(formatSuccessResponse({ ...product, inventory: inv || { stock: 0 } }))
  } catch (e) {
    return res.status(500).json(formatErrorResponse('Internal server error'))
  }
}