package com.grocery.admin.ui.activities

import android.content.Intent
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.widget.Toast
import androidx.lifecycle.lifecycleScope
import com.grocery.admin.R
import com.grocery.admin.data.local.TokenStore
import com.grocery.admin.databinding.ActivityMainBinding
import com.grocery.admin.ui.fragments.DashboardFragment
import com.grocery.admin.ui.fragments.ProductsFragment
import com.grocery.admin.ui.fragments.OrdersFragment
import com.google.android.material.bottomnavigation.BottomNavigationView
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Main Activity - Entry point of the Admin app.
 * Displays the DashboardFragment and handles navigation.
 */
@AndroidEntryPoint
class MainActivity : BaseActivity<ActivityMainBinding>() {

    @Inject
    lateinit var tokenStore: TokenStore
    
    private var isFirstLaunch = true

    override fun inflateViewBinding(): ActivityMainBinding {
        return ActivityMainBinding.inflate(layoutInflater)
    }
    
    override fun onCreate(savedInstanceState: Bundle?) {
        isFirstLaunch = savedInstanceState == null
        super.onCreate(savedInstanceState)
    }

    override fun setupUI() {
        // Setup toolbar
        setSupportActionBar(binding.toolbar)
        
        // Setup bottom navigation
        setupBottomNavigation()
        
        // Load DashboardFragment by default
        if (isFirstLaunch) {
            loadDashboardFragment()
        }
    }

    override fun setupObservers() {
        // Set up observers for ViewModels if needed
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
    
    private fun loadDashboardFragment() {
        supportFragmentManager.beginTransaction()
            .replace(R.id.fragmentContainer, DashboardFragment())
            .commit()
    }
    
    private fun loadProductsFragment() {
        supportFragmentManager.beginTransaction()
            .replace(R.id.fragmentContainer, ProductsFragment())
            .commit()
    }
    
    private fun loadOrdersFragment() {
        supportFragmentManager.beginTransaction()
            .replace(R.id.fragmentContainer, OrdersFragment())
            .commit()
    }
    
    private fun setupBottomNavigation() {
        binding.bottomNavigation.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_dashboard -> {
                    loadDashboardFragment()
                    true
                }
                R.id.nav_orders -> {
                    loadOrdersFragment()
                    true
                }
                R.id.nav_products -> {
                    loadProductsFragment()
                    true
                }
                R.id.nav_inventory -> {
                    // TODO: Implement InventoryFragment
                    Toast.makeText(this, "Inventory - Coming Soon", Toast.LENGTH_SHORT).show()
                    true
                }
                else -> false
            }
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
