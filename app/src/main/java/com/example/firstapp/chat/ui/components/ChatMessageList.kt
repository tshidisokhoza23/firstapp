package com.example.firstapp.chat.ui.components

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.firstapp.chat.model.ChatMessage
import com.example.firstapp.chat.model.Message
import com.example.firstapp.common.model.ConversationType
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
    onMediaClick: (String, String) -> Unit,
    playingAudioUri: String? = null,
    onPlayAudio: (String?) -> Unit = {},
    highlightId: String? = null
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        if (messages.isEmpty()) {
            Column(
                modifier = Modifier.fillMaxSize().padding(32.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Icon(
                    imageVector = when (chat.type) {
                        ConversationType.DIRECT -> Icons.Default.ChatBubbleOutline
                        ConversationType.GROUP -> Icons.Default.Groups
                        ConversationType.CHANNEL -> Icons.Default.Campaign
                        else -> Icons.Default.Chat
                    },
                    contentDescription = null,
                    modifier = Modifier.size(80.dp),
                    tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)
                )
                Spacer(Modifier.height(16.dp))
                Text(
                    text = "No messages here yet",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = if (chat.type == ConversationType.CHANNEL) "This is the start of the ${chat.senderName} channel" else "Send a message to start the conversation",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )
            }
        }

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
                    key = { it.id }
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
                        isHighlighted = m.id == highlightId,
                        onClick = { onMessageClick(m) },
                        onLongClick = { onMessageLongClick(m) },
                        onReply = onReply,
                        onMediaClick = onMediaClick,
                        playingAudioUri = playingAudioUri,
                        onPlayAudio = onPlayAudio
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
