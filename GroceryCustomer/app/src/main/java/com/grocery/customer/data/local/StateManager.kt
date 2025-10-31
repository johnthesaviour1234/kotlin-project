package com.grocery.customer.data.local

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.grocery.customer.data.remote.dto.*
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.security.MessageDigest
import java.time.Instant
import javax.inject.Inject
import javax.inject.Singleton

/**
 * StateManager handles local state storage with timestamps and checksums
 * for cart, orders, and profile data. Provides thread-safe operations
 * and MD5 checksum calculation for change detection.
 */
@Singleton
class StateManager @Inject constructor(
    context: Context
) {
    private val prefs: SharedPreferences = context.getSharedPreferences(
        PREFS_NAME,
        Context.MODE_PRIVATE
    )
    
    private val gson = Gson()
    private val mutex = Mutex()
    
    companion object {
        private const val TAG = "StateManager"
        private const val PREFS_NAME = "state_sync_prefs"
        
        // Keys for cart state
        private const val KEY_CART_ITEMS = "cart_items"
        private const val KEY_CART_TIMESTAMP = "cart_timestamp"
        private const val KEY_CART_CHECKSUM = "cart_checksum"
        
        // Keys for orders state
        private const val KEY_ORDERS_ITEMS = "orders_items"
        private const val KEY_ORDERS_TIMESTAMP = "orders_timestamp"
        private const val KEY_ORDERS_CHECKSUM = "orders_checksum"
        
        // Keys for profile state
        private const val KEY_PROFILE_DATA = "profile_data"
        private const val KEY_PROFILE_TIMESTAMP = "profile_timestamp"
        
        // Unix epoch for empty state
        private const val UNIX_EPOCH = "1970-01-01T00:00:00.000Z"
    }
    
    // ==================== Cart State ====================
    
    /**
     * Save cart state locally with timestamp and checksum
     */
    suspend fun saveCartState(
        items: List<CartItemApi>,
        timestamp: String = getCurrentTimestamp()
    ) = mutex.withLock {
        try {
            val itemsJson = gson.toJson(items)
            val checksum = calculateMD5(itemsJson)
            
            prefs.edit().apply {
                putString(KEY_CART_ITEMS, itemsJson)
                putString(KEY_CART_TIMESTAMP, timestamp)
                putString(KEY_CART_CHECKSUM, checksum)
                apply()
            }
            
            Log.d(TAG, "Cart state saved: ${items.size} items, timestamp=$timestamp, checksum=$checksum")
        } catch (e: Exception) {
            Log.e(TAG, "Error saving cart state", e)
            throw e
        }
    }
    
    /**
     * Get cart state with timestamp and checksum
     */
    suspend fun getCartState(): CartStateLocal = mutex.withLock {
        try {
            val itemsJson = prefs.getString(KEY_CART_ITEMS, "[]") ?: "[]"
            val timestamp = prefs.getString(KEY_CART_TIMESTAMP, UNIX_EPOCH) ?: UNIX_EPOCH
            val checksum = prefs.getString(KEY_CART_CHECKSUM, "") ?: ""
            
            val type = object : TypeToken<List<CartItemApi>>() {}.type
            val items: List<CartItemApi> = gson.fromJson(itemsJson, type) ?: emptyList()
            
            return CartStateLocal(
                items = items,
                timestamp = timestamp,
                checksum = checksum
            )
        } catch (e: Exception) {
            Log.e(TAG, "Error getting cart state", e)
            // Return empty state on error
            return CartStateLocal(
                items = emptyList(),
                timestamp = UNIX_EPOCH,
                checksum = ""
            )
        }
    }
    
    /**
     * Clear cart state (set to empty with Unix epoch timestamp)
     */
    suspend fun clearCartState() = mutex.withLock {
        saveCartState(emptyList(), UNIX_EPOCH)
    }
    
    // ==================== Orders State ====================
    
    /**
     * Save orders state locally with timestamp and checksum
     */
    suspend fun saveOrdersState(
        orders: List<OrderApiResponse>,
        timestamp: String = getCurrentTimestamp()
    ) = mutex.withLock {
        try {
            val ordersJson = gson.toJson(orders)
            val checksum = calculateMD5(ordersJson)
            
            prefs.edit().apply {
                putString(KEY_ORDERS_ITEMS, ordersJson)
                putString(KEY_ORDERS_TIMESTAMP, timestamp)
                putString(KEY_ORDERS_CHECKSUM, checksum)
                apply()
            }
            
            Log.d(TAG, "Orders state saved: ${orders.size} orders, timestamp=$timestamp, checksum=$checksum")
        } catch (e: Exception) {
            Log.e(TAG, "Error saving orders state", e)
            throw e
        }
    }
    
    /**
     * Get orders state with timestamp and checksum
     */
    suspend fun getOrdersState(): OrdersStateLocal = mutex.withLock {
        try {
            val ordersJson = prefs.getString(KEY_ORDERS_ITEMS, "[]") ?: "[]"
            val timestamp = prefs.getString(KEY_ORDERS_TIMESTAMP, UNIX_EPOCH) ?: UNIX_EPOCH
            val checksum = prefs.getString(KEY_ORDERS_CHECKSUM, "") ?: ""
            
            val type = object : TypeToken<List<OrderApiResponse>>() {}.type
            val orders: List<OrderApiResponse> = gson.fromJson(ordersJson, type) ?: emptyList()
            
            return OrdersStateLocal(
                items = orders,
                timestamp = timestamp,
                checksum = checksum
            )
        } catch (e: Exception) {
            Log.e(TAG, "Error getting orders state", e)
            // Return empty state on error
            return OrdersStateLocal(
                items = emptyList(),
                timestamp = UNIX_EPOCH,
                checksum = ""
            )
        }
    }
    
    /**
     * Clear orders state (set to empty with Unix epoch timestamp)
     */
    suspend fun clearOrdersState() = mutex.withLock {
        saveOrdersState(emptyList(), UNIX_EPOCH)
    }
    
    // ==================== Profile State ====================
    
    /**
     * Save profile state locally with timestamp
     */
    suspend fun saveProfileState(
        profile: UserProfile,
        timestamp: String = getCurrentTimestamp()
    ) = mutex.withLock {
        try {
            val profileJson = gson.toJson(profile)
            
            prefs.edit().apply {
                putString(KEY_PROFILE_DATA, profileJson)
                putString(KEY_PROFILE_TIMESTAMP, timestamp)
                apply()
            }
            
            Log.d(TAG, "Profile state saved: timestamp=$timestamp")
        } catch (e: Exception) {
            Log.e(TAG, "Error saving profile state", e)
            throw e
        }
    }
    
    /**
     * Get profile state with timestamp
     */
    suspend fun getProfileState(): ProfileStateLocal? = mutex.withLock {
        try {
            val profileJson = prefs.getString(KEY_PROFILE_DATA, null)
            val timestamp = prefs.getString(KEY_PROFILE_TIMESTAMP, UNIX_EPOCH) ?: UNIX_EPOCH
            
            if (profileJson.isNullOrBlank()) {
                return null
            }
            
            val profile: UserProfile = gson.fromJson(profileJson, UserProfile::class.java)
            
            return ProfileStateLocal(
                profile = profile,
                timestamp = timestamp
            )
        } catch (e: Exception) {
            Log.e(TAG, "Error getting profile state", e)
            return null
        }
    }
    
    /**
     * Clear profile state
     */
    suspend fun clearProfileState() = mutex.withLock {
        prefs.edit().apply {
            remove(KEY_PROFILE_DATA)
            remove(KEY_PROFILE_TIMESTAMP)
            apply()
        }
    }
    
    // ==================== Utility Methods ====================
    
    /**
     * Calculate MD5 checksum for a JSON string
     */
    fun calculateMD5(input: String): String {
        return try {
            val md = MessageDigest.getInstance("MD5")
            val digest = md.digest(input.toByteArray())
            digest.joinToString("") { "%02x".format(it) }
        } catch (e: Exception) {
            Log.e(TAG, "Error calculating MD5", e)
            ""
        }
    }
    
    /**
     * Get current timestamp in ISO 8601 format (UTC)
     */
    fun getCurrentTimestamp(): String {
        return Instant.now().toString()
    }
    
    /**
     * Compare two ISO 8601 timestamps
     * Returns: -1 if timestamp1 < timestamp2, 0 if equal, 1 if timestamp1 > timestamp2
     */
    fun compareTimestamps(timestamp1: String, timestamp2: String): Int {
        return try {
            val instant1 = Instant.parse(timestamp1)
            val instant2 = Instant.parse(timestamp2)
            instant1.compareTo(instant2)
        } catch (e: Exception) {
            Log.e(TAG, "Error comparing timestamps: $timestamp1 vs $timestamp2", e)
            0
        }
    }
    
    /**
     * Check if local state is newer than server state
     */
    fun isLocalNewer(localTimestamp: String, serverTimestamp: String): Boolean {
        return compareTimestamps(localTimestamp, serverTimestamp) > 0
    }
    
    /**
     * Clear all state (useful for logout)
     */
    suspend fun clearAllState() = mutex.withLock {
        prefs.edit().clear().apply()
        Log.d(TAG, "All state cleared")
    }
}

/**
 * Local cart state with timestamp and checksum
 */
data class CartStateLocal(
    val items: List<CartItemApi>,
    val timestamp: String,
    val checksum: String
)

/**
 * Local orders state with timestamp and checksum
 */
data class OrdersStateLocal(
    val items: List<OrderApiResponse>,
    val timestamp: String,
    val checksum: String
)

/**
 * Local profile state with timestamp
 */
data class ProfileStateLocal(
    val profile: UserProfile,
    val timestamp: String
)
