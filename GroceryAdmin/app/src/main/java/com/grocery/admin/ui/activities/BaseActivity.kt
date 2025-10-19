package com.grocery.admin.ui.activities

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.viewbinding.ViewBinding

/**
 * Base Activity class that provides common functionality for all activities.
 * Handles view binding, loading states, and common UI operations.
 */
abstract class BaseActivity<VB : ViewBinding> : AppCompatActivity() {

    private var _binding: VB? = null
    protected val binding get() = _binding!!

    /**
     * Abstract method to inflate the view binding.
     * Each activity must implement this method to provide its specific binding.
     */
    abstract fun inflateViewBinding(): VB

    /**
     * Abstract method for setting up the UI after view binding is ready.
     * Called after onCreate() and view binding setup.
     */
    abstract fun setupUI()

    /**
     * Abstract method for setting up observers (LiveData, StateFlow, etc.).
     * Called after setupUI() to observe ViewModel data.
     */
    abstract fun setupObservers()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        _binding = inflateViewBinding()
        setContentView(binding.root)
        
        setupUI()
        setupObservers()
    }

    override fun onDestroy() {
        super.onDestroy()
        _binding = null
    }

    /**
     * Show loading indicator. Can be overridden by child activities for custom loading UI.
     */
    protected open fun showLoading() {
        // Default implementation - can be overridden
        // For example, show a progress dialog or loading spinner
    }

    /**
     * Hide loading indicator. Can be overridden by child activities for custom loading UI.
     */
    protected open fun hideLoading() {
        // Default implementation - can be overridden
        // Hide progress dialog or loading spinner
    }

    /**
     * Show error message. Can be overridden by child activities for custom error handling.
     */
    protected open fun showError(message: String) {
        // Default implementation - can show toast, snackbar, or dialog
        // This can be overridden for custom error handling
    }
}