package com.grocery.admin.data.sync

/**
 * Represents the connection state of Supabase Realtime WebSocket
 */
sealed class RealtimeConnectionState {
    object Disconnected : RealtimeConnectionState()
    object Connecting : RealtimeConnectionState()
    object Connected : RealtimeConnectionState()
    data class Error(val message: String) : RealtimeConnectionState()
}
