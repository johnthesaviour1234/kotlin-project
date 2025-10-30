import { supabaseClient, getAuthenticatedClient } from '../../../lib/supabase';
import { formatSuccessResponse, formatErrorResponse } from '../../../lib/validation';
import crypto from 'crypto';

/**
 * Helper function to get Bearer token from request
 */
function getBearer(req) {
  const h = req.headers['authorization'] || req.headers['Authorization'];
  if (!h || typeof h !== 'string') return null;
  if (!h.toLowerCase().startsWith('bearer ')) return null;
  return h.slice(7).trim();
}

/**
 * Calculate checksum for data to detect changes
 */
function calculateChecksum(data) {
  const str = JSON.stringify(data);
  return crypto.createHash('md5').update(str).digest('hex');
}

/**
 * GET /api/sync/state
 * Returns current state for authenticated user with timestamps and checksums
 * 
 * Response includes:
 * - cart: User's cart items with updated_at timestamp and checksum
 * - orders: Recent orders with updated_at timestamp and checksum
 * - profile: User profile with updated_at timestamp
 * 
 * This endpoint is called by Android apps every 10 seconds to check for state changes
 */
export default async function handler(req, res) {
  // Only allow GET requests
  if (req.method !== 'GET') {
    return res.status(405).json(formatErrorResponse('Method not allowed'));
  }

  const token = getBearer(req);
  if (!token) {
    return res.status(401).json(formatErrorResponse('Authorization: Bearer token required'));
  }

  try {
    // Authenticate user
    const { data: userData, error: authError } = await supabaseClient.auth.getUser(token);
    if (authError || !userData?.user) {
      return res.status(401).json(formatErrorResponse('Invalid or expired token'));
    }
    const userId = userData.user.id;
    const client = getAuthenticatedClient(token);

    // Fetch cart state
    const { data: cartData, error: cartError } = await client
      .from('cart')
      .select(`
        id,
        product_id,
        quantity,
        price,
        created_at,
        updated_at,
        products:product_id (
          id,
          name,
          image_url
        )
      `)
      .eq('user_id', userId)
      .order('updated_at', { ascending: false });

    if (cartError) {
      console.error('Cart fetch error:', cartError);
      return res.status(500).json(formatErrorResponse('Failed to fetch cart state'));
    }

    // Transform cart data
    const cartItems = cartData?.map(item => ({
      id: item.id,
      product_id: item.product_id,
      product_name: item.products?.name || 'Unknown Product',
      image_url: item.products?.image_url || '',
      quantity: item.quantity,
      price: item.price,
      total_price: item.quantity * item.price,
      updated_at: item.updated_at
    })) || [];

    // Find most recent cart update
    const cartUpdatedAt = cartItems.length > 0 
      ? new Date(cartItems[0].updated_at).toISOString()
      : new Date(0).toISOString(); // Unix epoch if no items

    const cartChecksum = calculateChecksum(cartItems);

    // Fetch recent orders (last 30 days)
    const thirtyDaysAgo = new Date();
    thirtyDaysAgo.setDate(thirtyDaysAgo.getDate() - 30);

    const { data: ordersData, error: ordersError } = await client
      .from('orders')
      .select(`
        id,
        order_number,
        status,
        total_amount,
        created_at,
        updated_at,
        estimated_delivery_time
      `)
      .eq('customer_id', userId)
      .gte('created_at', thirtyDaysAgo.toISOString())
      .order('updated_at', { ascending: false })
      .limit(20);

    if (ordersError) {
      console.error('Orders fetch error:', ordersError);
      return res.status(500).json(formatErrorResponse('Failed to fetch orders state'));
    }

    const ordersUpdatedAt = ordersData && ordersData.length > 0
      ? new Date(ordersData[0].updated_at).toISOString()
      : new Date(0).toISOString();

    const ordersChecksum = calculateChecksum(ordersData || []);

    // Fetch user profile
    const { data: profileData, error: profileError } = await client
      .from('user_profiles')
      .select('*')
      .eq('id', userId)
      .single();

    if (profileError) {
      console.error('Profile fetch error:', profileError);
      // Profile is optional, continue without error
    }

    const profileUpdatedAt = profileData?.updated_at 
      ? new Date(profileData.updated_at).toISOString()
      : new Date(0).toISOString();

    // Construct response
    const response = {
      cart: {
        items: cartItems,
        total_items: cartItems.reduce((sum, item) => sum + item.quantity, 0),
        total_price: cartItems.reduce((sum, item) => sum + item.total_price, 0),
        updated_at: cartUpdatedAt,
        checksum: cartChecksum
      },
      orders: {
        items: ordersData || [],
        count: (ordersData || []).length,
        updated_at: ordersUpdatedAt,
        checksum: ordersChecksum
      },
      profile: {
        data: profileData || null,
        updated_at: profileUpdatedAt
      },
      timestamp: new Date().toISOString()
    };

    return res.status(200).json(formatSuccessResponse(response));

  } catch (error) {
    console.error('State sync error:', error);
    return res.status(500).json(formatErrorResponse('Internal server error'));
  }
}
