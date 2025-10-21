package com.grocery.customer.ui.viewmodels

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.grocery.customer.data.remote.dto.*
import com.grocery.customer.domain.repository.AuthRepository
import com.grocery.customer.domain.repository.ProductRepository
import com.grocery.customer.util.Resource
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * ViewModel for ProfileFragment
 * Handles user profile management, addresses, and account settings
 */
@HiltViewModel
class ProfileViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    private val productRepository: ProductRepository
) : BaseViewModel() {

    private val _userProfile = MutableLiveData<Resource<UserProfile>>(Resource.Loading())
    val userProfile: LiveData<Resource<UserProfile>> = _userProfile

    private val _userAddresses = MutableLiveData<Resource<List<UserAddress>>>(Resource.Loading())
    val userAddresses: LiveData<Resource<List<UserAddress>>> = _userAddresses

    private val _profileUpdateResult = MutableLiveData<Resource<UserProfile>>()
    val profileUpdateResult: LiveData<Resource<UserProfile>> = _profileUpdateResult

    private val _passwordChangeResult = MutableLiveData<Resource<String>>()
    val passwordChangeResult: LiveData<Resource<String>> = _passwordChangeResult

    private val _addressOperationResult = MutableLiveData<Resource<UserAddress>>()
    val addressOperationResult: LiveData<Resource<UserAddress>> = _addressOperationResult

    companion object {
        private const val TAG = "ProfileViewModel"
    }

    init {
        loadUserProfile()
        loadUserAddresses()
    }

    fun loadUserProfile() {
        Log.d(TAG, "Loading user profile...")
        viewModelScope.launch {
            _userProfile.value = Resource.Loading()
            try {
                Log.d(TAG, "Calling authRepository.getUserProfile()")
                val result = authRepository.getUserProfile()
                result.fold(
                    onSuccess = { profile ->
                        Log.d(TAG, "Successfully loaded user profile: ${profile.email}")
                        _userProfile.value = Resource.Success(profile)
                    },
                    onFailure = { exception ->
                        Log.e(TAG, "Failed to load user profile: ${exception.message}", exception)
                        _userProfile.value = Resource.Error(
                            exception.message ?: "Failed to load profile"
                        )
                    }
                )
            } catch (exception: Exception) {
                Log.e(TAG, "Exception in loadUserProfile: ${exception.message}", exception)
                _userProfile.value = Resource.Error(
                    exception.message ?: "Unknown error occurred"
                )
            }
        }
    }

    fun loadUserAddresses() {
        Log.d(TAG, "Loading user addresses...")
        viewModelScope.launch {
            _userAddresses.value = Resource.Loading()
            try {
                Log.d(TAG, "Calling authRepository.getUserAddresses()")
                val result = authRepository.getUserAddresses()
                result.fold(
                    onSuccess = { addressPayload ->
                        Log.d(TAG, "Successfully loaded ${addressPayload.items.size} addresses")
                        _userAddresses.value = Resource.Success(addressPayload.items)
                    },
                    onFailure = { exception ->
                        Log.e(TAG, "Failed to load addresses: ${exception.message}", exception)
                        _userAddresses.value = Resource.Error(
                            exception.message ?: "Failed to load addresses"
                        )
                    }
                )
            } catch (exception: Exception) {
                Log.e(TAG, "Exception in loadUserAddresses: ${exception.message}", exception)
                _userAddresses.value = Resource.Error(
                    exception.message ?: "Unknown error occurred"
                )
            }
        }
    }

    fun updateProfile(request: ProfileUpdateRequest) {
        Log.d(TAG, "Updating user profile...")
        viewModelScope.launch {
            _profileUpdateResult.value = Resource.Loading()
            try {
                Log.d(TAG, "Calling authRepository.updateUserProfile()")
                val result = authRepository.updateUserProfile(request)
                result.fold(
                    onSuccess = { profile ->
                        Log.d(TAG, "Successfully updated user profile")
                        _profileUpdateResult.value = Resource.Success(profile)
                        _userProfile.value = Resource.Success(profile)
                    },
                    onFailure = { exception ->
                        Log.e(TAG, "Failed to update profile: ${exception.message}", exception)
                        _profileUpdateResult.value = Resource.Error(
                            exception.message ?: "Failed to update profile"
                        )
                    }
                )
            } catch (exception: Exception) {
                Log.e(TAG, "Exception in updateProfile: ${exception.message}", exception)
                _profileUpdateResult.value = Resource.Error(
                    exception.message ?: "Unknown error occurred"
                )
            }
        }
    }

    fun changePassword(currentPassword: String, newPassword: String) {
        Log.d(TAG, "Changing user password...")
        viewModelScope.launch {
            _passwordChangeResult.value = Resource.Loading()
            try {
                val request = ChangePasswordRequest(currentPassword, newPassword)
                Log.d(TAG, "Calling authRepository.changePassword()")
                val result = authRepository.changePassword(request)
                result.fold(
                    onSuccess = { response ->
                        Log.d(TAG, "Successfully changed password")
                        _passwordChangeResult.value = Resource.Success(
                            response["message"] ?: "Password changed successfully"
                        )
                    },
                    onFailure = { exception ->
                        Log.e(TAG, "Failed to change password: ${exception.message}", exception)
                        _passwordChangeResult.value = Resource.Error(
                            exception.message ?: "Failed to change password"
                        )
                    }
                )
            } catch (exception: Exception) {
                Log.e(TAG, "Exception in changePassword: ${exception.message}", exception)
                _passwordChangeResult.value = Resource.Error(
                    exception.message ?: "Unknown error occurred"
                )
            }
        }
    }

    fun createAddress(request: CreateAddressRequest) {
        Log.d(TAG, "Creating new address...")
        viewModelScope.launch {
            _addressOperationResult.value = Resource.Loading()
            try {
                Log.d(TAG, "Calling authRepository.createAddress()")
                val result = authRepository.createAddress(request)
                result.fold(
                    onSuccess = { address ->
                        Log.d(TAG, "Successfully created address: ${address.name}")
                        _addressOperationResult.value = Resource.Success(address)
                        loadUserAddresses() // Refresh address list
                    },
                    onFailure = { exception ->
                        Log.e(TAG, "Failed to create address: ${exception.message}", exception)
                        _addressOperationResult.value = Resource.Error(
                            exception.message ?: "Failed to create address"
                        )
                    }
                )
            } catch (exception: Exception) {
                Log.e(TAG, "Exception in createAddress: ${exception.message}", exception)
                _addressOperationResult.value = Resource.Error(
                    exception.message ?: "Unknown error occurred"
                )
            }
        }
    }

    fun updateAddress(addressId: String, request: UpdateAddressRequest) {
        Log.d(TAG, "Updating address: $addressId")
        viewModelScope.launch {
            _addressOperationResult.value = Resource.Loading()
            try {
                Log.d(TAG, "Calling authRepository.updateAddress()")
                val result = authRepository.updateAddress(addressId, request)
                result.fold(
                    onSuccess = { address ->
                        Log.d(TAG, "Successfully updated address: ${address.name}")
                        _addressOperationResult.value = Resource.Success(address)
                        loadUserAddresses() // Refresh address list
                    },
                    onFailure = { exception ->
                        Log.e(TAG, "Failed to update address: ${exception.message}", exception)
                        _addressOperationResult.value = Resource.Error(
                            exception.message ?: "Failed to update address"
                        )
                    }
                )
            } catch (exception: Exception) {
                Log.e(TAG, "Exception in updateAddress: ${exception.message}", exception)
                _addressOperationResult.value = Resource.Error(
                    exception.message ?: "Unknown error occurred"
                )
            }
        }
    }

    fun deleteAddress(addressId: String) {
        Log.d(TAG, "Deleting address: $addressId")
        viewModelScope.launch {
            try {
                Log.d(TAG, "Calling authRepository.deleteAddress()")
                val result = authRepository.deleteAddress(addressId)
                result.fold(
                    onSuccess = { response ->
                        Log.d(TAG, "Successfully deleted address")
                        loadUserAddresses() // Refresh address list
                    },
                    onFailure = { exception ->
                        Log.e(TAG, "Failed to delete address: ${exception.message}", exception)
                        _addressOperationResult.value = Resource.Error(
                            exception.message ?: "Failed to delete address"
                        )
                    }
                )
            } catch (exception: Exception) {
                Log.e(TAG, "Exception in deleteAddress: ${exception.message}", exception)
                _addressOperationResult.value = Resource.Error(
                    exception.message ?: "Unknown error occurred"
                )
            }
        }
    }

    fun logout() {
        Log.d(TAG, "Logging out user...")
        viewModelScope.launch {
            try {
                Log.d(TAG, "Calling authRepository.logout()")
                authRepository.logout()
                Log.d(TAG, "Successfully logged out")
            } catch (exception: Exception) {
                Log.e(TAG, "Exception in logout: ${exception.message}", exception)
            }
        }
    }

    fun clearResults() {
        _profileUpdateResult.value = null
        _passwordChangeResult.value = null
        _addressOperationResult.value = null
    }
}