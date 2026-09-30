package com.tpsk.tj.channels.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.tpsk.tj.chat.model.Channel
import com.tpsk.tj.common.util.CountryUtils

@Composable
fun ChannelList(
    channels: List<Channel>, 
    topBarHeight: Dp = 0.dp,
    currentUserId: String? = null,
    onChannelClick: (Channel) -> Unit,
    onSubscribe: (String) -> Unit = {}
) {
    if (channels.isEmpty()) {
        Column(
            modifier = Modifier.fillMaxSize().padding(horizontal = 32.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text("No channels yet", fontSize = 18.sp, color = MaterialTheme.colorScheme.onBackground)
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                "Channels you follow show up here. The most followed channels in your country appear in this list too.",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 14.sp,
                textAlign = TextAlign.Center
            )
        }
        return
    }
    val topStart = channels.indexOfFirst { it.isTopInCountry && !it.isSubscribed && !it.isAdmin }
    val followed = if (topStart >= 0) channels.take(topStart) else channels
    val top = if (topStart >= 0) channels.drop(topStart) else emptyList()
    LazyColumn(modifier = Modifier.fillMaxSize(), contentPadding = PaddingValues(bottom = 160.dp)) {
        item { Spacer(modifier = Modifier.height(topBarHeight)) }
        items(followed, key = { it.conversationId ?: it.id }) { ch ->
            ChannelItem(
                channel = ch,
                currentUserId = currentUserId,
                onClick = { onChannelClick(ch) },
                onSubscribe = onSubscribe
            )
        }
        if (top.isNotEmpty()) {
            item(key = "top-in-country") {
                Text(
                    text = topCountryTitle(top.first().countryCode),
                    modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 18.dp, bottom = 4.dp),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 13.sp
                )
            }
            items(top, key = { "top-${it.conversationId ?: it.id}" }) { ch ->
                ChannelItem(
                    channel = ch,
                    currentUserId = currentUserId,
                    onClick = { onChannelClick(ch) },
                    onSubscribe = onSubscribe
                )
            }
        }
    }
}

private fun topCountryTitle(countryCode: String): String {
    val match = CountryUtils.allCountries.find { it.code.equals(countryCode, ignoreCase = true) }
    return if (match != null) "Most followed in ${match.name}" else "Most followed in your country"
}
