package com.grocery.delivery.data.remote

import android.util.Log
import com.grocery.delivery.data.local.PreferencesManager
import okhttp3.Interceptor
import okhttp3.Response
import javax.inject.Inject

/**
 * AuthInterceptor - Adds authentication token to API requests
 * Keeps it simple: just adds Bearer token, no complex logic
 * 401 errors are handled at the repository layer
 */
class AuthInterceptor @Inject constructor(
    private val preferencesManager: PreferencesManager
) : Interceptor {
    
    companion object {
        private const val TAG = "AuthInterceptor"
    }
    
    override fun intercept(chain: Interceptor.Chain): Response {
        val original = chain.request()
        
        return try {
            val token = preferencesManager.getAuthToken()
            Log.d(TAG, "Token retrieved: ${if (token?.isNotBlank() == true) "Present (${token.length} chars)" else "Missing/Empty"}")
            Log.d(TAG, "Request URL: ${original.url}")
            
            if (!token.isNullOrBlank()) {
                // Remove any whitespace/newlines from token
                val cleanToken = token.replace("\n", "").replace("\r", "").trim()
                val newRequest = original.newBuilder()
                    .addHeader("Authorization", "Bearer $cleanToken")
                    .build()
                Log.d(TAG, "Added Authorization header to request")
                chain.proceed(newRequest)
            } else {
                Log.w(TAG, "No token available, proceeding without Authorization header")
                chain.proceed(original)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error in AuthInterceptor: ${e.message}", e)
            chain.proceed(original)
        }
    }
}
