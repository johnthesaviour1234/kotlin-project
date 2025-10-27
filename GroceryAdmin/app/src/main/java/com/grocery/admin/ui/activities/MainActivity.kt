package com.grocery.admin.ui.activities

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.lifecycle.lifecycleScope
import com.grocery.admin.data.local.TokenStore
import com.grocery.admin.databinding.ActivityMainBinding
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Main Activity - Entry point of the Admin app.
 * This activity will handle navigation between different screens.
 */
@AndroidEntryPoint
class MainActivity : BaseActivity<ActivityMainBinding>() {

    @Inject
    lateinit var tokenStore: TokenStore

    override fun inflateViewBinding(): ActivityMainBinding {
        return ActivityMainBinding.inflate(layoutInflater)
    }

    override fun setupUI() {
        // Set up navigation, toolbar, bottom navigation, etc.
        // This will be expanded when we add navigation components
        
        setupWelcomeMessage()
        
        // Temporary logout button - will be replaced with proper UI
        binding.textViewStatus.text = "✅ Authentication Complete - Click to Logout"
        binding.textViewStatus.setOnClickListener {
            logout()
        }
    }

    override fun setupObservers() {
        // Set up observers for ViewModels
        // This will be expanded when we add ViewModels
    }

    private fun setupWelcomeMessage() {
        // For now, just show a welcome message
        // This will be replaced with proper navigation setup
        binding.textViewWelcome.text = "Welcome to Grocery Admin App!"
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
