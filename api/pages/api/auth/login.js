import { supabase, handleError, handleSuccess } from '../../../lib/supabase';

export default async function handler(req, res) {
  if (req.method !== 'POST') {
    return res.status(405).json(handleError({ message: 'Method not allowed' }));
  }

  try {
    const { email, password } = req.body;

    if (!email || !password) {
      return res.status(400).json(handleError({ 
        message: 'Email and password are required' 
      }));
    }

    // Authenticate user with Supabase
    const { data: authData, error: authError } = await supabase.auth.signInWithPassword({
      email,
      password
    });

    if (authError) {
      throw authError;
    }

    const { user, session } = authData;

    // Get user profile information
    const { data: profile, error: profileError } = await supabase
      .from('user_profiles')
      .select('id, email, full_name, phone_number, user_type, is_active')
      .eq('id', user.id)
      .single();

    if (profileError && profileError.code !== 'PGRST116') { // PGRST116 = not found
      throw profileError;
    }

    // Check if user is active
    if (profile && !profile.is_active) {
      return res.status(403).json(handleError({ 
        message: 'Account is deactivated. Please contact support.' 
      }));
    }

    res.status(200).json(handleSuccess({
      user: {
        id: user.id,
        email: user.email,
        profile: profile || null
      },
      session: {
        access_token: session.access_token,
        refresh_token: session.refresh_token,
        expires_at: session.expires_at,
        expires_in: session.expires_in
      }
    }, 'Login successful'));

  } catch (error) {
    console.error('Login API error:', error);
    
    // Handle specific authentication errors
    if (error.message.includes('Invalid login credentials')) {
      return res.status(401).json(handleError({ 
        message: 'Invalid email or password' 
      }));
    }
    
    res.status(500).json(handleError(error));
  }
}