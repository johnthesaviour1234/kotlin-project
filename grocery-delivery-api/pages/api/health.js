import { supabase } from '../../lib/supabase'
import { formatSuccessResponse, formatErrorResponse } from '../../lib/validation'

export default async function handler(req, res) {
  // Only allow GET requests
  if (req.method !== 'GET') {
    return res.status(405).json(formatErrorResponse('Method not allowed'))
  }

  try {
    const startTime = Date.now()
    
    // Test Supabase connection by checking if we can query the database
    const { data, error } = await supabase
      .from('information_schema.tables')
      .select('table_name', { count: 'exact', head: true })
      .limit(1)
    
    const dbLatency = Date.now() - startTime
    
    if (error) {
      console.error('Database connection error:', error)
      return res.status(503).json(formatErrorResponse('Database connection failed', [{
        field: 'database',
        message: error.message
      }]))
    }

    // Check environment variables
    const requiredEnvVars = [
      'SUPABASE_URL',
      'SUPABASE_ANON_KEY', 
      'SUPABASE_SERVICE_ROLE_KEY',
      'JWT_SECRET'
    ]
    
    const missingEnvVars = requiredEnvVars.filter(envVar => !process.env[envVar])
    
    if (missingEnvVars.length > 0) {
      return res.status(503).json(formatErrorResponse('Missing required environment variables', 
        missingEnvVars.map(envVar => ({
          field: 'environment',
          message: `Missing ${envVar}`
        }))
      ))
    }

    // Return healthy status
    const healthData = {
      status: 'healthy',
      timestamp: new Date().toISOString(),
      version: process.env.API_VERSION || 'v1',
      environment: process.env.NODE_ENV || 'development',
      services: {
        database: {
          status: 'operational',
          latency_ms: dbLatency,
          provider: 'supabase'
        },
        api: {
          status: 'operational',
          uptime_ms: process.uptime() * 1000
        }
      },
      endpoints: {
        authentication: '/api/auth/*',
        users: '/api/users/*',
        health: '/api/health'
      }
    }

    res.status(200).json(formatSuccessResponse(healthData, 'System is healthy'))

  } catch (error) {
    console.error('Health check error:', error)
    res.status(503).json(formatErrorResponse('Health check failed', [{
      field: 'system',
      message: error.message || 'Unknown error occurred'
    }]))
  }
}