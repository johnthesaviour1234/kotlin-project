package com.grocery.delivery.util

/**
 * A generic wrapper class for handling different states of data.
 * Provides Success, Error, and Loading states for UI state management.
 */
sealed class Resource<T>(
    val data: T? = null,
    val message: String? = null
) {
    /**
     * Represents a successful state with data.
     */
    class Success<T>(data: T) : Resource<T>(data)

    /**
     * Represents an error state with optional data and error message.
     * @param isAuthError Indicates if this is an authentication error (401)
     */
    class Error<T>(message: String, data: T? = null, val isAuthError: Boolean = false) : Resource<T>(data, message)

    /**
     * Represents a loading state with optional data.
     */
    class Loading<T>(data: T? = null) : Resource<T>(data)
}