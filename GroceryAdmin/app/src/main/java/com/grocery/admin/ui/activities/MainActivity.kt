package com.grocery.admin.ui.activities

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.widget.Toast
import androidx.lifecycle.lifecycleScope
import androidx.navigation.NavController
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.setupWithNavController
import com.grocery.admin.BuildConfig
import com.grocery.admin.R
import com.grocery.admin.data.local.TokenStore
import com.grocery.admin.data.sync.RealtimeManager
import com.grocery.admin.databinding.ActivityMainBinding
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Main Activity - Entry point of the Admin app.
 * Displays the DashboardFragment and handles navigation.
 */
@AndroidEntryPoint
class MainActivity : BaseActivity<ActivityMainBinding>() {

    companion object {
        private const val TAG = "Admin_MainActivity"
    }

    @Inject
    lateinit var tokenStore: TokenStore
    
    @Inject
    lateinit var realtimeManager: RealtimeManager
    
    private lateinit var navController: NavController

    override fun inflateViewBinding(): ActivityMainBinding {
        return ActivityMainBinding.inflate(layoutInflater)
    }
    
    override fun setupUI() {
        // Setup toolbar
        setSupportActionBar(binding.toolbar)
        
        // Setup Navigation Component
        val navHostFragment = supportFragmentManager
            .findFragmentById(R.id.nav_host_fragment) as NavHostFragment
        navController = navHostFragment.navController
        
        // Setup bottom navigation with NavController
        binding.bottomNavigation.setupWithNavController(navController)
        
        // Log configuration
        Log.d(TAG, "Admin app started")
        Log.d(TAG, "API Base URL: ${BuildConfig.API_BASE_URL}")
        Log.d(TAG, "Supabase URL: ${BuildConfig.SUPABASE_URL}")
    }

    override fun setupObservers() {
        // Observe realtime connection state
        lifecycleScope.launch {
            realtimeManager.connectionState.collect { state ->
                Log.d(TAG, "Realtime connection state: $state")
            }
        }
        
        // Observe order refresh triggers from realtime
        lifecycleScope.launch {
            realtimeManager.orderRefreshTrigger.collect {
                Log.d(TAG, "🔄 Realtime order change detected - triggering UI refresh")
                // Broadcast to all fragments/viewmodels via local broadcast or event bus
                // For now, fragments will need to observe this themselves
            }
        }
        
        // Observe assignment refresh triggers from realtime
        lifecycleScope.launch {
            realtimeManager.assignmentRefreshTrigger.collect {
                Log.d(TAG, "🔄 Realtime assignment change detected")
            }
        }
    }
    
    override fun onResume() {
        super.onResume()
        Log.d(TAG, "Admin app resumed - starting realtime sync")
        
        // Subscribe to realtime when app comes to foreground
        lifecycleScope.launch {
            try {
                // Admin sees ALL orders and assignments
                realtimeManager.subscribeToAllOrders()
                realtimeManager.subscribeToDeliveryAssignments()
                Log.d(TAG, "✅ Realtime sync active - monitoring all orders and assignments")
            } catch (e: Exception) {
                Log.e(TAG, "Failed to start realtime sync", e)
            }
        }
    }
    
    override fun onPause() {
        super.onPause()
        Log.d(TAG, "Admin app paused - pausing realtime sync")
        
        // Unsubscribe when app goes to background to save battery
        lifecycleScope.launch {
            realtimeManager.unsubscribeAll()
        }
    }
    
    override fun onDestroy() {
        super.onDestroy()
        // Cleanup realtime connections
        lifecycleScope.launch {
            realtimeManager.unsubscribeAll()
        }
    }
    
    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.menu_main, menu)
        return true
    }
    
    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.action_logout -> {
                logout()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }
    
    private fun logout() {
        lifecycleScope.launch {
            // Clear tokens
            tokenStore.clear()
            
            Toast.makeText(this@MainActivity, "Logged out successfully", Toast.LENGTH_SHORT).show()
            
            // Navigate to Login
            val intent = Intent(this@MainActivity, LoginActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            startActivity(intent)
            finish()
        }
    }
}
