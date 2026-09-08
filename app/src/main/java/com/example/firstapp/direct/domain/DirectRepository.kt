package com.example.firstapp.direct.domain

import com.example.firstapp.chat.model.CallLog
import com.example.firstapp.chat.model.ChatMessage
import com.example.firstapp.common.util.Resource
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
