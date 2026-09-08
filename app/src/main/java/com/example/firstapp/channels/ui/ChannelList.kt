package com.example.firstapp.channels.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.firstapp.chat.model.Channel

@Composable
fun ChannelList(
    channels: List<Channel>, 
    topBarHeight: Dp = 0.dp,
    currentUserId: String? = null,
    onChannelClick: (Channel) -> Unit,
    onSubscribe: (String) -> Unit = {}
) {
    LazyColumn(modifier = Modifier.fillMaxSize(), contentPadding = PaddingValues(bottom = 16.dp)) {
        item { Spacer(modifier = Modifier.height(topBarHeight)) }
        items(channels, key = { it.id }) { ch -> 
            ChannelItem(
                channel = ch,
                currentUserId = currentUserId,
                onClick = { onChannelClick(ch) },
                onSubscribe = onSubscribe
            )
        }
    }
}
