package com.example.firstapp.chat.data

import android.util.Log
import com.example.firstapp.chat.model.Message
import java.util.concurrent.ConcurrentHashMap

/**
 * ChatWebSocketDedupManager implements client-side deduplication for realtime chat.
 *
 * It tracks outgoing client UUIDs so websocket echo messages from this device
 * are not appended a second time.
 *
 * The repository already handles optimistic pending insert and REST confirmation;
 * this manager only guards websocket duplicates and tracks simple send outcomes.
 */
class ChatWebSocketDedupManager {
    private val pendingOutgoing = ConcurrentHashMap<String, String>() // clientUuid -> conversationId
    private val seenServerMessageIds = ConcurrentHashMap.newKeySet<String>()

    fun trackOutgoing(clientUuid: String, conversationId: String) {
        pendingOutgoing[clientUuid] = conversationId
    }

    fun confirmOutgoing(clientUuid: String?) {
        if (!clientUuid.isNullOrBlank()) {
            pendingOutgoing.remove(clientUuid)
        }
    }

    fun failOutgoing(clientUuid: String?) {
        if (!clientUuid.isNullOrBlank()) {
            pendingOutgoing.remove(clientUuid)
        }
    }

    // Returns true when message should be appended to local feed.
    fun shouldProcessIncoming(message: Message): Boolean {
        val clientUuid = message.clientId
        if (!clientUuid.isNullOrBlank() && pendingOutgoing.containsKey(clientUuid)) {
            pendingOutgoing.remove(clientUuid)
            Log.d("ChatDedup", "Ignoring websocket echo for pending client_uuid=$clientUuid")
            return false
        }

        if (message.id.isNotBlank() && !seenServerMessageIds.add(message.id)) {
            Log.d("ChatDedup", "Ignoring duplicate server message id=${message.id}")
            return false
        }

        return true
    }

    fun clearConversationState(conversationId: String) {
        pendingOutgoing.entries.removeIf { it.value == conversationId }
    }
}
