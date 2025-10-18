package com.grocery.delivery.ui.activities

import android.os.Bundle
import com.grocery.delivery.databinding.ActivityMainBinding
import dagger.hilt.android.AndroidEntryPoint

/**
 * Main Activity - Entry point of the Customer app.
 * This activity will handle navigation between different screens.
 */
@AndroidEntryPoint
class MainActivity : BaseActivity<ActivityMainBinding>() {

    override fun inflateViewBinding(): ActivityMainBinding {
        return ActivityMainBinding.inflate(layoutInflater)
    }

    override fun setupUI() {
        // Set up navigation, toolbar, bottom navigation, etc.
        // This will be expanded when we add navigation components
        
        setupWelcomeMessage()
    }

    override fun setupObservers() {
        // Set up observers for ViewModels
        // This will be expanded when we add ViewModels
    }

    private fun setupWelcomeMessage() {
        // For now, just show a welcome message
        // This will be replaced with proper navigation setup
        binding.textViewWelcome.text = "Welcome to Grocery Delivery App!"
    }
}