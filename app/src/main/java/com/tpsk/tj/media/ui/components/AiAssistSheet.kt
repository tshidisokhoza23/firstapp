package com.tpsk.tj.media.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.tpsk.tj.common.data.api.AiAssistResponse
import com.tpsk.tj.common.data.repository.AiRepository
import com.tpsk.tj.common.util.Resource
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AiAssistSheet(
    initialText: String,
    aiRepository: AiRepository,
    onDismiss: () -> Unit,
    onApplyText: (newText: String, hashtags: List<String>, isAiImproved: Boolean) -> Unit
) {
    val coroutineScope = rememberCoroutineScope()
    var isLoading by remember { mutableStateOf(false) }
    var selectedAction by remember { mutableStateOf<String?>(null) }
    var resultText by remember { mutableStateOf(initialText) }
    var suggestedHashtags by remember { mutableStateOf<List<String>>(emptyList()) }
    var threadParts by remember { mutableStateOf<List<String>>(emptyList()) }
    var errorText by remember { mutableStateOf<String?>(null) }

    val options = listOf(
        "Improve writing" to "improve_writing",
        "Make shorter" to "make_shorter",
        "Make longer" to "make_longer",
        "Add humor" to "add_humor",
        "Make professional" to "make_professional",
        "Suggest hashtags" to "suggest_hashtags",
        "Fix grammar" to "fix_grammar",
        "Thread mode" to "thread"
    )

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = Color(0xFF1A1A1A),
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(bottom = 32.dp)
        ) {
            // Header with Gradient Title
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(bottom = 16.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.AutoAwesome,
                    contentDescription = null,
                    tint = Color(0xFF00E5FF),
                    modifier = Modifier.size(22.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "AI Assist ✨",
                    color = Color.White,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.weight(1f))
                IconButton(onClick = onDismiss) {
                    Icon(Icons.Default.Close, contentDescription = "Close", tint = Color.Gray)
                }
            }

            // Quick Actions Horizontal Scroll
            Text(
                text = "CHOOSE ENHANCEMENT",
                color = Color(0xFF00E5FF),
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.sp,
                modifier = Modifier.padding(bottom = 8.dp)
            )

            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                items(options) { (label, actionKey) ->
                    val isSelected = selectedAction == actionKey
                    Surface(
                        onClick = {
                            selectedAction = actionKey
                            isLoading = true
                            coroutineScope.launch {
                                val res = aiRepository.runAiAssist(actionKey, initialText)
                                isLoading = false
                                if (res is Resource.Success && res.data != null) {
                                    val data = res.data
                                    resultText = data.suggestedText
                                    suggestedHashtags = data.suggestedHashtags
                                    threadParts = data.threadParts
                                    errorText = if (data.source == "offline") "Rewritten from your text. No extra facts were added." else null
                                } else if (res is Resource.Error) {
                                    errorText = res.errorMessage
                                }
                            }
                        },
                        shape = RoundedCornerShape(20.dp),
                        color = if (isSelected) Color(0xFF00E5FF).copy(alpha = 0.2f) else Color(0xFF2A2A2A),
                        border = androidx.compose.foundation.BorderStroke(
                            width = 1.dp,
                            color = if (isSelected) Color(0xFF00E5FF) else Color(0xFF333333)
                        )
                    ) {
                        Text(
                            text = label,
                            color = if (isSelected) Color(0xFF00E5FF) else Color.White,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Result Preview Area
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xFF242424), RoundedCornerShape(16.dp))
                    .border(1.dp, Color(0xFF333333), RoundedCornerShape(16.dp))
                    .padding(16.dp)
            ) {
                if (isLoading) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(vertical = 12.dp)
                    ) {
                        CircularProgressIndicator(
                            color = Color(0xFF00E5FF),
                            modifier = Modifier.size(20.dp),
                            strokeWidth = 2.dp
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = "AI is refining your draft...",
                            color = Color.Gray,
                            fontSize = 14.sp
                        )
                    }
                } else {
                    Column {
                        Text(
                            text = resultText.ifBlank { initialText.ifBlank { "Type a prompt or write text above to see AI suggestions." } },
                            color = Color.White,
                            fontSize = 15.sp,
                            lineHeight = 22.sp
                        )

                        if (suggestedHashtags.isNotEmpty()) {
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(
                                text = suggestedHashtags.joinToString(" "),
                                color = Color(0xFF00E5FF),
                                fontSize = 13.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }

                        if (!errorText.isNullOrBlank()) {
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(errorText.orEmpty(), color = Color(0xFFFFB74D), fontSize = 12.sp)
                        }

                        if (threadParts.isNotEmpty()) {
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(
                                text = "Thread parts generated: ${threadParts.size} posts",
                                color = Color(0xFF8A2BE2),
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Accept & Discard Actions
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedButton(
                    onClick = onDismiss,
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp),
                    shape = RoundedCornerShape(24.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF444444)),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White)
                ) {
                    Text("Discard", fontWeight = FontWeight.SemiBold)
                }

                Button(
                    onClick = {
                        val finalHashtags = suggestedHashtags
                        val finalText = if (suggestedHashtags.isNotEmpty()) {
                            "$resultText\n\n${suggestedHashtags.joinToString(" ")}"
                        } else {
                            resultText
                        }
                        onApplyText(finalText, finalHashtags, true)
                        onDismiss()
                    },
                    enabled = !isLoading && resultText.isNotBlank(),
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp),
                    shape = RoundedCornerShape(24.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF00E5FF),
                        contentColor = Color.Black
                    )
                ) {
                    Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Accept Suggestion", fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}
