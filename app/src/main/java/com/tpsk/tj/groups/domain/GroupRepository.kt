package com.tpsk.tj.groups.domain

import com.tpsk.tj.chat.data.api.GroupInfoResponse
import com.tpsk.tj.chat.model.ChatMessage
import com.tpsk.tj.common.util.Resource
import kotlinx.coroutines.flow.Flow

interface GroupRepository {
    fun observeGroups(): Flow<Resource<List<ChatMessage>>>
    suspend fun syncGroups(): Boolean
    fun createGroup(name: String, contactIds: List<String>): Flow<Resource<ChatMessage>>
    fun getGroupInfo(groupId: String): Flow<Resource<GroupInfoResponse>>
    fun addGroupMember(groupId: String, userId: String): Flow<Resource<Boolean>>
    fun removeGroupMember(groupId: String, userId: String): Flow<Resource<Boolean>>
    fun promoteToAdmin(groupId: String, userId: String): Flow<Resource<Boolean>>
    fun demoteAdmin(groupId: String, userId: String): Flow<Resource<Boolean>>
    fun updateGroupAvatar(groupId: String, avatarUrl: String): Flow<Resource<Boolean>>
    fun renameGroup(groupId: String, name: String): Flow<Resource<Boolean>>
    fun leaveGroup(groupId: String): Flow<Resource<Boolean>>
    fun generateGroupInviteCode(groupId: String): Flow<Resource<String>>
    fun joinGroupByInvite(inviteCode: String): Flow<Resource<String>>
}
