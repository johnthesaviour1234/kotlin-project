package com.grocery.customer.domain.repository

import com.grocery.customer.data.remote.dto.SyncAction
import com.grocery.customer.data.remote.dto.SyncEntity
import com.grocery.customer.data.remote.dto.SyncStateResponse

/**
 * Repository interface for state synchronization
 */
interface SyncRepository {
    
    /**
     * Fetch current state from server for all entities (cart, orders, profile)
     * @return Result with SyncStateResponse containing all entity states with timestamps and checksums
     */
    suspend fun fetchServerState(): Result<SyncStateResponse>
    
    /**
     * Resolve conflict between local and server state for a specific entity
     * @param entity The entity type (cart, orders, or profile)
     * @param localState The local state data
     * @param localTimestamp The timestamp of local state in ISO 8601 format
     * @return Result with action taken (local_wins, server_wins, no_conflict) and resolved state
     */
    suspend fun resolveConflict(
        entity: SyncEntity,
        localState: Any,
        localTimestamp: String
    ): Result<SyncConflictResult>
    
    /**
     * Perform full sync: fetch server state and resolve conflicts for all entities
     * This is the main sync method called by background worker
     * @return Result with sync summary (what was synced, any errors)
     */
    suspend fun performFullSync(): Result<SyncSummary>
}

/**
 * Result of conflict resolution
 */
data class SyncConflictResult(
    val action: SyncAction,
    val resolvedState: Any,
    val timestamp: String
)

/**
 * Summary of full sync operation
 */
data class SyncSummary(
    val cartSynced: Boolean,
    val ordersSynced: Boolean,
    val profileSynced: Boolean,
    val cartAction: SyncAction?,
    val ordersAction: SyncAction?,
    val profileAction: SyncAction?,
    val errors: List<String> = emptyList(),
    val timestamp: String
)
