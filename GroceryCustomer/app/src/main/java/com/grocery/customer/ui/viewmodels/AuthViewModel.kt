package com.grocery.customer.ui.viewmodels

import androidx.lifecycle.viewModelScope
import com.grocery.customer.data.local.TokenStore
import com.grocery.customer.data.remote.dto.LoginResponse
import com.grocery.customer.data.remote.dto.RegisterResponse
import com.grocery.customer.domain.usecase.LoginUseCase
import com.grocery.customer.domain.usecase.LogoutUseCase
import com.grocery.customer.domain.usecase.RegisterUseCase
import com.grocery.customer.domain.usecase.LogoutUseCase
import com.grocery.customer.domain.usecase.ResendVerificationUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

@HiltViewModel
class AuthViewModel @Inject constructor(
    private val loginUseCase: LoginUseCase,
    private val registerUseCase: RegisterUseCase,
    private val logoutUseCase: LogoutUseCase,
    private val resendVerificationUseCase: ResendVerificationUseCase,
    private val tokenStore: TokenStore
) : BaseViewModel() {

    private val _loginState = MutableStateFlow<Resource<LoginResponse>>(Resource.Loading())
    val loginState: StateFlow<Resource<LoginResponse>> = _loginState.asStateFlow()

    private val _registerState = MutableStateFlow<Resource<RegisterResponse>>(Resource.Loading())
    val registerState: StateFlow<Resource<RegisterResponse>> = _registerState.asStateFlow()

    private val _navigateTo = MutableStateFlow<Destination?>(null)
    val navigateTo: StateFlow<Destination?> = _navigateTo.asStateFlow()

    private val _resendState = MutableStateFlow<Resource<Unit>?>(null)
    val resendState: StateFlow<Resource<Unit>?> = _resendState.asStateFlow()

    fun checkAuth() {
        viewModelScope.launch(Dispatchers.IO) {
            val token = tokenStore.getAccessToken()
            _navigateTo.update { if (!token.isNullOrBlank()) Destination.Main else Destination.Login }
        }
    }

    fun login(email: String, password: String) {
        executeWithResource(stateFlow = _loginState) {
            val result = loginUseCase(email, password)
            result.fold(
                onSuccess = { Resource.Success(it) },
                onFailure = { Resource.Error(it.message ?: "Login failed") }
            )
        }
    }

    fun register(email: String, password: String, fullName: String, phone: String?, userType: String = "customer") {
        executeWithResource(stateFlow = _registerState) {
            val result = registerUseCase(email, password, fullName, phone, userType)
            result.fold(
                onSuccess = { Resource.Success(it) },
                onFailure = { Resource.Error(it.message ?: "Registration failed") }
            )
        }
    }

    fun resendVerification(email: String) {
        executeWithResource(stateFlow = _resendState) {
            resendVerificationUseCase(email).fold(
                onSuccess = { Resource.Success(Unit) },
                onFailure = { Resource.Error(it.message ?: "Failed to resend") }
            )
        }
    }

    fun logout() {
        viewModelScope.launch(Dispatchers.IO) {
            logoutUseCase()
            _navigateTo.update { Destination.Login }
        }
    }

    // Debug helper: clear any cached tokens to ensure fresh auth flow
    fun clearTokensForDebug() {
        viewModelScope.launch(Dispatchers.IO) {
            tokenStore.clear()
        }
    }
}

enum class Destination { Login, Main }
