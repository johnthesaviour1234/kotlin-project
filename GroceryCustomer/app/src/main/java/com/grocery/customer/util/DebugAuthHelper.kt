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
     * Set a mock authentication token for testing purposes.
     * In a real app, this would come from the login process.
     */
    suspend fun setMockAuthToken() {
        val mockToken = "mock_auth_token_for_testing_orders"
        val expiresAt = System.currentTimeMillis() + (24 * 60 * 60 * 1000) // 24 hours
        
        tokenStore.saveTokens(
            accessToken = mockToken,
            refreshToken = "mock_refresh_token",
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