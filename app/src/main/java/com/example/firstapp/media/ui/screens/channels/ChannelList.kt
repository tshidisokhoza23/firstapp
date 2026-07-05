package com.example.firstapp.media.ui.screens.channels

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.example.firstapp.media.model.Channel
import com.example.firstapp.media.ui.components.ChannelItem

@Composable
fun ChannelList(channels: List<Channel>, onChannelClick: (Channel) -> Unit) {
    LazyColumn(modifier = Modifier.fillMaxSize(), contentPadding = PaddingValues(bottom = 180.dp)) {
        items(channels, key = { it.id }) { ch -> 
            ChannelItem(channel = ch, onClick = { onChannelClick(ch) })
            HorizontalDivider(modifier = Modifier.padding(start = 72.dp), thickness = 0.5.dp, color = Color(0xFF233138))
        }
    }
}
