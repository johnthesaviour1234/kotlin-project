package com.grocery.admin.data.exceptions

/**
 * Exception thrown when the authentication token has expired.
 * This signals that the user needs to login again.
 */
class TokenExpiredException(message: String) : Exception(message)
