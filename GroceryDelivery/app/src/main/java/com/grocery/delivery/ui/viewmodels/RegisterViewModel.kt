package com.grocery.delivery.ui.viewmodels

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.grocery.delivery.data.dto.DeliveryRegisterResponse
import com.grocery.delivery.data.repository.AuthRepository
import com.grocery.delivery.utils.Resource
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import javax.inject.Inject

/**
 * ViewModel for registration screen
 */
@HiltViewModel
class RegisterViewModel @Inject constructor(
    private val authRepository: AuthRepository
) : ViewModel() {
    
    private val _registerState = MutableLiveData<Resource<DeliveryRegisterResponse>>()
    val registerState: LiveData<Resource<DeliveryRegisterResponse>> = _registerState
    
    private val _emailError = MutableLiveData<String?>()
    val emailError: LiveData<String?> = _emailError
    
    private val _passwordError = MutableLiveData<String?>()
    val passwordError: LiveData<String?> = _passwordError
    
    private val _confirmPasswordError = MutableLiveData<String?>()
    val confirmPasswordError: LiveData<String?> = _confirmPasswordError
    
    private val _fullNameError = MutableLiveData<String?>()
    val fullNameError: LiveData<String?> = _fullNameError
    
    private val _phoneError = MutableLiveData<String?>()
    val phoneError: LiveData<String?> = _phoneError
    
    private val _vehicleTypeError = MutableLiveData<String?>()
    val vehicleTypeError: LiveData<String?> = _vehicleTypeError
    
    private val _vehicleNumberError = MutableLiveData<String?>()
    val vehicleNumberError: LiveData<String?> = _vehicleNumberError
    
    /**
     * Register a new delivery driver
     */
    fun register(
        email: String,
        password: String,
        confirmPassword: String,
        fullName: String,
        phone: String,
        vehicleType: String,
        vehicleNumber: String
    ) {
        // Reset errors
        _emailError.value = null
        _passwordError.value = null
        _confirmPasswordError.value = null
        _fullNameError.value = null
        _phoneError.value = null
        _vehicleTypeError.value = null
        _vehicleNumberError.value = null
        
        // Validate inputs
        if (!validateInputs(email, password, confirmPassword, fullName, phone, vehicleType, vehicleNumber)) {
            return
        }
        
        // Perform registration
        authRepository.register(email, password, fullName, phone, vehicleType, vehicleNumber)
            .onEach { resource ->
                _registerState.value = resource
            }
            .launchIn(viewModelScope)
    }
    
    /**
     * Validate all registration inputs
     */
    private fun validateInputs(
        email: String,
        password: String,
        confirmPassword: String,
        fullName: String,
        phone: String,
        vehicleType: String,
        vehicleNumber: String
    ): Boolean {
        var isValid = true
        
        // Validate email
        if (email.isBlank()) {
            _emailError.value = "Email is required"
            isValid = false
        } else if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            _emailError.value = "Invalid email format"
            isValid = false
        }
        
        // Validate password
        if (password.isBlank()) {
            _passwordError.value = "Password is required"
            isValid = false
        } else if (password.length < 6) {
            _passwordError.value = "Password must be at least 6 characters"
            isValid = false
        }
        
        // Validate confirm password
        if (confirmPassword.isBlank()) {
            _confirmPasswordError.value = "Please confirm your password"
            isValid = false
        } else if (password != confirmPassword) {
            _confirmPasswordError.value = "Passwords do not match"
            isValid = false
        }
        
        // Validate full name
        if (fullName.isBlank()) {
            _fullNameError.value = "Full name is required"
            isValid = false
        } else if (fullName.length < 3) {
            _fullNameError.value = "Name must be at least 3 characters"
            isValid = false
        }
        
        // Validate phone
        if (phone.isBlank()) {
            _phoneError.value = "Phone number is required"
            isValid = false
        } else if (phone.length < 10) {
            _phoneError.value = "Invalid phone number"
            isValid = false
        }
        
        // Validate vehicle type
        if (vehicleType.isBlank()) {
            _vehicleTypeError.value = "Vehicle type is required"
            isValid = false
        }
        
        // Validate vehicle number
        if (vehicleNumber.isBlank()) {
            _vehicleNumberError.value = "Vehicle number is required"
            isValid = false
        }
        
        return isValid
    }
    
    /**
     * Clear registration state
     */
    fun clearRegisterState() {
        _registerState.value = null
    }
}
