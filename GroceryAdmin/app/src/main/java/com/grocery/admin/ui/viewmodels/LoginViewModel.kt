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
class LoginViewModel @Inject constructor(
    private val authRepository: AuthRepository
) : ViewModel() {

    private val _loginState = MutableStateFlow<Resource<Unit>?>(null)
    val loginState: StateFlow<Resource<Unit>?> = _loginState.asStateFlow()

    fun login(email: String, password: String) {
        viewModelScope.launch {
            authRepository.login(email, password).collect { resource ->
                _loginState.value = when (resource) {
                    is Resource.Success -> Resource.Success(Unit)
                    is Resource.Error -> Resource.Error(resource.message ?: "Login failed")
                    is Resource.Loading -> Resource.Loading()
                }
            }
        }
    }

    fun resetState() {
        _loginState.value = null
    }
}
