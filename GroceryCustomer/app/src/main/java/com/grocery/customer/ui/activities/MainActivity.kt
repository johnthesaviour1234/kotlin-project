package com.grocery.customer.ui.activities

import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.TextView
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.setupWithNavController
import com.google.android.material.badge.BadgeDrawable
import com.grocery.customer.R
import com.grocery.customer.databinding.ActivityMainBinding
import com.grocery.customer.BuildConfig
import com.grocery.customer.data.remote.ApiService
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

    companion object {
        private const val TAG = "MainActivity"
    }

    override fun inflateViewBinding(): ActivityMainBinding {
        return ActivityMainBinding.inflate(layoutInflater)
    }

    override fun setupUI() {
        Log.d(TAG, "MainActivity started")
        Log.d(TAG, "API Base URL: ${BuildConfig.API_BASE_URL}")
        setupNavigation()
        setupCartBadge()
        testApiConnection()
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
        // TODO: Set up observers for ViewModels when needed
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
}
