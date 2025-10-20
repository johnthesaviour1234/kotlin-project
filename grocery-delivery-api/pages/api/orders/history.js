import { supabase } from '../../../lib/supabase';
import { authMiddleware } from '../../../lib/authMiddleware';

export default async function handler(req, res) {
  if (req.method !== 'GET') {
    return res.status(405).json({ message: 'Method not allowed' });
  }

  try {
    // Authenticate user
    const user = await authMiddleware(req);
    if (!user) {
      return res.status(401).json({ message: 'Unauthorized' });
    }

    const { page = 1, limit = 10, status } = req.query;
    const offset = (parseInt(page) - 1) * parseInt(limit);

    let query = supabase
      .from('orders')
      .select(`
        id,
        order_number,
        status,
        total_amount,
        payment_status,
        created_at,
        estimated_delivery_time,
        delivered_at,
        order_items (
          id,
          quantity,
          product_name,
          product_image_url
        )
      `)
      .eq('customer_id', user.id)
      .order('created_at', { ascending: false })
      .range(offset, offset + parseInt(limit) - 1);

    // Filter by status if provided
    if (status && status !== 'all') {
      query = query.eq('status', status);
    }

    const { data: orders, error: ordersError } = await query;

    if (ordersError) {
      console.error('Error fetching order history:', ordersError);
      return res.status(500).json({ message: 'Error fetching order history' });
    }

    // Get total count for pagination
    let countQuery = supabase
      .from('orders')
      .select('id', { count: 'exact', head: true })
      .eq('customer_id', user.id);

    if (status && status !== 'all') {
      countQuery = countQuery.eq('status', status);
    }

    const { count, error: countError } = await countQuery;

    if (countError) {
      console.error('Error counting orders:', countError);
      return res.status(500).json({ message: 'Error counting orders' });
    }

    const totalPages = Math.ceil(count / parseInt(limit));

    res.status(200).json({
      message: 'Order history retrieved successfully',
      orders: orders || [],
      pagination: {
        currentPage: parseInt(page),
        totalPages: totalPages,
        totalItems: count,
        limit: parseInt(limit),
        hasNextPage: parseInt(page) < totalPages,
        hasPreviousPage: parseInt(page) > 1
      }
    });

  } catch (error) {
    console.error('Unexpected error fetching order history:', error);
    res.status(500).json({ message: 'Internal server error' });
  }
}