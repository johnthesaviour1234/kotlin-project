package com.grocery.customer.data.remote.dto

data class UserProfile(
    val id: String,
    val email: String?,
    val full_name: String?,
    val phone: String?,
    val user_type: String?,
    val avatar_url: String?,
    val created_at: String?,
    val updated_at: String?
)

data class ProfileUpdateRequest(
    val full_name: String? = null,
    val phone: String? = null
)
