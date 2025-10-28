package com.grocery.delivery.ui.viewmodels

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.grocery.delivery.data.dto.DeliveryLoginResponse
import com.grocery.delivery.data.repository.AuthRepository
import com.grocery.delivery.utils.Resource
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import javax.inject.Inject

/**
 * ViewModel for login screen
 */
@HiltViewModel
class LoginViewModel @Inject constructor(
    private val authRepository: AuthRepository
) : ViewModel() {
    
    private val _loginState = MutableLiveData<Resource<DeliveryLoginResponse>>()
    val loginState: LiveData<Resource<DeliveryLoginResponse>> = _loginState
    
    private val _emailError = MutableLiveData<String?>()
    val emailError: LiveData<String?> = _emailError
    
    private val _passwordError = MutableLiveData<String?>()
    val passwordError: LiveData<String?> = _passwordError
    
    /**
     * Check if user is already logged in
     */
    fun isLoggedIn(): Boolean {
        return authRepository.isLoggedIn()
    }
    
    /**
     * Login with email and password
     */
    fun login(email: String, password: String) {
        // Reset errors
        _emailError.value = null
        _passwordError.value = null
        
        // Validate inputs
        if (!validateInputs(email, password)) {
            return
        }
        
        // Perform login
        authRepository.login(email, password)
            .onEach { resource ->
                _loginState.value = resource
            }
            .launchIn(viewModelScope)
    }
    
    /**
     * Validate email and password inputs
     */
    private fun validateInputs(email: String, password: String): Boolean {
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
        
        return isValid
    }
    
    /**
     * Clear login state
     */
    fun clearLoginState() {
        _loginState.value = null
    }
}
