package com.tpsk.tj.direct.data

import com.tpsk.tj.chat.data.api.ChatApi
import com.tpsk.tj.chat.data.api.InitiateDMRequest
import com.tpsk.tj.chat.data.local.dao.CallLogDao
import com.tpsk.tj.chat.data.local.dao.ChatDao
import com.tpsk.tj.chat.data.local.dao.ContactDao
import com.tpsk.tj.chat.data.local.entity.ChatEntity
import com.tpsk.tj.chat.data.repository.mapApiCallLog
import com.tpsk.tj.chat.data.repository.mapApiChatSummary
import com.tpsk.tj.chat.data.repository.toEntity
import com.tpsk.tj.chat.data.repository.toModel
import com.tpsk.tj.common.domain.auth.SessionManager
import com.tpsk.tj.chat.model.CallLog
import com.tpsk.tj.chat.model.ChatMessage
import com.tpsk.tj.common.data.realtime.WebSocketManager
import com.tpsk.tj.common.model.ConversationType
import com.tpsk.tj.common.model.User
import com.tpsk.tj.common.util.DisplayNameResolver
import com.tpsk.tj.common.util.sanitizeDisplayName
import com.tpsk.tj.common.util.Resource
import com.tpsk.tj.direct.domain.DirectRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DirectRepositoryImpl @Inject constructor(
    private val chatApi: ChatApi,
    private val chatDao: ChatDao,
    private val callLogDao: CallLogDao,
    private val contactDao: ContactDao,
    private val webSocketManager: WebSocketManager,
    private val displayNameResolver: DisplayNameResolver,
    private val sessionManager: SessionManager
) : DirectRepository {

    override fun observeDirectChats(): Flow<Resource<List<ChatMessage>>> = flow {
        val cached = chatDao.getChatsByTypeOnce(ConversationType.DIRECT).map { it.toModel() }
        emit(if (cached.isNotEmpty()) Resource.Loading(cached) else Resource.Loading())
        syncDirectChats()
        emitAll(
            chatDao.getChatsByType(ConversationType.DIRECT).map { list ->
                val deduped = list.distinctBy { entity ->
                    if (!entity.otherUserId.isNullOrBlank()) "direct_${entity.otherUserId}" else entity.id
                }
                Resource.Success(deduped.map { it.toModel() })
            }
        )
    }

    override suspend fun syncDirectChats(): Boolean {
        return try {
            val response = chatApi.getChats()
            if (!response.isSuccessful) return false
            val directs = response.body()?.chats.orEmpty()
                .map { mapApiChatSummary(it) }
                .filter { it.type == ConversationType.DIRECT }
            if (directs.isNotEmpty()) {
                chatDao.insertChats(directs.map { it.toEntity() })
            }
            true
        } catch (e: Exception) {
            if (e is CancellationException) throw e
            false
        }
    }

    override fun observeCallLogs(): Flow<Resource<List<CallLog>>> = flow {
        val cached = callLogDao.getCallLogsOnce()
        emit(if (cached.isNotEmpty()) Resource.Loading(cached.map { it.toModel() }) else Resource.Loading())
        syncCallLogs()
        emitAll(callLogDao.getCallLogs().map { list -> Resource.Success(list.map { it.toModel() }) })
    }

    private suspend fun syncCallLogs() {
        try {
            val response = chatApi.getCallLogs()
            if (!response.isSuccessful) return
            val me = sessionManager.getUserId().orEmpty()
            val logs = response.body()?.calls.orEmpty().mapNotNull { api ->
                val mapped = mapApiCallLog(api)
                if (mapped.id.isBlank() || mapped.sessionId.isBlank()) return@mapNotNull null
                val otherId = when {
                    me.isNotBlank() && mapped.callerId == me -> mapped.calleeId
                    me.isNotBlank() && mapped.calleeId == me -> mapped.callerId
                    mapped.contactId.isNotBlank() -> mapped.contactId
                    else -> mapped.calleeId.ifBlank { mapped.callerId }
                }
                val contact = if (otherId.isBlank()) null else contactDao.getContactById(otherId)
                val name = mapped.contactName.ifBlank {
                    contact?.displayName?.takeIf { it.isNotBlank() }
                        ?: contact?.username?.takeIf { it.isNotBlank() }
                        ?: "Unknown"
                }
                val status = when (mapped.status.lowercase()) {
                    "missed", "rejected" -> "missed"
                    else -> if (me.isNotBlank() && mapped.callerId == me) "outgoing" else "incoming"
                }
                mapped.copy(
                    contactId = otherId,
                    contactName = name,
                    contactAvatar = mapped.contactAvatar ?: contact?.avatarUrl,
                    status = status
                )
            }
            callLogDao.clearCallLogs()
            if (logs.isNotEmpty()) callLogDao.insertCallLogs(logs.map { it.toEntity() })
        } catch (e: Exception) {
            if (e is CancellationException) throw e
        }
    }

    override suspend fun initiateDM(contactId: String): Flow<Resource<String>> = flow {
        emit(Resource.Loading())
        try {
            val response = chatApi.initiateDM(InitiateDMRequest(contactId))
            if (response.isSuccessful && response.body() != null) {
                val conversationId = response.body()!!.conversation_id
                val contact = contactDao.getContactById(contactId)
                val resolvedName = displayNameResolver.resolve(
                    User(id = contactId, displayName = contact?.displayName ?: "", phoneNumber = contact?.phoneNumber ?: ""),
                    contact?.phoneNumber ?: ""
                )
                val sanitizedName = sanitizeDisplayName(resolvedName, contactId, contact?.phoneNumber)
                chatDao.insertChats(
                    listOf(
                        ChatEntity(
                            id = conversationId,
                            type = ConversationType.DIRECT,
                            senderName = sanitizedName,
                            lastMessage = "",
                            timestamp = System.currentTimeMillis(),
                            unreadCount = 0,
                            avatarUrl = contact?.avatarUrl,
                            otherUserId = contactId,
                            isOnline = false,
                            isMuted = false,
                            isPinned = false,
                            isTyping = false,
                            phoneNumber = contact?.phoneNumber
                        )
                    )
                )
                webSocketManager.send("subscribe", buildJsonObject { put("topic", "chat:$conversationId") })
                emit(Resource.Success(conversationId))
            } else {
                emit(Resource.Error("Failed to initiate DM"))
            }
        } catch (e: Exception) {
            if (e is CancellationException) throw e
            emit(Resource.Error(e.message ?: "Error"))
        }
    }

    override fun pinChat(chatId: String, isPinned: Boolean): Flow<Resource<Boolean>> = flow {
        chatDao.updatePinned(chatId, isPinned)
        emit(Resource.Success(true))
    }

    override fun muteChat(chatId: String, isMuted: Boolean): Flow<Resource<Boolean>> = flow {
        chatDao.updateMuted(chatId, isMuted)
        emit(Resource.Success(true))
    }

    override fun deleteChat(chatId: String): Flow<Resource<Boolean>> = flow {
        try {
            chatApi.deleteConversation(chatId)
            chatDao.deleteChat(chatId)
            emit(Resource.Success(true))
        } catch (e: Exception) {
            emit(Resource.Error(e.message ?: "Error"))
        }
    }
}
