package com.grocery.admin.ui.activities

import android.content.Intent
import android.os.Bundle
import android.util.Patterns
import android.view.View
import android.widget.Toast
import androidx.activity.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.grocery.admin.databinding.ActivityLoginBinding
import com.grocery.admin.ui.viewmodels.LoginViewModel
import com.grocery.admin.util.Resource
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class LoginActivity : BaseActivity<ActivityLoginBinding>() {

    private val viewModel: LoginViewModel by viewModels()

    override fun inflateViewBinding(): ActivityLoginBinding {
        return ActivityLoginBinding.inflate(layoutInflater)
    }

    override fun setupUI() {
        binding.buttonLogin.setOnClickListener {
            val email = binding.editTextEmail.text?.toString()?.trim().orEmpty()
            val password = binding.editTextPassword.text?.toString()?.trim().orEmpty()

            // Validate email
            if (email.isEmpty()) {
                showError("Email is required")
                binding.textInputLayoutEmail.error = "Required"
                return@setOnClickListener
            }

            if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
                showError("Please enter a valid email address")
                binding.textInputLayoutEmail.error = "Invalid email"
                return@setOnClickListener
            }

            // Validate password
            if (password.isEmpty()) {
                showError("Password is required")
                binding.textInputLayoutPassword.error = "Required"
                return@setOnClickListener
            }

            if (password.length < 6) {
                showError("Password must be at least 6 characters")
                binding.textInputLayoutPassword.error = "Too short"
                return@setOnClickListener
            }

            // Clear errors
            binding.textInputLayoutEmail.error = null
            binding.textInputLayoutPassword.error = null
            hideError()

            // Attempt login
            viewModel.login(email, password)
        }

        binding.textViewRegister.setOnClickListener {
            startActivity(Intent(this, RegisterActivity::class.java))
        }
    }

    override fun setupObservers() {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.loginState.collect { state ->
                    when (state) {
                        is Resource.Success -> {
                            hideLoadingState()
                            Toast.makeText(
                                this@LoginActivity,
                                "Login successful!",
                                Toast.LENGTH_SHORT
                            ).show()
                            
                            // Navigate to MainActivity
                            val intent = Intent(this@LoginActivity, MainActivity::class.java)
                            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                            startActivity(intent)
                            finish()
                        }

                        is Resource.Error -> {
                            hideLoadingState()
                            showError(state.message ?: "Login failed. Please try again.")
                        }

                        is Resource.Loading -> {
                            showLoadingState()
                        }

                        null -> {
                            // Do nothing - initial state
                        }
                    }
                }
            }
        }
    }

    override fun showError(message: String) {
        binding.textViewError.text = message
        binding.textViewError.visibility = View.VISIBLE
    }

    private fun hideError() {
        binding.textViewError.visibility = View.GONE
    }

    private fun showLoadingState() {
        binding.progressBar.visibility = View.VISIBLE
        binding.buttonLogin.isEnabled = false
        binding.buttonLogin.alpha = 0.6f
    }

    private fun hideLoadingState() {
        binding.progressBar.visibility = View.GONE
        binding.buttonLogin.isEnabled = true
        binding.buttonLogin.alpha = 1f
    }

    override fun onDestroy() {
        super.onDestroy()
        viewModel.resetState()
    }
}
