import { supabaseClient, supabase, getAuthenticatedClient } from '../../../lib/supabase'
import { validateLoginRequest, formatSuccessResponse, formatErrorResponse, sanitizeUser } from '../../../lib/validation'

// Helper function to handle successful authentication (deployed 2025-10-18)
async function handleSuccessfulAuth(res, authData) {
  // Get user profile from user_profiles table using authenticated client
  const authenticatedClient = getAuthenticatedClient(authData.session.access_token)
  const { data: profile, error: profileError } = await authenticatedClient
    .from('user_profiles')
    .select('*')
    .eq('id', authData.user.id)
    .single()

  // If profile doesn't exist, create a basic one
  let userProfile = profile
  if (profileError && profileError.code === 'PGRST116') { // Row not found
    const { data: newProfile, error: createError } = await authenticatedClient
      .from('user_profiles')
      .insert({
        id: authData.user.id,
        email: authData.user.email,
        full_name: authData.user.user_metadata?.full_name || null,
        user_type: 'customer',
        created_at: new Date().toISOString()
      })
      .select()
      .single()
    
    if (createError) {
      console.error('Error creating user profile:', createError)
      // Don't fail login if profile creation fails
      userProfile = {
        id: authData.user.id,
        email: authData.user.email,
        full_name: null,
        user_type: 'customer'
      }
    } else {
      userProfile = newProfile
    }
  } else if (profileError) {
    console.error('Error fetching user profile:', profileError)
    return res.status(500).json(formatErrorResponse('Failed to fetch user profile'))
  }

  // Prepare response data
  const responseData = {
    user: {
      id: authData.user.id,
      email: authData.user.email,
      email_confirmed: !!authData.user.email_confirmed_at,
      profile: sanitizeUser(userProfile)
    },
    tokens: {
      access_token: authData.session.access_token,
      refresh_token: authData.session.refresh_token,
      expires_at: authData.session.expires_at,
      expires_in: authData.session.expires_in
    },
    session: {
      provider_token: authData.session.provider_token,
      provider_refresh_token: authData.session.provider_refresh_token,
      token_type: authData.session.token_type || 'bearer'
    }
  }

  res.status(200).json(formatSuccessResponse(responseData, 'Login successful'))
}

export default async function handler(req, res) {
  // Only allow POST requests
  if (req.method !== 'POST') {
    return res.status(405).json(formatErrorResponse('Method not allowed'))
  }

  try {
    const { email, password } = req.body

    // Validate request data
    const validation = validateLoginRequest({ email, password })
    if (!validation.isValid) {
      return res.status(400).json(formatErrorResponse('Validation failed', validation.errors))
    }

    // Authenticate with Supabase
    const { data: authData, error: authError } = await supabaseClient.auth.signInWithPassword({
      email: validation.data.email,
      password: validation.data.password
    })

    if (authError) {
      console.error('Authentication error:', authError)
      
      // Handle specific authentication errors
      if (authError.message.includes('Invalid login credentials')) {
        return res.status(401).json(formatErrorResponse('Invalid email or password'))
      }
      
      if (authError.message.includes('Email not confirmed')) {
        // Development bypass for email verification
        if (process.env.NODE_ENV === 'development' && process.env.ALLOW_UNVERIFIED_LOGIN === 'true') {
          console.log('Development mode: Bypassing email verification - allowing unverified login')
          // In development, we'll continue despite email not being confirmed
          // by using service role to get user info
          try {
            const { data: userByEmail } = await supabase.auth.admin.listUsers()
            const foundUser = userByEmail.users.find(user => user.email === validation.data.email)
            
            if (foundUser) {
              // Create mock session data for development
              const mockSession = {
                access_token: 'dev-token-' + Date.now(),
                refresh_token: 'dev-refresh-' + Date.now(),
                expires_at: Math.floor(Date.now() / 1000) + 3600, // 1 hour from now
                expires_in: 3600,
                token_type: 'bearer'
              }
              
              // Use the found user data
              const devAuthData = {
                user: foundUser,
                session: mockSession
              }
              
              console.log('Development bypass successful for user:', foundUser.email)
              // Continue with normal flow using devAuthData instead of authData
              return await handleSuccessfulAuth(res, devAuthData)
            }
          } catch (devError) {
            console.error('Development bypass failed:', devError)
            return res.status(401).json(formatErrorResponse('Development bypass failed'))
          }
        }
        
        return res.status(401).json(formatErrorResponse('Please verify your email address before signing in', [{
          field: 'email_verification',
          message: 'Email verification required. Check your email and click the verification link.',
          action: 'resend_verification',
          endpoint: '/api/auth/resend-verification'
        }]))
      }
      
      return res.status(401).json(formatErrorResponse('Authentication failed'))
    }

    if (!authData.user || !authData.session) {
      return res.status(401).json(formatErrorResponse('Authentication failed - no user data'))
    }

    // Handle successful authentication using the helper function
    return await handleSuccessfulAuth(res, authData)

  } catch (error) {
    console.error('Login endpoint error:', error)
    res.status(500).json(formatErrorResponse('Internal server error', [{
      field: 'system',
      message: error.message || 'Unknown error occurred'
    }]))
  }
}