package com.grocery.customer.data.remote.dto

import com.google.gson.annotations.SerializedName

/**
 * Request for POST /api/sync/resolve endpoint
 * Used to resolve conflicts between local and server state
 */
data class SyncResolveRequest(
    @SerializedName("entity") val entity: String, // "cart", "orders", or "profile"
    @SerializedName("local_state") val localState: Any, // The local state data
    @SerializedName("local_timestamp") val localTimestamp: String // ISO 8601 timestamp
)

/**
 * Response from POST /api/sync/resolve endpoint
 * Contains resolved state and action taken
 */
data class SyncResolveResponse(
    @SerializedName("success") val success: Boolean,
    @SerializedName("data") val data: SyncResolveData,
    @SerializedName("timestamp") val timestamp: String // Server timestamp
)

data class SyncResolveData(
    @SerializedName("resolved_state") val resolvedState: Any, // The resolved state data
    @SerializedName("action") val action: String, // "local_wins", "server_wins", or "no_conflict"
    @SerializedName("timestamp") val timestamp: String // Server timestamp
)

/**
 * Enum for sync resolution actions
 */
enum class SyncAction(val value: String) {
    LOCAL_WINS("local_wins"),
    SERVER_WINS("server_wins"),
    NO_CONFLICT("no_conflict");

    companion object {
        fun fromString(value: String): SyncAction {
            return values().find { it.value == value } ?: NO_CONFLICT
        }
    }
}

/**
 * Entity types for sync
 */
enum class SyncEntity(val value: String) {
    CART("cart"),
    ORDERS("orders"),
    PROFILE("profile");

    override fun toString(): String = value
}
