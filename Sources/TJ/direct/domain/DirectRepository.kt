package com.tpsk.tj.direct.domain

import com.tpsk.tj.chat.model.CallLog
import com.tpsk.tj.chat.model.ChatMessage
import com.tpsk.tj.common.util.Resource
import kotlinx.coroutines.flow.Flow

interface DirectRepository {
    fun observeDirectChats(): Flow<Resource<List<ChatMessage>>>
    suspend fun syncDirectChats(): Boolean
    fun observeCallLogs(): Flow<Resource<List<CallLog>>>
    suspend fun initiateDM(contactId: String): Flow<Resource<String>>
    fun pinChat(chatId: String, isPinned: Boolean): Flow<Resource<Boolean>>
    fun muteChat(chatId: String, isMuted: Boolean): Flow<Resource<Boolean>>
    fun deleteChat(chatId: String): Flow<Resource<Boolean>>
}
