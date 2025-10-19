package com.grocery.customer.ui.activities

import android.content.Intent
import android.util.Patterns
import android.view.View
import android.widget.Toast
import androidx.activity.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.grocery.customer.databinding.ActivityRegisterBinding
import com.grocery.customer.ui.viewmodels.AuthViewModel
import com.grocery.customer.util.Resource
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class RegisterActivity : BaseActivity<ActivityRegisterBinding>() {

    private val viewModel: AuthViewModel by viewModels()

    override fun inflateViewBinding(): ActivityRegisterBinding = ActivityRegisterBinding.inflate(layoutInflater)

    override fun setupUI() {
        binding.buttonRegister.setOnClickListener {
            val email = binding.editEmail.text?.toString()?.trim().orEmpty()
            val password = binding.editPassword.text?.toString()?.trim().orEmpty()
            val fullName = binding.editFullName.text?.toString()?.trim().orEmpty()
            val phone = binding.editPhone.text?.toString()?.trim().takeIf { it?.isNotEmpty() == true }

            if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
                showError("Enter a valid email")
                return@setOnClickListener
            }
            val strong = Regex("^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d).{8,}$").matches(password)
            if (!strong) {
                showError("Password must be 8+ chars incl. uppercase, lowercase, and a number")
                return@setOnClickListener
            }
            if (fullName.length < 2) {
                showError("Enter your full name")
                return@setOnClickListener
            }
            viewModel.register(email, password, fullName, phone, "customer")
        }
        binding.textLogin.setOnClickListener {
            startActivity(Intent(this, LoginActivity::class.java))
            finish()
        }
    }

    override fun setupObservers() {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.isLoading.collect { loading ->
                    binding.progressBar.visibility = if (loading) View.VISIBLE else View.GONE
                    binding.buttonRegister.isEnabled = !loading
                }
            }
        }
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.error.collect { err ->
                    err?.let { Toast.makeText(this@RegisterActivity, it, Toast.LENGTH_SHORT).show() }
                }
            }
        }
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.registerState.collect { state ->
                    when (state) {
                        is Resource.Success -> {
                            val autoSignedIn = state.data?.tokens?.accessToken != null
                            Toast.makeText(
                                this@RegisterActivity,
                                if (autoSignedIn) "Registration successful" else "Registered. Please verify email.",
                                Toast.LENGTH_LONG
                            ).show()
                            if (autoSignedIn) {
                                startActivity(Intent(this@RegisterActivity, MainActivity::class.java))
                                finish()
                            } else {
                                startActivity(Intent(this@RegisterActivity, LoginActivity::class.java))
                                finish()
                            }
                        }
                        is Resource.Error -> showError(state.message ?: "Registration failed")
                        is Resource.Loading -> {}
                    }
                }
            }
        }
    }

    override fun showError(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }
}