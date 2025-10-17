// Health check endpoint for Vercel deployment
// GET /api/health - Returns API status and basic information

export default function handler(req, res) {
  // Only allow GET requests
  if (req.method !== 'GET') {
    return res.status(405).json({ 
      error: 'Method not allowed',
      message: 'This endpoint only supports GET requests'
    });
  }

  try {
    const healthData = {
      status: 'healthy',
      timestamp: new Date().toISOString(),
      version: '1.0.0',
      service: 'grocery-delivery-api',
      environment: process.env.VERCEL_ENV || 'development',
      region: process.env.VERCEL_REGION || 'unknown',
      supabase: {
        configured: !!process.env.SUPABASE_URL,
        url: process.env.SUPABASE_URL ? 'configured' : 'not configured'
      },
      endpoints: [
        '/api/health',
        '/api/auth/*',
        '/api/users/*', 
        '/api/products/*'
      ]
    };

    res.status(200).json(healthData);
  } catch (error) {
    console.error('Health check error:', error);
    res.status(500).json({
      status: 'unhealthy',
      error: 'Internal server error',
      timestamp: new Date().toISOString()
    });
  }
}