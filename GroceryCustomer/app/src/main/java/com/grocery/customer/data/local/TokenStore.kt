package com.grocery.customer.data.local

import android.content.Context
import android.util.Log
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStoreFile
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TokenStore @Inject constructor(
    @ApplicationContext context: Context
) {
    
    companion object {
        private const val TAG = "TokenStore"
    }
    
    private val dataStore: DataStore<Preferences> = PreferenceDataStoreFactory.create(
        scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    ) { context.preferencesDataStoreFile("auth_prefs") }

    private val KEY_ACCESS_TOKEN = stringPreferencesKey("access_token")
    private val KEY_REFRESH_TOKEN = stringPreferencesKey("refresh_token")
    private val KEY_EXPIRES_AT = longPreferencesKey("expires_at")
    private val KEY_USER_ID = stringPreferencesKey("user_id")

    suspend fun saveTokens(accessToken: String?, refreshToken: String?, expiresAt: Long?, userId: String? = null) {
        Log.d(TAG, "Saving tokens - Access: ${if (accessToken != null) "Present (${accessToken.length} chars)" else "null"}, Refresh: ${refreshToken != null}, ExpiresAt: $expiresAt, UserId: ${userId != null}")
        dataStore.edit { prefs ->
            if (accessToken != null) prefs[KEY_ACCESS_TOKEN] = accessToken else prefs.remove(KEY_ACCESS_TOKEN)
            if (refreshToken != null) prefs[KEY_REFRESH_TOKEN] = refreshToken else prefs.remove(KEY_REFRESH_TOKEN)
            if (expiresAt != null) prefs[KEY_EXPIRES_AT] = expiresAt else prefs.remove(KEY_EXPIRES_AT)
            if (userId != null) prefs[KEY_USER_ID] = userId else prefs.remove(KEY_USER_ID)
        }
        Log.d(TAG, "Tokens saved successfully")
    }

    suspend fun clear() {
        dataStore.edit { it.clear() }
    }

    suspend fun getAccessToken(): String? {
        val token = dataStore.data.map { it[KEY_ACCESS_TOKEN] }.firstOrNull()
        Log.d(TAG, "Retrieved access token: ${if (token?.isNotBlank() == true) "Present (${token.length} chars)" else "Missing/Empty"}")
        return token
    }
    
    suspend fun getUserId(): String? {
        val userId = dataStore.data.map { it[KEY_USER_ID] }.firstOrNull()
        Log.d(TAG, "Retrieved user ID: ${if (userId?.isNotBlank() == true) "Present" else "Missing/Empty"}")
        return userId
    }
    
    suspend fun getExpiresAt(): Long? {
        val expiresAt = dataStore.data.map { it[KEY_EXPIRES_AT] }.firstOrNull()
        Log.d(TAG, "Retrieved expires_at: $expiresAt")
        return expiresAt
    }
    
    suspend fun getRefreshToken(): String? {
        val token = dataStore.data.map { it[KEY_REFRESH_TOKEN] }.firstOrNull()
        Log.d(TAG, "Retrieved refresh token: ${if (token?.isNotBlank() == true) "Present" else "Missing/Empty"}")
        return token
    }
}
