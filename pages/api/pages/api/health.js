import { supabase } from '../../lib/supabase';

export default async function handler(req, res) {
  // Only allow GET requests
  if (req.method !== 'GET') {
    return res.status(405).json({ 
      success: false, 
      error: 'Method not allowed' 
    });
  }

  try {
    // Test database connection
    const { data, error } = await supabase
      .from('products')
      .select('count')
      .limit(1);

    if (error) {
      throw error;
    }

    // Return health status
    res.status(200).json({
      success: true,
      message: 'Grocery Delivery API is healthy',
      timestamp: new Date().toISOString(),
      database: 'connected',
      version: '1.0.0',
      endpoints: {
        health: '/api/health',
        auth: '/api/auth/*',
        products: '/api/products/*',
        users: '/api/users/*'
      }
    });

  } catch (error) {
    console.error('Health check failed:', error);
    res.status(503).json({
      success: false,
      message: 'Service unavailable',
      timestamp: new Date().toISOString(),
      database: 'disconnected',
      error: error.message
    });
  }
}