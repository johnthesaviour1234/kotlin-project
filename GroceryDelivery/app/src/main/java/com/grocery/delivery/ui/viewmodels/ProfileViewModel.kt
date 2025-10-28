package com.grocery.delivery.ui.viewmodels

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.grocery.delivery.data.dto.DriverProfile
import com.grocery.delivery.data.dto.ProfileResponse
import com.grocery.delivery.data.repository.DeliveryRepository
import com.grocery.delivery.utils.Resource
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import javax.inject.Inject

/**
 * ViewModel for Profile screen
 * Handles profile data loading, validation, and updates
 */
@HiltViewModel
class ProfileViewModel @Inject constructor(
    private val deliveryRepository: DeliveryRepository
) : ViewModel() {
    
    // Profile data state
    private val _profileState = MutableLiveData<Resource<ProfileResponse>>()
    val profileState: LiveData<Resource<ProfileResponse>> = _profileState
    
    // Profile update state
    private val _updateState = MutableLiveData<Resource<DriverProfile>>()
    val updateState: LiveData<Resource<DriverProfile>> = _updateState
    
    // Form validation state
    private val _validationErrors = MutableLiveData<ValidationErrors>()
    val validationErrors: LiveData<ValidationErrors> = _validationErrors
    
    // Track if form is dirty (has unsaved changes)
    private val _isDirty = MutableLiveData(false)
    val isDirty: LiveData<Boolean> = _isDirty
    
    init {
        loadProfile()
    }
    
    /**
     * Load profile data from backend
     */
    fun loadProfile() {
        deliveryRepository.getProfile()
            .onEach { resource ->
                _profileState.value = resource
            }
            .launchIn(viewModelScope)
    }
    
    /**
     * Refresh profile data
     */
    fun refreshProfile() {
        loadProfile()
    }
    
    /**
     * Validate and update profile
     */
    fun updateProfile(fullName: String, phone: String) {
        // Clear previous validation errors
        _validationErrors.value = ValidationErrors()
        
        // Validate inputs
        val errors = validateInputs(fullName, phone)
        
        if (errors.hasErrors()) {
            _validationErrors.value = errors
            return
        }
        
        // Proceed with update
        deliveryRepository.updateProfile(
            fullName = fullName.trim(),
            phone = phone.trim()
        )
            .onEach { resource ->
                _updateState.value = resource
                
                // If successful, reload profile to get latest data
                if (resource is Resource.Success) {
                    _isDirty.value = false
                    loadProfile()
                }
            }
            .launchIn(viewModelScope)
    }
    
    /**
     * Mark form as dirty (has unsaved changes)
     */
    fun markDirty() {
        _isDirty.value = true
    }
    
    /**
     * Clear dirty flag
     */
    fun clearDirty() {
        _isDirty.value = false
    }
    
    /**
     * Clear update state (after showing success/error message)
     */
    fun clearUpdateState() {
        _updateState.value = null
    }
    
    /**
     * Validate all input fields
     */
    private fun validateInputs(fullName: String, phone: String): ValidationErrors {
        val errors = ValidationErrors()
        
        // Validate full name
        when {
            fullName.isBlank() -> {
                errors.fullNameError = "Name cannot be empty"
            }
            fullName.trim().length < MIN_NAME_LENGTH -> {
                errors.fullNameError = "Name must be at least $MIN_NAME_LENGTH characters"
            }
            fullName.trim().length > MAX_NAME_LENGTH -> {
                errors.fullNameError = "Name cannot exceed $MAX_NAME_LENGTH characters"
            }
            !fullName.trim().matches(NAME_REGEX) -> {
                errors.fullNameError = "Name can only contain letters and spaces"
            }
        }
        
        // Validate phone
        when {
            phone.isBlank() -> {
                errors.phoneError = "Phone number cannot be empty"
            }
            !phone.trim().matches(PHONE_REGEX) -> {
                errors.phoneError = "Phone number must be 10 digits"
            }
        }
        
        return errors
    }
    
    /**
     * Validation errors data class
     */
    data class ValidationErrors(
        var fullNameError: String? = null,
        var phoneError: String? = null
    ) {
        fun hasErrors(): Boolean {
            return fullNameError != null || phoneError != null
        }
    }
    
    companion object {
        // Validation constants
        private const val MIN_NAME_LENGTH = 2
        private const val MAX_NAME_LENGTH = 100
        
        // Name can only contain letters and spaces
        private val NAME_REGEX = Regex("^[a-zA-Z\\s]+$")
        
        // Phone must be 10 digits (US format)
        private val PHONE_REGEX = Regex("^[0-9]{10}$")
    }
}
