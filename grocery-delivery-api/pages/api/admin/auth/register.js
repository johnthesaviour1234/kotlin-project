import { supabaseClient, supabase } from '../../../../lib/supabase.js'
import { formatSuccessResponse, formatErrorResponse, validateRegisterRequest, sanitizeUser } from '../../../../lib/validation.js'

export default async function handler(req, res) {
  if (req.method !== 'POST') {
    return res.status(405).json(formatErrorResponse('Method not allowed'))
  }

  try {
    const { email, password, full_name, phone } = req.body

    // Validate request - force user_type to 'admin'
    const validation = validateRegisterRequest({ 
      email, 
      password, 
      full_name, 
      phone, 
      user_type: 'admin' 
    })
    
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
      return res.status(409).json(formatErrorResponse('Admin already exists with this email address'))
    }

    // Register admin with Supabase Auth
    const { data: authData, error: authError } = await supabaseClient.auth.signUp({
      email: validatedData.email,
      password: validatedData.password,
      options: {
        data: {
          full_name: validatedData.full_name,
          user_type: 'admin'
        }
      }
    })

    if (authError) {
      console.error('Admin registration error:', authError)
      
      if (authError.message.includes('User already registered')) {
        return res.status(409).json(formatErrorResponse('Admin already exists with this email address'))
      }
      
      if (authError.message.includes('Password should be')) {
        return res.status(400).json(formatErrorResponse('Password does not meet requirements (minimum 6 characters)'))
      }
      
      if (authError.message.includes('Invalid email')) {
        return res.status(400).json(formatErrorResponse('Invalid email address format'))
      }
      
      return res.status(400).json(formatErrorResponse('Admin registration failed', [{
        field: 'registration',
        message: authError.message
      }]))
    }

    if (!authData.user) {
      return res.status(400).json(formatErrorResponse('Admin registration failed - no user data'))
    }

    // Create admin profile in database using service role
    const { data: profile, error: profileError } = await supabase
      .from('user_profiles')
      .insert({
        id: authData.user.id,
        email: validatedData.email,
        full_name: validatedData.full_name,
        phone: validatedData.phone || null,
        user_type: 'admin',
        is_active: true,
        created_at: new Date().toISOString(),
        updated_at: new Date().toISOString()
      })
      .select()
      .single()

    if (profileError) {
      console.error('Error creating admin profile:', profileError)
      return res.status(500).json(formatErrorResponse('Admin registration completed but profile creation failed'))
    }

    // Prepare response data
    const responseData = {
      user: {
        id: authData.user.id,
        email: authData.user.email,
        profile: {
          full_name: profile.full_name,
          user_type: profile.user_type,
          is_active: profile.is_active
        }
      }
    }

    // Include tokens if user is automatically signed in
    if (authData.session) {
      responseData.tokens = {
        access_token: authData.session.access_token,
        refresh_token: authData.session.refresh_token,
        expires_at: authData.session.expires_at,
        expires_in: authData.session.expires_in
      }
    }

    const statusCode = authData.session ? 201 : 202
    const message = authData.session 
      ? 'Admin registration successful'
      : 'Admin registration successful - please check email for verification'

    res.status(statusCode).json(formatSuccessResponse(responseData, message))

  } catch (error) {
    console.error('Admin registration endpoint error:', error)
    res.status(500).json(formatErrorResponse('Internal server error'))
  }
}
