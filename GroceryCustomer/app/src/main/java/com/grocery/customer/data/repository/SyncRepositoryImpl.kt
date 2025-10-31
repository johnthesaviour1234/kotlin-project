package com.grocery.customer.data.repository

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.util.Log
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.grocery.customer.data.local.StateManager
import com.grocery.customer.data.remote.ApiService
import com.grocery.customer.data.remote.dto.*
import com.grocery.customer.domain.repository.SyncConflictResult
import com.grocery.customer.domain.repository.SyncRepository
import com.grocery.customer.domain.repository.SyncSummary
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.delay
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.pow

/**
 * Implementation of SyncRepository
 * Handles API calls to sync endpoints and manages conflict resolution
 */
@Singleton
class SyncRepositoryImpl @Inject constructor(
    private val apiService: ApiService,
    private val stateManager: StateManager,
    @ApplicationContext private val context: Context
) : SyncRepository {
    
    private val gson = Gson()
    
    companion object {
        private const val TAG = "SyncRepository"
        private const val MAX_RETRIES = 3
        private const val INITIAL_RETRY_DELAY_MS = 1000L // 1 second
    }
    
    /**
     * Check if device has network connectivity
     */
    private fun isNetworkAvailable(): Boolean {
        val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val network = connectivityManager.activeNetwork ?: return false
        val capabilities = connectivityManager.getNetworkCapabilities(network) ?: return false
        
        return capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) &&
               capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)
    }
    
    /**
     * Execute API call with exponential backoff retry
     */
    private suspend fun <T> executeWithRetry(
        operation: suspend () -> T
    ): Result<T> {
        var lastException: Exception? = null
        
        repeat(MAX_RETRIES) { attempt ->
            try {
                return Result.success(operation())
            } catch (e: Exception) {
                lastException = e
                Log.w(TAG, "Attempt ${attempt + 1} failed: ${e.message}")
                
                if (attempt < MAX_RETRIES - 1) {
                    val delay = INITIAL_RETRY_DELAY_MS * 2.0.pow(attempt.toDouble()).toLong()
                    Log.d(TAG, "Retrying in ${delay}ms...")
                    delay(delay)
                }
            }
        }
        
        return Result.failure(lastException ?: Exception("Operation failed after $MAX_RETRIES attempts"))
    }
    
    /**
     * Fetch current state from server
     */
    override suspend fun fetchServerState(): Result<SyncStateResponse> {
        if (!isNetworkAvailable()) {
            Log.w(TAG, "No network connectivity")
            return Result.failure(Exception("No network connectivity"))
        }
        
        return executeWithRetry {
            val response = apiService.getSyncState()
            
            if (response.isSuccessful) {
                val body = response.body()
                if (body != null && body.success) {
                    Log.d(TAG, "Fetched server state successfully")
                    Log.d(TAG, "Cart: ${body.data.cart.totalItems} items, checksum=${body.data.cart.checksum}")
                    Log.d(TAG, "Orders: ${body.data.orders.count} orders, checksum=${body.data.orders.checksum}")
                    body
                } else {
                    throw Exception("Server returned unsuccessful response")
                }
            } else {
                throw Exception("HTTP ${response.code()}: ${response.message()}")
            }
        }
    }
    
    /**
     * Resolve conflict for a specific entity
     */
    override suspend fun resolveConflict(
        entity: SyncEntity,
        localState: Any,
        localTimestamp: String
    ): Result<SyncConflictResult> {
        if (!isNetworkAvailable()) {
            Log.w(TAG, "No network connectivity")
            return Result.failure(Exception("No network connectivity"))
        }
        
        return executeWithRetry {
            val request = SyncResolveRequest(
                entity = entity.value,
                localState = localState,
                localTimestamp = localTimestamp
            )
            
            val response = apiService.resolveSyncConflict(request)
            
            if (response.isSuccessful) {
                val body = response.body()
                if (body != null && body.success) {
                    Log.d(TAG, "Resolved conflict for ${entity.value}: action=${body.data.action}")
                    
                    SyncConflictResult(
                        action = SyncAction.fromString(body.data.action),
                        resolvedState = body.data.resolvedState,
                        timestamp = body.data.timestamp
                    )
                } else {
                    throw Exception("Server returned unsuccessful response")
                }
            } else {
                throw Exception("HTTP ${response.code()}: ${response.message()}")
            }
        }
    }
    
    /**
     * Perform full sync for all entities
     */
    override suspend fun performFullSync(): Result<SyncSummary> {
        if (!isNetworkAvailable()) {
            Log.w(TAG, "No network connectivity, skipping sync")
            return Result.failure(Exception("No network connectivity"))
        }
        
        Log.d(TAG, "Starting full sync...")
        val errors = mutableListOf<String>()
        val timestamp = stateManager.getCurrentTimestamp()
        
        var cartSynced = false
        var ordersSynced = false
        var profileSynced = false
        var cartAction: SyncAction? = null
        var ordersAction: SyncAction? = null
        var profileAction: SyncAction? = null
        
        try {
            // 1. Fetch server state
            val serverStateResult = fetchServerState()
            if (serverStateResult.isFailure) {
                errors.add("Failed to fetch server state: ${serverStateResult.exceptionOrNull()?.message}")
                return Result.failure(serverStateResult.exceptionOrNull() ?: Exception("Failed to fetch server state"))
            }
            
            val serverState = serverStateResult.getOrThrow()
            
            // 2. Sync Cart
            try {
                val cartResult = syncCart(serverState.data.cart)
                cartSynced = cartResult.first
                cartAction = cartResult.second
            } catch (e: Exception) {
                Log.e(TAG, "Error syncing cart", e)
                errors.add("Cart sync failed: ${e.message}")
            }
            
            // 3. Sync Orders (always server-authoritative)
            try {
                val ordersResult = syncOrders(serverState.data.orders)
                ordersSynced = ordersResult.first
                ordersAction = ordersResult.second
            } catch (e: Exception) {
                Log.e(TAG, "Error syncing orders", e)
                errors.add("Orders sync failed: ${e.message}")
            }
            
            // 4. Sync Profile
            try {
                val profileResult = syncProfile(serverState.data.profile)
                profileSynced = profileResult.first
                profileAction = profileResult.second
            } catch (e: Exception) {
                Log.e(TAG, "Error syncing profile", e)
                errors.add("Profile sync failed: ${e.message}")
            }
            
            Log.d(TAG, "Full sync completed: cart=$cartSynced, orders=$ordersSynced, profile=$profileSynced")
            
            return Result.success(
                SyncSummary(
                    cartSynced = cartSynced,
                    ordersSynced = ordersSynced,
                    profileSynced = profileSynced,
                    cartAction = cartAction,
                    ordersAction = ordersAction,
                    profileAction = profileAction,
                    errors = errors,
                    timestamp = timestamp
                )
            )
        } catch (e: Exception) {
            Log.e(TAG, "Full sync failed", e)
            errors.add("Full sync failed: ${e.message}")
            
            return Result.failure(e)
        }
    }
    
    /**
     * Sync cart with server state
     */
    private suspend fun syncCart(serverCart: SyncCartState): Pair<Boolean, SyncAction> {
        val localCart = stateManager.getCartState()
        
        // Check if checksums match (no changes)
        if (localCart.checksum == serverCart.checksum && localCart.checksum.isNotEmpty()) {
            Log.d(TAG, "Cart checksum match, no sync needed")
            return Pair(true, SyncAction.NO_CONFLICT)
        }
        
        // Compare timestamps to determine winner
        val isLocalNewer = stateManager.isLocalNewer(localCart.timestamp, serverCart.updatedAt)
        
        return if (isLocalNewer) {
            // Local wins - push local state to server
            Log.d(TAG, "Cart: Local is newer, pushing to server")
            
            val resolveResult = resolveConflict(
                entity = SyncEntity.CART,
                localState = mapOf("items" to localCart.items),
                localTimestamp = localCart.timestamp
            )
            
            if (resolveResult.isSuccess) {
                val result = resolveResult.getOrThrow()
                // Server confirmed local wins, no need to update local state
                Pair(true, result.action)
            } else {
                Pair(false, SyncAction.NO_CONFLICT)
            }
        } else {
            // Server wins - update local state
            Log.d(TAG, "Cart: Server is newer, updating local state")
            stateManager.saveCartState(serverCart.items, serverCart.updatedAt)
            Pair(true, SyncAction.SERVER_WINS)
        }
    }
    
    /**
     * Sync orders with server state (always server-authoritative)
     */
    private suspend fun syncOrders(serverOrders: SyncOrdersState): Pair<Boolean, SyncAction> {
        val localOrders = stateManager.getOrdersState()
        
        // Check if checksums match (no changes)
        if (localOrders.checksum == serverOrders.checksum && localOrders.checksum.isNotEmpty()) {
            Log.d(TAG, "Orders checksum match, no sync needed")
            return Pair(true, SyncAction.NO_CONFLICT)
        }
        
        // Orders are always server-authoritative
        Log.d(TAG, "Orders: Updating from server (${serverOrders.count} orders)")
        stateManager.saveOrdersState(serverOrders.items, serverOrders.updatedAt)
        
        return Pair(true, SyncAction.SERVER_WINS)
    }
    
    /**
     * Sync profile with server state
     */
    private suspend fun syncProfile(serverProfile: SyncProfileState): Pair<Boolean, SyncAction> {
        val localProfile = stateManager.getProfileState()
        
        if (localProfile == null) {
            // No local profile, save server profile
            Log.d(TAG, "Profile: No local profile, saving from server")
            stateManager.saveProfileState(serverProfile.data, serverProfile.updatedAt)
            return Pair(true, SyncAction.SERVER_WINS)
        }
        
        // Compare timestamps
        val isLocalNewer = stateManager.isLocalNewer(localProfile.timestamp, serverProfile.updatedAt)
        
        return if (isLocalNewer) {
            // Local wins - push local state to server
            Log.d(TAG, "Profile: Local is newer, pushing to server")
            
            val resolveResult = resolveConflict(
                entity = SyncEntity.PROFILE,
                localState = localProfile.profile,
                localTimestamp = localProfile.timestamp
            )
            
            if (resolveResult.isSuccess) {
                val result = resolveResult.getOrThrow()
                Pair(true, result.action)
            } else {
                Pair(false, SyncAction.NO_CONFLICT)
            }
        } else {
            // Server wins - update local state
            Log.d(TAG, "Profile: Server is newer, updating local state")
            stateManager.saveProfileState(serverProfile.data, serverProfile.updatedAt)
            Pair(true, SyncAction.SERVER_WINS)
        }
    }
}
