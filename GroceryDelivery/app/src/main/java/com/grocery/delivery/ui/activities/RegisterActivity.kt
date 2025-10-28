package com.grocery.delivery.ui.activities

import android.content.Intent
import android.view.View
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.activity.viewModels
import com.grocery.delivery.R
import com.grocery.delivery.databinding.ActivityRegisterBinding
import com.grocery.delivery.ui.viewmodels.RegisterViewModel
import com.grocery.delivery.utils.Resource
import dagger.hilt.android.AndroidEntryPoint

/**
 * Register Activity for new delivery drivers
 */
@AndroidEntryPoint
class RegisterActivity : BaseActivity<ActivityRegisterBinding>() {
    
    private val viewModel: RegisterViewModel by viewModels()
    
    override fun inflateViewBinding(): ActivityRegisterBinding {
        return ActivityRegisterBinding.inflate(layoutInflater)
    }
    
    override fun setupUI() {
        // Setup vehicle type dropdown
        setupVehicleTypeDropdown()
        
        // Set up click listeners
        binding.buttonRegister.setOnClickListener {
            performRegistration()
        }
        
        binding.textViewLoginLink.setOnClickListener {
            finish() // Go back to login
        }
    }
    
    override fun setupObservers() {
        // Observe registration state
        viewModel.registerState.observe(this) { resource ->
            when (resource) {
                is Resource.Loading -> {
                    showLoading(true)
                }
                is Resource.Success -> {
                    showLoading(false)
                    Toast.makeText(this, getString(R.string.registration_successful), Toast.LENGTH_SHORT).show()
                    navigateToMain()
                }
                is Resource.Error -> {
                    showLoading(false)
                    Toast.makeText(this, resource.message ?: "Registration failed", Toast.LENGTH_SHORT).show()
                }
                else -> {
                    showLoading(false)
                }
            }
        }
        
        // Observe field errors
        viewModel.emailError.observe(this) { error ->
            binding.textInputLayoutEmail.error = error
        }
        
        viewModel.passwordError.observe(this) { error ->
            binding.textInputLayoutPassword.error = error
        }
        
        viewModel.confirmPasswordError.observe(this) { error ->
            binding.textInputLayoutConfirmPassword.error = error
        }
        
        viewModel.fullNameError.observe(this) { error ->
            binding.textInputLayoutFullName.error = error
        }
        
        viewModel.phoneError.observe(this) { error ->
            binding.textInputLayoutPhone.error = error
        }
        
        viewModel.vehicleTypeError.observe(this) { error ->
            binding.textInputLayoutVehicleType.error = error
        }
        
        viewModel.vehicleNumberError.observe(this) { error ->
            binding.textInputLayoutVehicleNumber.error = error
        }
    }
    
    /**
     * Setup vehicle type dropdown with options
     */
    private fun setupVehicleTypeDropdown() {
        val vehicleTypes = arrayOf(
            getString(R.string.vehicle_type_bike),
            getString(R.string.vehicle_type_scooter),
            getString(R.string.vehicle_type_car)
        )
        
        val adapter = ArrayAdapter(this, android.R.layout.simple_dropdown_item_1line, vehicleTypes)
        binding.autoCompleteVehicleType.setAdapter(adapter)
    }
    
    /**
     * Perform registration with entered information
     */
    private fun performRegistration() {
        val email = binding.editTextEmail.text.toString().trim()
        val password = binding.editTextPassword.text.toString().trim()
        val confirmPassword = binding.editTextConfirmPassword.text.toString().trim()
        val fullName = binding.editTextFullName.text.toString().trim()
        val phone = binding.editTextPhone.text.toString().trim()
        val vehicleType = binding.autoCompleteVehicleType.text.toString().trim()
        val vehicleNumber = binding.editTextVehicleNumber.text.toString().trim().uppercase()
        
        viewModel.register(email, password, confirmPassword, fullName, phone, vehicleType, vehicleNumber)
    }
    
    /**
     * Show/hide loading state
     */
    private fun showLoading(isLoading: Boolean) {
        binding.progressBar.visibility = if (isLoading) View.VISIBLE else View.GONE
        binding.buttonRegister.isEnabled = !isLoading
        binding.editTextEmail.isEnabled = !isLoading
        binding.editTextPassword.isEnabled = !isLoading
        binding.editTextConfirmPassword.isEnabled = !isLoading
        binding.editTextFullName.isEnabled = !isLoading
        binding.editTextPhone.isEnabled = !isLoading
        binding.autoCompleteVehicleType.isEnabled = !isLoading
        binding.editTextVehicleNumber.isEnabled = !isLoading
    }
    
    /**
     * Navigate to main activity after successful registration
     */
    private fun navigateToMain() {
        val intent = Intent(this, MainActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        finish()
    }
    
    override fun onDestroy() {
        super.onDestroy()
        viewModel.clearRegisterState()
    }
}
