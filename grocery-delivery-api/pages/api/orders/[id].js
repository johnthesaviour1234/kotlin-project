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

    const { id } = req.query;

    if (!id) {
      return res.status(400).json({ message: 'Order ID is required' });
    }

    // Fetch order with items
    const { data: order, error: orderError } = await supabase
      .from('orders')
      .select(`
        *,
        order_items (
          id,
          product_id,
          quantity,
          unit_price,
          total_price,
          product_name,
          product_image_url,
          created_at
        )
      `)
      .eq('id', id)
      .eq('customer_id', user.id) // Ensure user can only access their own orders
      .single();

    if (orderError) {
      if (orderError.code === 'PGRST116') {
        return res.status(404).json({ message: 'Order not found' });
      }
      console.error('Error fetching order:', orderError);
      return res.status(500).json({ message: 'Error fetching order' });
    }

    if (!order) {
      return res.status(404).json({ message: 'Order not found' });
    }

    res.status(200).json({
      message: 'Order retrieved successfully',
      order: order
    });

  } catch (error) {
    console.error('Unexpected error fetching order:', error);
    res.status(500).json({ message: 'Internal server error' });
  }
}