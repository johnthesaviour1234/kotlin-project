/** @type {import('next').NextConfig} */
const nextConfig = {
  // API routes configuration
  experimental: {
    serverComponentsExternalPackages: ['@supabase/supabase-js']
  },
  
  // Environment variables
  env: {
    CUSTOM_KEY: 'grocery-delivery-api',
  },
  
  // Headers for CORS (mobile app access)
  async headers() {
    return [
      {
        source: '/api/:path*',
        headers: [
          { key: 'Access-Control-Allow-Credentials', value: 'true' },
          { key: 'Access-Control-Allow-Origin', value: '*' },
          { key: 'Access-Control-Allow-Methods', value: 'GET,OPTIONS,PATCH,DELETE,POST,PUT' },
          { key: 'Access-Control-Allow-Headers', value: 'X-CSRF-Token, X-Requested-With, Accept, Accept-Version, Content-Length, Content-MD5, Content-Type, Date, X-Api-Version, Authorization' },
        ],
      },
    ];
  },
  
  // API configuration
  api: {
    bodyParser: {
      sizeLimit: '1mb',
    },
    responseLimit: '8mb',
  }
};

module.exports = nextConfig;