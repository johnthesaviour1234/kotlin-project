// Debug version of login.js to trace the exact issue
import { supabaseClient, supabase, getAuthenticatedClient } from './lib/supabase.js'

async function debugLogin() {
  console.log('🔧 Debug: Testing login flow step by step...')
  
  try {
    console.log('1. Testing Supabase authentication...')
    
    const { data: authData, error: authError } = await supabaseClient.auth.signInWithPassword({
      email: 'testuser.1760826150808@grocery.com',
      password: 'TestPassword123!'
    })
    
    if (authError) {
      console.log('❌ Auth Error:', authError)
      return
    }
    
    console.log('✅ Authentication successful')
    console.log('User ID:', authData.user.id)
    console.log('Email:', authData.user.email)
    console.log('Email confirmed:', authData.user.email_confirmed_at)
    console.log('Access token length:', authData.session.access_token.length)
    console.log('Access token starts with:', authData.session.access_token.substring(0, 50) + '...')
    
    console.log('\n2. Testing authenticated client creation...')
    const authenticatedClient = getAuthenticatedClient(authData.session.access_token)
    console.log('✅ Authenticated client created')
    
    console.log('\n3. Testing profile fetch with authenticated client...')
    const { data: profile, error: profileError } = await authenticatedClient
      .from('user_profiles')
      .select('*')
      .eq('id', authData.user.id)
      .single()
    
    if (profileError) {
      console.log('❌ Profile Error:', profileError)
      console.log('Error details:', {
        code: profileError.code,
        message: profileError.message,
        details: profileError.details,
        hint: profileError.hint
      })
      return
    }
    
    console.log('✅ Profile fetch successful:', profile)
    
  } catch (error) {
    console.error('❌ Unexpected error:', error)
  }
}

debugLogin()