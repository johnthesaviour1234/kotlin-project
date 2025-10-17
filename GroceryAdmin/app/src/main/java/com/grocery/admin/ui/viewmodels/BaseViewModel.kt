package com.grocery.admin.ui.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.grocery.admin.util.Resource
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * Base ViewModel class that provides common functionality for all ViewModels.
 * Handles loading states, error handling, and coroutine management.
 */
abstract class BaseViewModel : ViewModel() {

    protected val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    protected val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    /**
     * Execute a suspend function with automatic loading state management.
     * 
     * @param dispatcher The coroutine dispatcher to use (defaults to IO)
     * @param block The suspend function to execute
     */
    protected fun executeWithLoading(
        dispatcher: CoroutineDispatcher = Dispatchers.IO,
        block: suspend () -> Unit
    ) {
        viewModelScope.launch(dispatcher) {
            _isLoading.value = true
            _error.value = null
            
            try {
                block()
            } catch (exception: Exception) {
                handleError(exception)
            } finally {
                _isLoading.value = false
            }
        }
    }

    /**
     * Execute a suspend function that returns a Resource with automatic state management.
     * 
     * @param dispatcher The coroutine dispatcher to use
     * @param stateFlow The MutableStateFlow to update with the result
     * @param block The suspend function that returns a Resource
     */
    protected fun <T> executeWithResource(
        dispatcher: CoroutineDispatcher = Dispatchers.IO,
        stateFlow: MutableStateFlow<Resource<T>>,
        block: suspend () -> Resource<T>
    ) {
        viewModelScope.launch(dispatcher) {
            stateFlow.value = Resource.Loading()
            _error.value = null
            
            try {
                val result = block()
                stateFlow.value = result
                
                if (result is Resource.Error) {
                    _error.value = result.message
                }
            } catch (exception: Exception) {
                val errorMessage = handleError(exception)
                stateFlow.value = Resource.Error(errorMessage)
            }
        }
    }

    /**
     * Handle errors and return appropriate error message.
     * Can be overridden by child ViewModels for custom error handling.
     */
    protected open fun handleError(exception: Exception): String {
        val errorMessage = when (exception) {
            is java.net.UnknownHostException -> "Network error. Please check your connection."
            is java.net.SocketTimeoutException -> "Request timeout. Please try again."
            is retrofit2.HttpException -> {
                when (exception.code()) {
                    401 -> "Authentication error. Please login again."
                    403 -> "Access denied."
                    404 -> "Resource not found."
                    500 -> "Server error. Please try again later."
                    else -> "HTTP error: ${exception.code()}"
                }
            }
            else -> exception.message ?: "An unknown error occurred"
        }
        
        _error.value = errorMessage
        return errorMessage
    }

    /**
     * Clear error state.
     */
    fun clearError() {
        _error.value = null
    }
}
