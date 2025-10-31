package com.grocery.customer.domain.exceptions

/**
 * Exception thrown when JWT token has expired
 * This signals that the user needs to re-authenticate
 */
class TokenExpiredException(message: String = "Session expired. Please login again.") : Exception(message)
