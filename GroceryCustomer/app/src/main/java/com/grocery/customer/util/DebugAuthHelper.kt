package com.grocery.customer.util

import com.grocery.customer.data.local.TokenStore
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Debug helper for setting mock authentication token during development.
 * This allows testing of order management features without full authentication flow.
 */
@Singleton
class DebugAuthHelper @Inject constructor(
    private val tokenStore: TokenStore
) {
    
    /**
     * Set a production authentication token for testing purposes.
     * This uses the real token from production user: abcd@gmail.com
     */
    suspend fun setMockAuthToken() {
        // Use the real production token for testing order management
        val productionToken = "eyJhbGciOiJIUzI1NiIsImtpZCI6IldwMXNuR3pOQ1g0a083NVpnbXJZRmc9PSIsInR5cCI6IkpXVCJ9.eyJhdWQiOiJhdXRoZW50aWNhdGVkIiwiZXhwIjoxNzYxMTc0NDk3LCJpYXQiOjE3NjExNzA4OTcsImlzcyI6Imh0dHBzOi8vaGZ4ZHh4cG1jZW1kanN2aHNkY2Yuc3VwYWJhc2UuY28vYXV0aC92MSIsInN1YiI6ImI0OThhZDdhLTZkYzctNGNjNy04ZGJjLTZkNTgwOWJkNzk5NyIsImVtYWlsIjoiYWJjZEBnbWFpbC5jb20iLCJwaG9uZSI6IiIsImFwcF9tZXRhZGF0YSI6eyJwcm92aWRlciI6ImVtYWlsIiwicHJvdmlkZXJzIjpbImVtYWlsIl19LCJ1c2VyX21ldGFkYXRhIjp7ImVtYWlsIjoiYWJjZEBnbWFpbC5jb20iLCJlbWFpbF92ZXJpZmllZCI6ZmFsc2UsImZ1bGxfbmFtZSI6IiIsInBob25lX3ZlcmlmaWVkIjpmYWxzZSwic3ViIjoiYjQ5OGFkN2EtNmRjNy00Y2M3LThkYmMtNmQ1ODA5YmQ3OTk3In0sInJvbGUiOiJhdXRoZW50aWNhdGVkIiwiYWFsIjoiYWFsMSIsImFtciI6W3sibWV0aG9kIjoicGFzc3dvcmQiLCJ0aW1lc3RhbXAiOjE3NjExNzA4OTd9XSwic2Vzc2lvbl9pZCI6IjZiMTFlN2YyLWE2ZjYtNGIyZS04YjllLTJkNGViNjhjYjFjZSIsImlzX2Fub255bW91cyI6ZmFsc2V9.L5YGGJ5fGcjJmf6FxbE2XOyKnHrEHg8Kw_5QXF5F3hs"
        val expiresAt = 1761174497000L // Token expiration timestamp
        
        tokenStore.saveTokens(
            accessToken = productionToken,
            refreshToken = "production_refresh_token", 
            expiresAt = expiresAt
        )
    }
    
    /**
     * Check if we have any auth token
     */
    suspend fun hasAuthToken(): Boolean {
        return !tokenStore.getAccessToken().isNullOrBlank()
    }
    
    /**
     * Clear all auth tokens
     */
    suspend fun clearAuthTokens() {
        tokenStore.clear()
    }
}