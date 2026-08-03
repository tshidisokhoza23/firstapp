package com.example.firstapp.chat.ui.components

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.material3.MaterialTheme
import com.example.firstapp.chat.model.Message
import com.example.firstapp.chat.model.ChatMessage
import com.example.firstapp.common.ui.theme.AppBackground
import kotlinx.coroutines.CoroutineScope

@Composable
fun ChatMessageList(
    modifier: Modifier = Modifier,
    messagesLazyListState: LazyListState,
    groupedMessages: Map<String, List<Message>>,
    messages: List<Message> = emptyList(),
    selectedMessages: List<String>,
    chat: ChatMessage,
    fontSize: Float,
    showReadReceipts: Boolean,
    animationsEnabled: Boolean,
    scope: CoroutineScope,
    messagesCount: Int,
    onMessageClick: (Message) -> Unit,
    onMessageLongClick: (Message) -> Unit,
    onReply: (Message) -> Unit,
    onMediaClick: (String, String) -> Unit
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        LazyColumn(
            state = messagesLazyListState,
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 8.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            item { Spacer(modifier = Modifier.height(12.dp)) }
            
            if (chat.publicKey != null) {
                item { EncryptionInfoBubble() }
            }
            
            for (date in groupedMessages.keys) {
                val dayMessages = groupedMessages[date] ?: emptyList<Message>()
                
                item(key = "header_$date") { 
                    ModernDateHeader(date) 
                }
                
                items(
                    items = dayMessages,
                    key = { it.clientId ?: it.id }
                ) { m: Message ->
                    val isSelected = selectedMessages.contains(m.id)
                    
                    val referencedMsg = remember(m.replyToId, messages) {
                        if (m.replyToId != null) {
                            var result: Message? = null
                            for (msg in messages) {
                                if (msg.id == m.replyToId) {
                                    result = msg
                                    break
                                }
                            }
                            result
                        } else null
                    }
                    
                    MessageItemUnique(
                        messageItem = m,
                        isSelected = isSelected,
                        referencedMessage = referencedMsg,
                        fontSize = fontSize,
                        showReadReceipts = showReadReceipts,
                        onClick = { onMessageClick(m) },
                        onLongClick = { onMessageLongClick(m) },
                        onReply = onReply,
                        onMediaClick = onMediaClick
                    )
                }
            }
            
            item { Spacer(modifier = Modifier.height(120.dp)) }
        }

        // Floating Scroll to Bottom
        val showScrollToBottom by remember { 
            derivedStateOf { 
                val firstVisible = messagesLazyListState.firstVisibleItemIndex
                val limit = if (messagesCount > 8) messagesCount - 8 else 0
                firstVisible < limit
            } 
        }
        
        if (showScrollToBottom) {
            Box(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(bottom = 16.dp, end = 16.dp)
            ) {
                ScrollToBottomButton(
                    state = messagesLazyListState, 
                    scope = scope, 
                    messageCount = messagesCount, 
                    animated = animationsEnabled
                )
            }
        }
    }
}
