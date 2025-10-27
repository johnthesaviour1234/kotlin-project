import { withAdminAuth } from '../../../../lib/adminMiddleware.js'
import { formatSuccessResponse, formatErrorResponse } from '../../../../lib/validation.js'

async function handler(req, res) {
  if (req.method !== 'POST') {
    return res.status(405).json(formatErrorResponse('Method not allowed'))
  }

  try {
    // Token invalidation happens client-side
    // This endpoint is mainly for logging/tracking
    
    res.status(200).json(formatSuccessResponse(null, 'Logout successful'))
  } catch (error) {
    console.error('Admin logout error:', error)
    res.status(500).json(formatErrorResponse('Internal server error'))
  }
}

export default withAdminAuth(handler)
