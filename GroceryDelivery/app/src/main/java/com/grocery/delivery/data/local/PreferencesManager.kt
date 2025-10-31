package com.grocery.delivery.data.local

import android.content.Context
import android.content.SharedPreferences
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Manager for SharedPreferences storage
 */
@Singleton
class PreferencesManager @Inject constructor(
    @ApplicationContext context: Context
) {
    
    private val sharedPreferences: SharedPreferences = context.getSharedPreferences(
        PREF_NAME,
        Context.MODE_PRIVATE
    )
    
    companion object {
        private const val PREF_NAME = "delivery_prefs"
        private const val KEY_AUTH_TOKEN = "auth_token"
        private const val KEY_REFRESH_TOKEN = "refresh_token"
        private const val KEY_EXPIRES_AT = "expires_at"
        private const val KEY_USER_ID = "user_id"
        private const val KEY_USER_EMAIL = "user_email"
        private const val KEY_USER_NAME = "user_name"
        private const val KEY_IS_AVAILABLE = "is_available"
    }
    
    /**
     * Auth token operations
     */
    fun saveAuthToken(token: String) {
        // Remove any whitespace/newlines from token before saving
        val cleanToken = token.replace("\n", "").replace("\r", "").trim()
        sharedPreferences.edit().putString(KEY_AUTH_TOKEN, cleanToken).apply()
    }
    
    fun getAuthToken(): String? {
        return sharedPreferences.getString(KEY_AUTH_TOKEN, null)
    }
    
    fun saveRefreshToken(token: String) {
        sharedPreferences.edit().putString(KEY_REFRESH_TOKEN, token).apply()
    }
    
    fun getRefreshToken(): String? {
        return sharedPreferences.getString(KEY_REFRESH_TOKEN, null)
    }
    
    fun saveExpiresAt(expiresAt: Long) {
        sharedPreferences.edit().putLong(KEY_EXPIRES_AT, expiresAt).apply()
    }
    
    fun getExpiresAt(): Long? {
        val expiresAt = sharedPreferences.getLong(KEY_EXPIRES_AT, -1L)
        return if (expiresAt == -1L) null else expiresAt
    }
    
    /**
     * User data operations
     */
    fun saveUserId(userId: String) {
        sharedPreferences.edit().putString(KEY_USER_ID, userId).apply()
    }
    
    fun getUserId(): String? {
        return sharedPreferences.getString(KEY_USER_ID, null)
    }
    
    fun saveUserEmail(email: String) {
        sharedPreferences.edit().putString(KEY_USER_EMAIL, email).apply()
    }
    
    fun getUserEmail(): String? {
        return sharedPreferences.getString(KEY_USER_EMAIL, null)
    }
    
    fun saveUserName(name: String) {
        sharedPreferences.edit().putString(KEY_USER_NAME, name).apply()
    }
    
    fun getUserName(): String? {
        return sharedPreferences.getString(KEY_USER_NAME, null)
    }
    
    /**
     * Driver availability
     */
    fun saveAvailability(isAvailable: Boolean) {
        sharedPreferences.edit().putBoolean(KEY_IS_AVAILABLE, isAvailable).apply()
    }
    
    fun isAvailable(): Boolean {
        return sharedPreferences.getBoolean(KEY_IS_AVAILABLE, false)
    }
    
    /**
     * Clear all preferences
     */
    fun clearAll() {
        sharedPreferences.edit().clear().apply()
    }
}
