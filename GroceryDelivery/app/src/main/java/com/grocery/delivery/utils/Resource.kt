package com.grocery.delivery.utils

/**
 * A generic wrapper class for handling API responses and states
 * @param data The data payload (can be null)
 * @param message Optional message (typically used for errors)
 * @param isAuthError Flag indicating if this is an authentication error (401)
 */
sealed class Resource<T>(
    val data: T? = null,
    val message: String? = null,
    val isAuthError: Boolean = false
) {
    class Success<T>(data: T) : Resource<T>(data)
    class Error<T>(
        message: String, 
        data: T? = null,
        isAuthError: Boolean = false
    ) : Resource<T>(data, message, isAuthError)
    class Loading<T>(data: T? = null) : Resource<T>(data)
}
