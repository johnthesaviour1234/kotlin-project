package com.grocery.admin.ui.activities

import android.content.Intent
import android.util.Patterns
import android.view.View
import android.widget.Toast
import androidx.activity.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.grocery.admin.databinding.ActivityRegisterBinding
import com.grocery.admin.ui.viewmodels.RegisterViewModel
import com.grocery.admin.util.Resource
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class RegisterActivity : BaseActivity<ActivityRegisterBinding>() {

    private val viewModel: RegisterViewModel by viewModels()

    override fun inflateViewBinding(): ActivityRegisterBinding {
        return ActivityRegisterBinding.inflate(layoutInflater)
    }

    override fun setupUI() {
        binding.buttonBack.setOnClickListener {
            finish()
        }

        binding.buttonRegister.setOnClickListener {
            val fullName = binding.editTextFullName.text?.toString()?.trim().orEmpty()
            val email = binding.editTextEmail.text?.toString()?.trim().orEmpty()
            val phone = binding.editTextPhone.text?.toString()?.trim()
            val password = binding.editTextPassword.text?.toString()?.trim().orEmpty()
            val confirmPassword = binding.editTextConfirmPassword.text?.toString()?.trim().orEmpty()

            // Clear previous errors
            binding.textInputLayoutFullName.error = null
            binding.textInputLayoutEmail.error = null
            binding.textInputLayoutPassword.error = null
            binding.textInputLayoutConfirmPassword.error = null
            hideError()

            // Validate full name
            if (fullName.isEmpty()) {
                showError("Full name is required")
                binding.textInputLayoutFullName.error = "Required"
                return@setOnClickListener
            }

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

            // Validate confirm password
            if (confirmPassword.isEmpty()) {
                showError("Please confirm your password")
                binding.textInputLayoutConfirmPassword.error = "Required"
                return@setOnClickListener
            }

            if (password != confirmPassword) {
                showError("Passwords do not match")
                binding.textInputLayoutConfirmPassword.error = "Passwords don't match"
                return@setOnClickListener
            }

            // Attempt registration
            viewModel.register(
                email = email,
                password = password,
                fullName = fullName,
                phone = phone.takeIf { !it.isNullOrEmpty() }
            )
        }
    }

    override fun setupObservers() {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.registerState.collect { state ->
                    when (state) {
                        is Resource.Success -> {
                            hideLoadingState()
                            Toast.makeText(
                                this@RegisterActivity,
                                "Admin account created successfully!",
                                Toast.LENGTH_SHORT
                            ).show()
                            
                            // Navigate to MainActivity (auto sign-in)
                            val intent = Intent(this@RegisterActivity, MainActivity::class.java)
                            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                            startActivity(intent)
                            finish()
                        }

                        is Resource.Error -> {
                            hideLoadingState()
                            showError(state.message ?: "Registration failed. Please try again.")
                        }

                        is Resource.Loading -> {
                            showLoadingState()
                        }

                        null -> {
                            // Initial state
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
        binding.buttonRegister.isEnabled = false
        binding.buttonRegister.alpha = 0.6f
    }

    private fun hideLoadingState() {
        binding.progressBar.visibility = View.GONE
        binding.buttonRegister.isEnabled = true
        binding.buttonRegister.alpha = 1f
    }

    override fun onDestroy() {
        super.onDestroy()
        viewModel.resetState()
    }
}
