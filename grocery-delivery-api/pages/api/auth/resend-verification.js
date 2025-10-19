import { supabaseClient } from '../../../lib/supabase'
import { formatSuccessResponse, formatErrorResponse, validate } from '../../../lib/validation'

export default async function handler(req, res) {
  if (req.method !== 'POST') {
    return res.status(405).json(formatErrorResponse('Method not allowed'))
  }

  try {
    const { email } = req.body || {}
    if (!email || typeof email !== 'string') {
      return res.status(400).json(formatErrorResponse('Email is required'))
    }

    const { data, error } = await supabaseClient.auth.resend({
      type: 'signup',
      email
    })

    if (error) {
      return res.status(400).json(formatErrorResponse('Failed to resend verification email', [{ field: 'email', message: error.message }]))
    }

    return res.status(200).json(formatSuccessResponse(null, 'Verification email sent'))
  } catch (e) {
    return res.status(500).json(formatErrorResponse('Internal server error'))
  }
}