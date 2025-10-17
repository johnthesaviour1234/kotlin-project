import { createClient } from '@supabase/supabase-js';

const supabaseUrl = process.env.SUPABASE_URL;
const supabaseKey = process.env.SUPABASE_ANON_KEY;
const supabaseServiceKey = process.env.SUPABASE_SERVICE_ROLE_KEY;

if (!supabaseUrl || !supabaseKey) {
  throw new Error('Missing Supabase environment variables');
}

// Public client (for client-side operations)
export const supabase = createClient(supabaseUrl, supabaseKey);

// Admin client (for server-side operations with elevated privileges)
export const supabaseAdmin = createClient(supabaseUrl, supabaseServiceKey, {
  auth: {
    autoRefreshToken: false,
    persistSession: false
  }
});

// Helper function to handle API errors
export function handleError(error) {
  console.error('Supabase error:', error);
  return {
    success: false,
    error: error.message || 'An unexpected error occurred',
    details: error
  };
}

// Helper function for successful responses
export function handleSuccess(data, message = 'Success') {
  return {
    success: true,
    message,
    data
  };
}