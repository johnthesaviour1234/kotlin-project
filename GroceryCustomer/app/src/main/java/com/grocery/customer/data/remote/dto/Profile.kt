package com.grocery.customer.data.remote.dto

data class UserProfile(
    val id: String,
    val email: String?,
    val full_name: String?,
    val phone: String?,
    val user_type: String?,
    val avatar_url: String?,
    val date_of_birth: String?,
    val address: Map<String, Any>?,
    val preferences: Map<String, Any>?,
    val created_at: String?,
    val updated_at: String?
)

data class ProfileUpdateRequest(
    val full_name: String? = null,
    val phone: String? = null,
    val date_of_birth: String? = null,
    val avatar_url: String? = null,
    val address: Map<String, Any>? = null,
    val preferences: Map<String, Any>? = null
)

data class UserAddress(
    val id: String,
    val user_id: String,
    val name: String,
    val street_address: String,
    val apartment: String?,
    val city: String,
    val state: String,
    val postal_code: String,
    val landmark: String?,
    val is_default: Boolean,
    val address_type: String,
    val created_at: String,
    val updated_at: String
)

data class AddressPayload(
    val items: List<UserAddress>
)

data class CreateAddressRequest(
    val name: String,
    val street_address: String,
    val apartment: String? = null,
    val city: String,
    val state: String,
    val postal_code: String,
    val landmark: String? = null,
    val address_type: String = "home",
    val is_default: Boolean = false
)

data class UpdateAddressRequest(
    val name: String,
    val street_address: String,
    val apartment: String? = null,
    val city: String,
    val state: String,
    val postal_code: String,
    val landmark: String? = null,
    val address_type: String = "home",
    val is_default: Boolean = false
)
