import { supabase, getAuthenticatedClient } from './supabase';

/**
 * Authentication middleware for API endpoints
 * Extracts and validates the user's access token from the Authorization header
 * @param {Object} req - The request object
 * @returns {Object|null} - User object if authenticated, null if not
 */
export async function authMiddleware(req) {
  try {
    // Get Authorization header
    const authHeader = req.headers.authorization;
    
    if (!authHeader) {
      console.log('No authorization header provided');
      return null;
    }

    // Extract token from "Bearer <token>" format
    const token = authHeader.replace('Bearer ', '');
    
    if (!token || token === 'null' || token === 'undefined') {
      console.log('No valid token provided');
      return null;
    }

    // Handle development bypass tokens (created in login endpoint for unverified emails)
    if (token.startsWith('dev-token-')) {
      if (process.env.NODE_ENV === 'development' && process.env.ALLOW_UNVERIFIED_LOGIN === 'true') {
        console.log('Development mode: Using bypass token');
        
        // Extract user info from development token (you'd store this differently in production)
        // For now, return a mock user for development testing
        return {
          id: 'dev-user-id',
          email: 'test@example.com',
          aud: 'authenticated',
          role: 'authenticated'
        };
      } else {
        console.log('Development token used in non-development environment');
        return null;
      }
    }

    // Verify the token with Supabase
    const { data: { user }, error } = await supabase.auth.getUser(token);
    
    if (error) {
      console.error('Error verifying token:', error);
      return null;
    }

    if (!user) {
      console.log('No user found for token');
      return null;
    }

    // Return the authenticated user
    return user;

  } catch (error) {
    console.error('Authentication middleware error:', error);
    return null;
  }
}

/**
 * Higher-order function that wraps an API handler with authentication
 * @param {Function} handler - The API handler function
 * @returns {Function} - Wrapped handler with authentication
 */
export function withAuth(handler) {
  return async (req, res) => {
    const user = await authMiddleware(req);
    
    if (!user) {
      return res.status(401).json({
        success: false,
        message: 'Authentication required',
        error: 'UNAUTHORIZED'
      });
    }

    // Add user to request object
    req.user = user;
    
    // Call the original handler
    return handler(req, res);
  };
}

/**
 * Get authenticated Supabase client for the current user
 * @param {Object} req - Request object (should have user attached)
 * @returns {Object} - Authenticated Supabase client
 */
export function getAuthenticatedSupabaseClient(req) {
  const authHeader = req.headers.authorization;
  const token = authHeader?.replace('Bearer ', '');
  
  if (!token) {
    throw new Error('No authentication token found');
  }

  return getAuthenticatedClient(token);
}