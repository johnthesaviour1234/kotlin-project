import { supabase, handleError, handleSuccess } from '../../../lib/supabase';

export default async function handler(req, res) {
  if (req.method !== 'GET') {
    return res.status(405).json(handleError({ message: 'Method not allowed' }));
  }

  try {
    const { category_id, limit = 50, offset = 0 } = req.query;

    let query = supabase
      .from('products')
      .select(`
        id,
        name,
        price,
        description,
        image_url,
        stock_quantity,
        is_active,
        created_at,
        product_categories (
          id,
          name
        )
      `)
      .eq('is_active', true)
      .order('name');

    // Filter by category if provided
    if (category_id) {
      query = query.eq('category_id', category_id);
    }

    // Apply pagination
    query = query.range(parseInt(offset), parseInt(offset) + parseInt(limit) - 1);

    const { data: products, error } = await query;

    if (error) {
      throw error;
    }

    res.status(200).json(handleSuccess({
      products,
      pagination: {
        limit: parseInt(limit),
        offset: parseInt(offset),
        count: products.length,
        has_more: products.length === parseInt(limit)
      },
      filters: {
        category_id: category_id || null
      }
    }, 'Products retrieved successfully'));

  } catch (error) {
    console.error('Products list API error:', error);
    res.status(500).json(handleError(error));
  }
}