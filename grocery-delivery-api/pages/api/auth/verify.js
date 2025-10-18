import { supabaseClient, supabase } from '../../../lib/supabase'
import { validateVerifyEmailRequest, formatSuccessResponse, formatErrorResponse, sanitizeUser } from '../../../lib/validation'

export default async function handler(req, res) {
  // Only allow POST requests
  if (req.method !== 'POST') {
    return res.status(405).json(formatErrorResponse('Method not allowed'))
  }

  try {
    const { token_hash, type, email } = req.body

    // For Supabase email verification, we typically get token_hash and type from the URL params
    // But also support email for additional validation
    let validationData = {}
    
    // Handle both URL parameter style (token_hash, type) and body style (token, email)
    if (token_hash && type) {
      validationData = { token: token_hash, email: email }
    } else {
      validationData = req.body
    }

    // Validate request data
    const validation = validateVerifyEmailRequest(validationData)
    if (!validation.isValid) {
      return res.status(400).json(formatErrorResponse('Validation failed', validation.errors))
    }

    const validatedData = validation.data

    try {
      // Verify the email using Supabase Auth
      const { data: verifyData, error: verifyError } = await supabaseClient.auth.verifyOtp({
        token_hash: token_hash || validatedData.token,
        type: type || 'email'
      })

      if (verifyError) {
        console.error('Email verification error:', verifyError)
        
        // Handle specific verification errors
        if (verifyError.message.includes('Token has expired')) {
          return res.status(400).json(formatErrorResponse('Verification link has expired', [{
            field: 'token',
            message: 'The verification link has expired. Please request a new verification email.',
            action: 'resend_verification',
            endpoint: '/api/auth/resend-verification'
          }]))
        }
        
        if (verifyError.message.includes('Invalid token')) {
          return res.status(400).json(formatErrorResponse('Invalid verification link', [{
            field: 'token',
            message: 'The verification link is invalid or has already been used.',
            action: 'resend_verification',
            endpoint: '/api/auth/resend-verification'
          }]))
        }
        
        return res.status(400).json(formatErrorResponse('Email verification failed', [{
          field: 'verification',
          message: verifyError.message
        }]))
      }

      if (!verifyData.user) {
        return res.status(400).json(formatErrorResponse('Verification failed - no user data'))
      }

      // Update user profile to mark as verified (optional - Supabase handles this automatically)
      // But we can update our user_profiles table with additional verification info if needed
      const { data: profile, error: profileError } = await supabaseClient
        .from('user_profiles')
        .select('*')
        .eq('id', verifyData.user.id)
        .single()

      if (profileError && profileError.code !== 'PGRST116') {
        console.error('Error fetching user profile during verification:', profileError)
        // Don't fail the verification if profile fetch fails
      }

      // If profile exists, we can optionally update verification timestamp
      let userProfile = profile
      if (profile) {
        const { data: updatedProfile, error: updateError } = await supabase
          .from('user_profiles')
          .update({
            updated_at: new Date().toISOString(),
            // Add email_verified_at if we want to track this separately
            // email_verified_at: new Date().toISOString()
          })
          .eq('id', verifyData.user.id)
          .select()
          .single()

        if (!updateError) {
          userProfile = updatedProfile
        }
      }

      // Prepare response data
      const responseData = {
        user: {
          id: verifyData.user.id,
          email: verifyData.user.email,
          email_confirmed: true, // Always true after successful verification
          email_confirmed_at: verifyData.user.email_confirmed_at,
          profile: sanitizeUser(userProfile)
        },
        verification: {
          status: 'verified',
          verified_at: new Date().toISOString(),
          type: type || 'email'
        }
      }

      // Include session data if user is automatically signed in after verification
      if (verifyData.session) {
        responseData.tokens = {
          access_token: verifyData.session.access_token,
          refresh_token: verifyData.session.refresh_token,
          expires_at: verifyData.session.expires_at,
          expires_in: verifyData.session.expires_in
        }
        responseData.session = {
          provider_token: verifyData.session.provider_token,
          provider_refresh_token: verifyData.session.provider_refresh_token,
          token_type: verifyData.session.token_type || 'bearer'
        }
      }

      const statusCode = verifyData.session ? 200 : 200 // Always 200 for successful verification
      const message = verifyData.session 
        ? 'Email verified successfully - user signed in'
        : 'Email verified successfully - please sign in to continue'

      res.status(statusCode).json(formatSuccessResponse(responseData, message))

    } catch (supabaseError) {
      console.error('Supabase verification error:', supabaseError)
      return res.status(500).json(formatErrorResponse('Verification service error'))
    }

  } catch (error) {
    console.error('Email verification endpoint error:', error)
    res.status(500).json(formatErrorResponse('Internal server error', [{
      field: 'system',
      message: error.message || 'Unknown error occurred'
    }]))
  }
}