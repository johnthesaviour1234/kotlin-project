package com.grocery.admin.ui.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.grocery.admin.domain.repository.AuthRepository
import com.grocery.admin.util.Resource
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class RegisterViewModel @Inject constructor(
    private val authRepository: AuthRepository
) : ViewModel() {

    private val _registerState = MutableStateFlow<Resource<Unit>?>(null)
    val registerState: StateFlow<Resource<Unit>?> = _registerState.asStateFlow()

    fun register(email: String, password: String, fullName: String, phone: String?) {
        viewModelScope.launch {
            authRepository.register(email, password, fullName, phone).collect { resource ->
                _registerState.value = when (resource) {
                    is Resource.Success -> Resource.Success(Unit)
                    is Resource.Error -> Resource.Error(resource.message ?: "Registration failed")
                    is Resource.Loading -> Resource.Loading()
                }
            }
        }
    }

    fun resetState() {
        _registerState.value = null
    }
}
