package com.grocery.delivery.ui.activities

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.activity.viewModels
import com.grocery.delivery.databinding.ActivityLoginBinding
import com.grocery.delivery.ui.viewmodels.LoginViewModel
import com.grocery.delivery.utils.Resource
import dagger.hilt.android.AndroidEntryPoint

/**
 * Login Activity for delivery drivers
 */
@AndroidEntryPoint
class LoginActivity : BaseActivity<ActivityLoginBinding>() {
    
    private val viewModel: LoginViewModel by viewModels()
    
    override fun inflateViewBinding(): ActivityLoginBinding {
        return ActivityLoginBinding.inflate(layoutInflater)
    }
    
    override fun setupUI() {
        // Set up click listeners
        binding.buttonLogin.setOnClickListener {
            performLogin()
        }
        
        binding.textViewForgotPassword.setOnClickListener {
            // TODO: Implement forgot password functionality
            Toast.makeText(this, "Forgot password feature coming soon", Toast.LENGTH_SHORT).show()
        }
        
        binding.textViewRegisterLink.setOnClickListener {
            navigateToRegister()
        }
    }
    
    override fun setupObservers() {
        // Observe login state
        viewModel.loginState.observe(this) { resource ->
            when (resource) {
                is Resource.Loading -> {
                    showLoading(true)
                }
                is Resource.Success -> {
                    showLoading(false)
                    Toast.makeText(this, "Login successful!", Toast.LENGTH_SHORT).show()
                    navigateToMain()
                }
                is Resource.Error -> {
                    showLoading(false)
                    Toast.makeText(this, resource.message ?: "Login failed", Toast.LENGTH_SHORT).show()
                }
                else -> {
                    showLoading(false)
                }
            }
        }
        
        // Observe email error
        viewModel.emailError.observe(this) { error ->
            binding.textInputLayoutEmail.error = error
        }
        
        // Observe password error
        viewModel.passwordError.observe(this) { error ->
            binding.textInputLayoutPassword.error = error
        }
    }
    
    /**
     * Perform login with entered credentials
     */
    private fun performLogin() {
        val email = binding.editTextEmail.text.toString().trim()
        val password = binding.editTextPassword.text.toString().trim()
        
        viewModel.login(email, password)
    }
    
    /**
     * Show/hide loading state
     */
    private fun showLoading(isLoading: Boolean) {
        binding.progressBar.visibility = if (isLoading) View.VISIBLE else View.GONE
        binding.buttonLogin.isEnabled = !isLoading
        binding.editTextEmail.isEnabled = !isLoading
        binding.editTextPassword.isEnabled = !isLoading
    }
    
    /**
     * Navigate to main activity
     */
    private fun navigateToMain() {
        val intent = Intent(this, MainActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        finish()
    }
    
    /**
     * Navigate to register activity
     */
    private fun navigateToRegister() {
        val intent = Intent(this, RegisterActivity::class.java)
        startActivity(intent)
    }
    
    override fun onDestroy() {
        super.onDestroy()
        viewModel.clearLoginState()
    }
}
