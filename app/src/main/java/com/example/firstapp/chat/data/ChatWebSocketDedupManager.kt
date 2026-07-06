package com.example.firstapp.chat.data

import android.util.Log
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import okhttp3.WebSocket
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.CopyOnWriteArrayList
import com.example.firstapp.domain.model.Message

/**
 * ChatWebSocketDedupManager implements CLIENT-SIDE DEDUPLICATION for real-time messaging.
 *
 * When a message is created locally:
 * 1. Generate client_uuid BEFORE sending to server
 * 2. Store in pendingMessages with status "sending"
 * 3. Emit to UI immediately
 *
 * When WebSocket receives a message:
 * 1. Check if message.client_uuid exists in pendingMessages
 * 2. If YES: This is our echo-back, update status to "sent", do NOT append again
 * 3. If NO: This is from someone else, append to feed and mark confirmed
 *
 * This prevents the Android app from showing duplicate messages during temporary network lag.
 */
class ChatWebSocketDedupManager {

    private val _newMessagesFlow = MutableSharedFlow<Message>(extraBufferCapacity = 32)
    val newMessagesFlow: Flow<Message> = _newMessagesFlow.asSharedFlow()

    private val _messageStatusUpdates = MutableSharedFlow<MessageStatusUpdate>(extraBufferCapacity = 32)
    val messageStatusUpdates: Flow<MessageStatusUpdate> = _messageStatusUpdates.asSharedFlow()

    // Track pending messages by client_uuid → local message object
    private val pendingMessages = ConcurrentHashMap<String, PendingMessage>()

    // Track confirmed server message IDs to prevent re-adding
    private val confirmedServerIds = CopyOnWriteArrayList<String>()

    data class PendingMessage(
        val clientUuid: String,
        val conversationId: String,
        val text: String,
        val status: String = "sending", // sending | sent | failed
        val createdAt: Long = System.currentTimeMillis()
    )

    data class MessageStatusUpdate(
        val clientUuid: String,
        val serverId: String?,
        val status: String, // sending | sent | failed
        val timestamp: Long = System.currentTimeMillis()
    )

    /**
     * Called BEFORE sending a message to the server.
     * Generates client_uuid and tracks message locally.
     */
    fun createOutgoingMessage(conversationId: String, text: String): String {
        val clientUuid = UUID.randomUUID().toString()

        // Create pending message locally
        val pendingMsg = PendingMessage(
            clientUuid = clientUuid,
            conversationId = conversationId,
            text = text,
            status = "sending"
        )

        pendingMessages[clientUuid] = pendingMsg

        // Emit to UI immediately with "sending" status
        val uiMessage = Message(
            id = "", // No server ID yet
            clientUuid = clientUuid,
            conversationId = conversationId,
            text = text,
            status = "sending",
            createdAt = System.currentTimeMillis()
        )

        _newMessagesFlow.tryEmit(uiMessage)

        Log.d("ChatDedup", "Created outgoing message: uuid=$clientUuid status=sending")

        return clientUuid
    }

    /**
     * Called when WebSocket receives a message payload.
     * Implements core deduplication logic.
     */
    fun handleIncomingMessage(payload: IncomingMessagePayload) {
        try {
            val clientUuid = payload.clientUuid
            val serverId = payload.id
            val conversationId = payload.conversationId

            // DEDUPLICATION CHECK
            if (clientUuid != null && clientUuid in pendingMessages) {
                // This is OUR locally-sent message echoed back from server
                val pending = pendingMessages.remove(clientUuid)
                if (pending != null) {
                    // Update status to "sent" and link server ID
                    _messageStatusUpdates.tryEmit(
                        MessageStatusUpdate(
                            clientUuid = clientUuid,
                            serverId = serverId,
                            status = "sent"
                        )
                    )

                    confirmedServerIds.add(serverId)

                    Log.d(
                        "ChatDedup",
                        "Deduped message: clientUuid=$clientUuid serverId=$serverId status=sent"
                    )

                    // Return early - DO NOT append to feed again
                    return
                }
            }

            // Not in pending → NEW message from someone else or server-generated
            if (serverId != null && serverId in confirmedServerIds) {
                // Already processed this message (shouldn't happen but just in case)
                Log.d("ChatDedup", "Ignoring duplicate server message: $serverId")
                return
            }

            // CLEAN APPEND: This is a new message from the network
            val newMsg = Message(
                id = serverId ?: UUID.randomUUID().toString(),
                clientUuid = clientUuid,
                conversationId = conversationId,
                text = payload.text,
                senderId = payload.senderId,
                senderName = payload.senderName,
                status = "received",
                createdAt = payload.createdAt
            )

            confirmedServerIds.add(newMsg.id)
            _newMessagesFlow.tryEmit(newMsg)

            Log.d(
                "ChatDedup",
                "New message from network: id=${newMsg.id} sender=${payload.senderName}"
            )

        } catch (e: Exception) {
            Log.e("ChatDedup", "Failed to handle incoming message", e)
        }
    }

    /**
     * Called when message send fails (network error, server error, etc.)
     */
    fun markMessageFailed(clientUuid: String) {
        if (clientUuid in pendingMessages) {
            pendingMessages[clientUuid] = pendingMessages[clientUuid]!!.copy(status = "failed")

            _messageStatusUpdates.tryEmit(
                MessageStatusUpdate(
                    clientUuid = clientUuid,
                    serverId = null,
                    status = "failed"
                )
            )

            Log.w("ChatDedup", "Message marked failed: $clientUuid")
        }
    }

    /**
     * Retry sending a failed message with same client_uuid.
     */
    fun retryFailedMessage(clientUuid: String): Boolean {
        if (clientUuid in pendingMessages) {
            val msg = pendingMessages[clientUuid]!!
            pendingMessages[clientUuid] = msg.copy(status = "sending")

            _messageStatusUpdates.tryEmit(
                MessageStatusUpdate(
                    clientUuid = clientUuid,
                    serverId = null,
                    status = "sending"
                )
            )

            Log.d("ChatDedup", "Retrying message: $clientUuid")
            return true
        }

        return false
    }

    /**
     * Get pending message count (for UI debugging/indicators).
     */
    fun getPendingCount(): Int = pendingMessages.size

    /**
     * Get confirmed message count (for statistics).
     */
    fun getConfirmedCount(): Int = confirmedServerIds.size

    /**
     * Clear old confirmed IDs to prevent unbounded memory growth.
     * Call periodically (e.g., when conversation changes).
     */
    fun clearOldConfirmed() {
        confirmedServerIds.clear()
    }

    /**
     * Parse incoming WebSocket message JSON.
     */
    fun parseIncomingMessage(json: String): IncomingMessagePayload? {
        return try {
            // Use your JSON library (Gson, kotlinx.serialization, etc.)
            // This is a placeholder - implement with your actual JSON parsing
            parseMessageFromJson(json)
        } catch (e: Exception) {
            Log.e("ChatDedup", "Failed to parse message JSON", e)
            null
        }
    }

    private fun parseMessageFromJson(json: String): IncomingMessagePayload? {
        // TODO: Implement with your JSON library
        // Example with Gson:
        // val gson = Gson()
        // return gson.fromJson(json, IncomingMessagePayload::class.java)
        return null
    }
}

/**
 * Represents a message received from WebSocket.
 */
data class IncomingMessagePayload(
    val id: String,
    val clientUuid: String?,
    val conversationId: String,
    val senderId: String,
    val senderName: String,
    val text: String,
    val createdAt: Long = System.currentTimeMillis()
)
