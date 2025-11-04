package com.grocery.customer.ui.activities

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.setupWithNavController
import com.google.android.material.badge.BadgeDrawable
import com.grocery.customer.R
import com.grocery.customer.databinding.ActivityMainBinding
import com.grocery.customer.BuildConfig
import com.grocery.customer.data.local.TokenStore
import com.grocery.customer.data.remote.ApiService
import com.grocery.customer.data.sync.RealtimeManager
import com.grocery.customer.data.workers.BackgroundSyncWorker
import com.grocery.customer.domain.repository.AuthRepository
import com.grocery.customer.domain.repository.CartRepository
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Main Activity - Entry point of the Customer app.
 * Handles navigation between Home, Categories, Cart, and Profile screens.
 */
@AndroidEntryPoint
class MainActivity : BaseActivity<ActivityMainBinding>() {

    @Inject
    lateinit var apiService: ApiService
    
    @Inject
    lateinit var cartRepository: CartRepository
    
    @Inject
    lateinit var tokenStore: TokenStore
    
    @Inject
    lateinit var authRepository: AuthRepository
    
    @Inject
    lateinit var realtimeManager: RealtimeManager
    
    private var currentUserId: String? = null

    companion object {
        private const val TAG = "MainActivity"
    }

    override fun inflateViewBinding(): ActivityMainBinding {
        return ActivityMainBinding.inflate(layoutInflater)
    }

    override fun setupUI() {
        Log.d(TAG, "MainActivity started")
        Log.d(TAG, "API Base URL: ${BuildConfig.API_BASE_URL}")
        Log.d(TAG, "Supabase URL: ${BuildConfig.SUPABASE_URL}")
        setupToolbar()
        setupNavigation()
        setupCartBadge()
        testApiConnection()
        initializeRealtimeSync()
    }
    
    private fun setupToolbar() {
        setSupportActionBar(binding.toolbar)
    }
    
    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.main_menu, menu)
        return true
    }
    
    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.action_logout -> {
                showLogoutConfirmation()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }
    
    private fun showLogoutConfirmation() {
        AlertDialog.Builder(this)
            .setTitle("Logout")
            .setMessage("Are you sure you want to logout?")
            .setPositiveButton("Logout") { _, _ ->
                performLogout()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }
    
    private fun performLogout() {
        // Clear all user data
        lifecycleScope.launch {
            try {
                tokenStore.clear()
                realtimeManager.unsubscribeAll()
                BackgroundSyncWorker.cancelAllSync(this@MainActivity)
                
                // Navigate to login screen
                val intent = Intent(this@MainActivity, LoginActivity::class.java)
                intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                startActivity(intent)
                finish()
            } catch (e: Exception) {
                Log.e(TAG, "Error during logout", e)
            }
        }
    }
    
    private fun testApiConnection() {
        lifecycleScope.launch {
            try {
                Log.d(TAG, "Testing API connection...")
                val response = apiService.getHealthCheck()
                Log.d(TAG, "Health check response code: ${response.code()}")
                if (response.isSuccessful) {
                    Log.d(TAG, "Health check successful: ${response.body()}")
                } else {
                    Log.e(TAG, "Health check failed: ${response.errorBody()?.string()}")
                }
                
                // Test products API
                Log.d(TAG, "Testing products API...")
                val productsResponse = apiService.getProducts(featured = true, limit = 5)
                Log.d(TAG, "Products response code: ${productsResponse.code()}")
                if (productsResponse.isSuccessful) {
                    val body = productsResponse.body()
                    Log.d(TAG, "Products API success: ${body?.success}, count: ${body?.data?.items?.size}")
                } else {
                    Log.e(TAG, "Products API failed: ${productsResponse.errorBody()?.string()}")
                }
            } catch (e: Exception) {
                Log.e(TAG, "API test failed: ${e.message}", e)
            }
        }
    }

    override fun setupObservers() {
        // Observe realtime connection state
        lifecycleScope.launch {
            realtimeManager.connectionState.collect { state ->
                Log.d(TAG, "Realtime connection state: $state")
            }
        }
    }

    private fun setupNavigation() {
        // Get the NavHostFragment and NavController
        val navHostFragment = supportFragmentManager
            .findFragmentById(R.id.nav_host_fragment) as NavHostFragment
        val navController = navHostFragment.navController
        
        // Setup bottom navigation with NavController
        binding.bottomNavigation.setupWithNavController(navController)
    }
    
    private fun setupCartBadge() {
        lifecycleScope.launch {
            cartRepository.getCart().collectLatest { cart ->
                val cartItemCount = cart.totalItems
                
                // Get or create badge for cart menu item
                val badge = binding.bottomNavigation.getOrCreateBadge(R.id.cartFragment)
                
                if (cartItemCount > 0) {
                    badge.number = cartItemCount
                    badge.isVisible = true
                } else {
                    badge.isVisible = false
                }
            }
        }
    }
    
    /**
     * Initialize realtime synchronization
     * Gets the current user ID and prepares for realtime subscriptions
     */
    private fun initializeRealtimeSync() {
        lifecycleScope.launch {
            try {
                // Get current user ID from token store
                // You may need to decode JWT or call an API to get user ID
                // For now, we'll get it when user resumes the app
                Log.d(TAG, "Realtime sync initialized - will subscribe in onResume()")
            } catch (e: Exception) {
                Log.e(TAG, "Failed to initialize realtime sync", e)
            }
        }
    }
    
    override fun onResume() {
        super.onResume()
        Log.d(TAG, "App resumed - starting realtime sync")
        
        // Subscribe to realtime channels when app comes to foreground
        lifecycleScope.launch {
            try {
                // Get current user from auth repository
                val userResult = authRepository.getCurrentUser()
                if (userResult.isSuccess) {
                    val user = userResult.getOrNull()
                    val userId = user?.id
                    if (userId != null) {
                        currentUserId = userId
                        Log.d(TAG, "Subscribing to realtime for user: $userId (${user.email})")
                        realtimeManager.subscribeToCartChanges(userId)
                        realtimeManager.subscribeToOrderChanges(userId)
                        Log.d(TAG, "✅ Realtime sync active - cart and orders will update in real-time")
                    } else {
                        Log.w(TAG, "No user ID available for realtime sync")
                    }
                } else {
                    Log.w(TAG, "User not logged in - realtime sync disabled")
                }
            } catch (e: Exception) {
                Log.e(TAG, "Failed to subscribe to realtime", e)
            }
        }
        
        // Keep WorkManager sync as fallback
        BackgroundSyncWorker.cancelBackgroundSync(this)
        BackgroundSyncWorker.scheduleForegroundSync(this)
    }
    
    override fun onPause() {
        super.onPause()
        Log.d(TAG, "App paused - unsubscribing from realtime to save battery")
        
        // Unsubscribe from realtime when app goes to background to save battery
        lifecycleScope.launch {
            try {
                realtimeManager.unsubscribeAll()
                Log.d(TAG, "Successfully unsubscribed from realtime")
            } catch (e: Exception) {
                Log.e(TAG, "Failed to unsubscribe from realtime", e)
            }
        }
        
        // Switch to background sync via WorkManager
        BackgroundSyncWorker.cancelForegroundSync(this)
        BackgroundSyncWorker.scheduleBackgroundSync(this)
    }
    
    override fun onDestroy() {
        super.onDestroy()
        Log.d(TAG, "MainActivity destroyed - cleaning up realtime")
        
        // Final cleanup of realtime connections
        lifecycleScope.launch {
            try {
                realtimeManager.unsubscribeAll()
                Log.d(TAG, "Realtime cleanup complete")
            } catch (e: Exception) {
                Log.e(TAG, "Error during realtime cleanup", e)
            }
        }
        
        // Note: Background sync will continue even after app is destroyed
        // To stop all sync, call BackgroundSyncWorker.cancelAllSync(this)
    }
    
}
