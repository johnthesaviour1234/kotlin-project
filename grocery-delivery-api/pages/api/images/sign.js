import { withCors } from '../../../lib/cors.js'
import { formatErrorResponse } from '../../../lib/validation.js'

export default withCors(async function handler(req, res) {
  if (req.method !== 'POST') {
    return res.status(405).json(formatErrorResponse('Method not allowed'))
  }
  // TODO: Implement signed upload if needed. For now, return 501.
  return res.status(501).json(formatErrorResponse('Not implemented'))
})
