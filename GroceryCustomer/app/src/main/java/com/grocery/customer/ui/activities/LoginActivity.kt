package com.grocery.customer.ui.activities

import android.content.Intent
import android.util.Patterns
import android.view.View
import android.widget.Toast
import androidx.activity.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.grocery.customer.databinding.ActivityLoginBinding
import com.grocery.customer.ui.viewmodels.AuthViewModel
import com.grocery.customer.util.Resource
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class LoginActivity : BaseActivity<ActivityLoginBinding>() {

    private val viewModel: AuthViewModel by viewModels()

    override fun inflateViewBinding(): ActivityLoginBinding = ActivityLoginBinding.inflate(layoutInflater)

    override fun setupUI() {
        binding.buttonLogin.setOnClickListener {
            val email = binding.editEmail.text?.toString()?.trim().orEmpty()
            val password = binding.editPassword.text?.toString()?.trim().orEmpty()
            if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
                showError("Enter a valid email")
                return@setOnClickListener
            }
            if (password.length < 6) {
                showError("Password must be at least 6 characters")
                return@setOnClickListener
            }
            
            // Show loading immediately
            binding.progressBar.visibility = View.VISIBLE
            binding.buttonLogin.isEnabled = false
            
            viewModel.login(email, password)
        }
        binding.textRegister.setOnClickListener {
            startActivity(Intent(this, RegisterActivity::class.java))
        }
        binding.textForgot.setOnClickListener {
            startActivity(Intent(this, ForgotPasswordActivity::class.java))
        }
        // Hide resend verification button since email verification is disabled
        binding.textResend.visibility = View.GONE
        
        // Disabled resend functionality
        // binding.textResend.setOnClickListener {
        //     val email = binding.editEmail.text?.toString()?.trim().orEmpty()
        //     if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
        //         showError("Enter your email above to resend")
        //     } else {
        //         viewModel.resendVerification(email)
        //     }
        // }
    }

    override fun setupObservers() {
        // Remove automatic loading observer - we'll control it manually
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.error.collect { err ->
                    err?.let { Toast.makeText(this@LoginActivity, it, Toast.LENGTH_SHORT).show() }
                }
            }
        }
        // Verification functionality disabled - no need to observe resend state
        // lifecycleScope.launch {
        //     repeatOnLifecycle(Lifecycle.State.STARTED) {
        //         viewModel.resendState.collect { state ->
        //             when (state) {
        //                 is Resource.Success -> Toast.makeText(this@LoginActivity, "Verification email sent", Toast.LENGTH_SHORT).show()
        //                 is Resource.Error -> state.message?.let { showError(it) }
        //                 else -> {}
        //             }
        //         }
        //     }
        // }
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.loginState.collect { state ->
                    when (state) {
                        is Resource.Success -> {
                            // Hide loading immediately on success
                            binding.progressBar.visibility = View.GONE
                            binding.buttonLogin.isEnabled = true
                            
                            Toast.makeText(this@LoginActivity, "Login successful", Toast.LENGTH_SHORT).show()
                            startActivity(Intent(this@LoginActivity, MainActivity::class.java))
                            finish()
                        }
                        is Resource.Error -> {
                            // Hide loading on error
                            binding.progressBar.visibility = View.GONE
                            binding.buttonLogin.isEnabled = true
                            
                            showError(state.message ?: "Login failed")
                        }
                        is Resource.Loading -> {
                            // Already handled by isLoading
                        }
                    }
                }
            }
        }
    }

    override fun showError(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }
}