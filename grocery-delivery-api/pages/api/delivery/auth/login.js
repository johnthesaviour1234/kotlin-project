import { supabaseClient } from '../../../../lib/supabase.js'
import { formatSuccessResponse, formatErrorResponse, validateLoginRequest } from '../../../../lib/validation.js'

export default async function handler(req, res) {
  if (req.method !== 'POST') {
    return res.status(405).json(formatErrorResponse('Method not allowed'))
  }

  try {
    const { email, password } = req.body

    const validation = validateLoginRequest({ email, password })
    if (!validation.isValid) {
      return res.status(400).json(formatErrorResponse('Validation failed', validation.errors))
    }

    // Authenticate
    const { data: authData, error: authError } = await supabaseClient.auth.signInWithPassword({
      email: validation.data.email,
      password: validation.data.password
    })

    if (authError) {
      return res.status(401).json(formatErrorResponse('Invalid credentials'))
    }

    // Get profile and verify delivery driver role
    const { data: profile, error: profileError } = await supabaseClient
      .from('user_profiles')
      .select('*')
      .eq('id', authData.user.id)
      .single()

    if (profileError || !profile) {
      return res.status(500).json(formatErrorResponse('Failed to fetch user profile'))
    }

    if (profile.user_type !== 'delivery_driver') {
      return res.status(403).json(formatErrorResponse('Access denied. Delivery personnel privileges required.'))
    }

    if (!profile.is_active) {
      return res.status(403).json(formatErrorResponse('Delivery account is inactive'))
    }

    res.status(200).json(formatSuccessResponse({
      user: {
        id: authData.user.id,
        email: authData.user.email,
        profile: {
          full_name: profile.full_name,
          phone: profile.phone,
          user_type: profile.user_type,
          is_active: profile.is_active
        }
      },
      tokens: {
        access_token: authData.session.access_token,
        refresh_token: authData.session.refresh_token,
        expires_at: authData.session.expires_at,
        expires_in: authData.session.expires_in
      }
    }, 'Delivery login successful'))

  } catch (error) {
    console.error('Delivery login error:', error)
    res.status(500).json(formatErrorResponse('Internal server error'))
  }
}
