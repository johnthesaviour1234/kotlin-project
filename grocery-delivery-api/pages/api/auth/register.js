import { supabaseClient, supabase } from '../../../lib/supabase'
import { validateRegisterRequest, formatSuccessResponse, formatErrorResponse, sanitizeUser } from '../../../lib/validation'

export default async function handler(req, res) {
  // Only allow POST requests
  if (req.method !== 'POST') {
    return res.status(405).json(formatErrorResponse('Method not allowed'))
  }

  try {
    const { email, password, full_name, phone, user_type } = req.body

    // Validate request data
    const validation = validateRegisterRequest({ email, password, full_name, phone, user_type })
    if (!validation.isValid) {
      return res.status(400).json(formatErrorResponse('Validation failed', validation.errors))
    }

    const validatedData = validation.data

    // Check if user already exists
    const { data: existingUser } = await supabaseClient
      .from('user_profiles')
      .select('email')
      .eq('email', validatedData.email)
      .single()

    if (existingUser) {
      return res.status(409).json(formatErrorResponse('User already exists with this email address'))
    }

    // Register user with Supabase Auth
    const { data: authData, error: authError } = await supabaseClient.auth.signUp({
      email: validatedData.email,
      password: validatedData.password,
      options: {
        data: {
          full_name: validatedData.full_name,
          user_type: validatedData.user_type
        }
      }
    })

    if (authError) {
      console.error('Registration error:', authError)
      
      // Handle specific registration errors
      if (authError.message.includes('User already registered')) {
        return res.status(409).json(formatErrorResponse('User already exists with this email address'))
      }
      
      if (authError.message.includes('Password should be')) {
        return res.status(400).json(formatErrorResponse('Password does not meet requirements'))
      }
      
      if (authError.message.includes('Invalid email')) {
        return res.status(400).json(formatErrorResponse('Invalid email address format'))
      }
      
      return res.status(400).json(formatErrorResponse('Registration failed', [{
        field: 'registration',
        message: authError.message
      }]))
    }

    if (!authData.user) {
      return res.status(400).json(formatErrorResponse('Registration failed - no user data'))
    }

    // Create user profile in database using service role
    const { data: profile, error: profileError } = await supabase
      .from('user_profiles')
      .insert({
        id: authData.user.id,
        email: validatedData.email,
        full_name: validatedData.full_name,
        phone: validatedData.phone || null,
        user_type: validatedData.user_type,
        is_active: true,
        created_at: new Date().toISOString(),
        updated_at: new Date().toISOString()
      })
      .select()
      .single()

    if (profileError) {
      console.error('Error creating user profile:', profileError)
      // User was created in auth but profile creation failed
      // We should attempt to delete the auth user or mark for cleanup
      return res.status(500).json(formatErrorResponse('Registration completed but profile creation failed'))
    }

    // Prepare response data
    const responseData = {
      user: {
        id: authData.user.id,
        email: authData.user.email,
        email_confirmed: !!authData.user.email_confirmed_at,
        profile: sanitizeUser(profile)
      },
      registration: {
        confirmation_sent: !authData.session, // If no session, email confirmation is required
        requires_verification: !authData.user.email_confirmed_at
      }
    }

    // Include session data if user is automatically signed in
    if (authData.session) {
      responseData.tokens = {
        access_token: authData.session.access_token,
        refresh_token: authData.session.refresh_token,
        expires_at: authData.session.expires_at,
        expires_in: authData.session.expires_in
      }
      responseData.session = {
        token_type: authData.session.token_type || 'bearer'
      }
    }

    const statusCode = authData.session ? 201 : 202 // 201 if signed in, 202 if confirmation required
    const message = authData.session 
      ? 'Registration successful - user signed in'
      : 'Registration successful - please check your email for verification'

    res.status(statusCode).json(formatSuccessResponse(responseData, message))

  } catch (error) {
    console.error('Registration endpoint error:', error)
    res.status(500).json(formatErrorResponse('Internal server error', [{
      field: 'system',
      message: error.message || 'Unknown error occurred'
    }]))
  }
}