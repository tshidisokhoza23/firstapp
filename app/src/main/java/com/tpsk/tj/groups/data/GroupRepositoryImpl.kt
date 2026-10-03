package com.tpsk.tj.groups.data

import com.tpsk.tj.chat.data.api.AddMemberRequest
import com.tpsk.tj.chat.data.api.ChatApi
import com.tpsk.tj.chat.data.api.CreateGroupRequest
import com.tpsk.tj.chat.data.api.GroupInfoResponse
import com.tpsk.tj.chat.data.local.dao.ChatDao
import com.tpsk.tj.chat.data.repository.toEntity
import com.tpsk.tj.chat.data.repository.toModel
import com.tpsk.tj.chat.model.ChatMessage
import com.tpsk.tj.common.model.ConversationType
import com.tpsk.tj.common.util.Resource
import com.tpsk.tj.groups.domain.GroupRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class GroupRepositoryImpl @Inject constructor(
    private val chatApi: ChatApi,
    private val chatDao: ChatDao
) : GroupRepository {

    override fun observeGroups(): Flow<Resource<List<ChatMessage>>> = flow {
        val cached = chatDao.getChatsByTypeOnce(ConversationType.GROUP).map { it.toModel() }
        emit(if (cached.isNotEmpty()) Resource.Loading(cached) else Resource.Loading())
        syncGroups()
        emitAll(
            chatDao.getChatsByType(ConversationType.GROUP).map { list ->
                Resource.Success(list.map { it.toModel() })
            }
        )
    }

    override suspend fun syncGroups(): Boolean {
        return try {
            val response = chatApi.getChats()
            if (!response.isSuccessful) return false
            val groups = response.body()?.chats.orEmpty()
                .map { com.tpsk.tj.chat.data.repository.mapApiChatSummary(it) }
                .filter { it.type == ConversationType.GROUP }
            if (groups.isNotEmpty()) {
                chatDao.insertChats(groups.map { it.toEntity() })
            }
            true
        } catch (e: Exception) {
            if (e is CancellationException) throw e
            false
        }
    }

    override fun createGroup(name: String, contactIds: List<String>): Flow<Resource<ChatMessage>> = flow {
        emit(Resource.Loading())
        try {
            val response = chatApi.createGroup(CreateGroupRequest(name, contactIds))
            if (response.isSuccessful && response.body() != null) {
                val convId = response.body()!!.conversation_id
                val group = ChatMessage(convId, ConversationType.GROUP, name, isGroup = true)
                chatDao.insertChats(listOf(group.toEntity()))
                emit(Resource.Success(group))
            } else emit(Resource.Error("Failed to create group"))
        } catch (e: Exception) {
            if (e is CancellationException) throw e
            emit(Resource.Error(e.message ?: "Error"))
        }
    }

    override fun getGroupInfo(groupId: String): Flow<Resource<GroupInfoResponse>> = flow {
        emit(Resource.Loading())
        try {
            val response = chatApi.getGroupInfo(groupId)
            if (response.isSuccessful && response.body() != null) {
                persistGroupInfo(groupId, response.body()!!)
                emit(Resource.Success(response.body()!!))
            } else emit(Resource.Error("Failed to load group"))
        } catch (e: Exception) {
            emit(Resource.Error(e.message ?: "Failed to load group"))
        }
    }

    override fun addGroupMember(groupId: String, userId: String): Flow<Resource<Boolean>> = flow {
        emit(Resource.Loading())
        try {
            val response = chatApi.addChatGroupMember(groupId, AddMemberRequest(userId))
            if (response.isSuccessful) emit(Resource.Success(true))
            else emit(Resource.Error("Only admins can add members"))
        } catch (e: Exception) {
            emit(Resource.Error(e.message ?: "Failed to add member"))
        }
    }

    override fun removeGroupMember(groupId: String, userId: String): Flow<Resource<Boolean>> = flow {
        emit(Resource.Loading())
        try {
            val response = chatApi.removeChatGroupMember(groupId, AddMemberRequest(userId))
            if (response.isSuccessful) emit(Resource.Success(true))
            else emit(Resource.Error("Only admins can remove members"))
        } catch (e: Exception) {
            emit(Resource.Error(e.message ?: "Failed to remove member"))
        }
    }

    override fun promoteToAdmin(groupId: String, userId: String): Flow<Resource<Boolean>> = flow {
        emit(Resource.Loading())
        try {
            val response = chatApi.promoteChatGroupAdmin(groupId, AddMemberRequest(userId))
            if (response.isSuccessful) emit(Resource.Success(true))
            else emit(Resource.Error("Only admins can promote members"))
        } catch (e: Exception) {
            emit(Resource.Error(e.message ?: "Failed to promote"))
        }
    }

    override fun demoteAdmin(groupId: String, userId: String): Flow<Resource<Boolean>> = flow {
        emit(Resource.Loading())
        try {
            val response = chatApi.demoteChatGroupAdmin(groupId, AddMemberRequest(userId))
            if (response.isSuccessful) emit(Resource.Success(true))
            else emit(Resource.Error("Only admins can dismiss admins"))
        } catch (e: Exception) {
            emit(Resource.Error(e.message ?: "Failed to demote"))
        }
    }

    override fun renameGroup(groupId: String, name: String): Flow<Resource<Boolean>> = flow {
        emit(Resource.Loading())
        val trimmed = name.trim()
        if (trimmed.isEmpty()) {
            emit(Resource.Error("Group name is required"))
            return@flow
        }
        try {
            val response = chatApi.renameGroup(groupId, com.tpsk.tj.chat.data.api.RenameGroupRequest(trimmed))
            if (response.isSuccessful) {
                rememberGroupName(groupId, trimmed)
                emit(Resource.Success(true))
            } else emit(Resource.Error("Only admins can rename the group"))
        } catch (e: Exception) {
            if (e is CancellationException) throw e
            emit(Resource.Error(e.message ?: "Failed to rename group"))
        }
    }

    override fun updateDescription(groupId: String, description: String): Flow<Resource<Boolean>> = flow {
        emit(Resource.Loading())
        val trimmed = description.trim()
        if (trimmed.isEmpty()) {
            emit(Resource.Error("Description is required"))
            return@flow
        }
        try {
            val response = chatApi.updateGroupDescription(groupId, com.tpsk.tj.chat.data.api.GroupDescriptionRequest(trimmed))
            if (response.isSuccessful) emit(Resource.Success(true))
            else emit(Resource.Error("Description was not saved"))
        } catch (e: Exception) {
            if (e is CancellationException) throw e
            emit(Resource.Error(e.message ?: "Description was not saved"))
        }
    }

    private suspend fun persistGroupInfo(requestedId: String, info: GroupInfoResponse) {
        val name = info.title?.takeIf { it.isNotBlank() } ?: info.name?.takeIf { it.isNotBlank() } ?: return
        rememberGroupName(requestedId, name, info.avatar_url, info.conversation_id, info.created_by)
    }

    private suspend fun rememberGroupName(
        requestedId: String,
        name: String,
        avatarUrl: String? = null,
        conversationId: String? = null,
        createdBy: String? = null
    ) {
        val ids = listOfNotNull(requestedId, conversationId).distinct()
        for (id in ids) {
            val existing = chatDao.searchChatsById(id) ?: continue
            chatDao.insertChat(
                existing.copy(
                    senderName = name,
                    avatarUrl = avatarUrl?.takeIf { it.isNotBlank() } ?: existing.avatarUrl,
                    createdBy = createdBy ?: existing.createdBy,
                    isGroup = true,
                    type = ConversationType.GROUP
                )
            )
        }
    }

    override fun updateGroupAvatar(groupId: String, avatarUrl: String): Flow<Resource<Boolean>> = flow {
        emit(Resource.Loading())
        try {
            val response = chatApi.updateGroupAvatar(
                groupId,
                com.tpsk.tj.chat.data.api.AvatarUrlRequest(avatarUrl)
            )
            if (response.isSuccessful) {
                runCatching { chatDao.updateAvatar(groupId, avatarUrl) }
                emit(Resource.Success(true))
            } else emit(Resource.Error("Only admins can update the group icon"))
        } catch (e: Exception) {
            emit(Resource.Error(e.message ?: "Failed to update icon"))
        }
    }

    override fun leaveGroup(groupId: String): Flow<Resource<Boolean>> = flow {
        emit(Resource.Loading())
        try {
            val response = chatApi.leaveChatGroup(groupId)
            if (response.isSuccessful) {
                runCatching { chatDao.deleteChat(groupId) }
                emit(Resource.Success(true))
            } else emit(Resource.Error("Failed to leave group"))
        } catch (e: Exception) {
            emit(Resource.Error(e.message ?: "Failed to leave group"))
        }
    }

    override fun generateGroupInviteCode(groupId: String): Flow<Resource<String>> = flow {
        emit(Resource.Loading())
        try {
            val response = chatApi.generateGroupInviteCode(groupId)
            val code = response.body()?.invite_code
            if (response.isSuccessful && !code.isNullOrBlank()) emit(Resource.Success(code))
            else emit(Resource.Error("Only admins can create invite links"))
        } catch (e: Exception) {
            emit(Resource.Error(e.message ?: "Failed to create invite link"))
        }
    }

    override fun joinGroupByInvite(inviteCode: String): Flow<Resource<String>> = flow {
        emit(Resource.Loading())
        try {
            val response = chatApi.joinGroupByInvite(inviteCode)
            val body = response.body()
            val joinedId = body?.conversation_id?.takeIf { it.isNotBlank() }
                ?: body?.group_id?.takeIf { it.isNotBlank() }
            if (response.isSuccessful && !joinedId.isNullOrBlank()) emit(Resource.Success(joinedId))
            else emit(Resource.Error("Invalid invite link"))
        } catch (e: Exception) {
            emit(Resource.Error(e.message ?: "Failed to join group"))
        }
    }
}
