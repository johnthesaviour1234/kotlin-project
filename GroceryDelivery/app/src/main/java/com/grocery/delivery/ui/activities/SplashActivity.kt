package com.grocery.delivery.ui.activities

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import androidx.activity.viewModels
import androidx.lifecycle.lifecycleScope
import com.grocery.delivery.databinding.ActivitySplashBinding
import com.grocery.delivery.ui.viewmodels.LoginViewModel
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * Splash Activity - Initial screen shown when app launches
 * Checks authentication status and routes to appropriate screen
 */
@SuppressLint("CustomSplashScreen")
@AndroidEntryPoint
class SplashActivity : BaseActivity<ActivitySplashBinding>() {
    
    private val viewModel: LoginViewModel by viewModels()
    
    override fun inflateViewBinding(): ActivitySplashBinding {
        return ActivitySplashBinding.inflate(layoutInflater)
    }
    
    override fun setupUI() {
        // Check authentication after a short delay
        lifecycleScope.launch {
            delay(2000) // Show splash for 2 seconds
            checkAuthenticationStatus()
        }
    }
    
    override fun setupObservers() {
        // No observers needed for splash screen
    }
    
    /**
     * Check if user is logged in and route accordingly
     */
    private fun checkAuthenticationStatus() {
        if (viewModel.isLoggedIn()) {
            // User is logged in, go to main activity
            navigateToMain()
        } else {
            // User is not logged in, go to login activity
            navigateToLogin()
        }
    }
    
    /**
     * Navigate to login activity
     */
    private fun navigateToLogin() {
        val intent = Intent(this, LoginActivity::class.java)
        startActivity(intent)
        finish()
    }
    
    /**
     * Navigate to main activity
     */
    private fun navigateToMain() {
        val intent = Intent(this, MainActivity::class.java)
        startActivity(intent)
        finish()
    }
}
