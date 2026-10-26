package com.tpsk.tj.ui.screens.contest

import android.content.Intent
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import com.tpsk.tj.MainActivity
import com.tpsk.tj.common.data.ads.AdMobManagerImpl
import com.tpsk.tj.media.data.api.ApiLeaderboardEntry
import com.tpsk.tj.media.ui.components.PostItem

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ContestFeedScreen(
    hashtag: String,
    onNavigateBack: () -> Unit,
    onPostClick: (String) -> Unit = {},
    onProfileClick: (String) -> Unit = {},
    onNotAContest: () -> Unit = {},
    onHashtagClick: (String) -> Unit = {},
    viewModel: ContestViewModel = hiltViewModel()
) {
    val cleanHashtag = if (hashtag.startsWith("#")) hashtag else "#$hashtag"
    val context = LocalContext.current
    val adManager = remember(context) { (context as? MainActivity)?.adManager ?: AdMobManagerImpl() }
    val feed by viewModel.feedState.collectAsState()

    LaunchedEffect(hashtag) { viewModel.loadFeed(hashtag) }
    LaunchedEffect(feed.isLoading, feed.contest, feed.error) {
        if (!feed.isLoading && feed.error == null && feed.contest == null) onNotAContest()
    }

    val bgColor = MaterialTheme.colorScheme.background
    val surfaceColor = MaterialTheme.colorScheme.surfaceVariant
    val textColor = MaterialTheme.colorScheme.onBackground
    val secondaryTextColor = MaterialTheme.colorScheme.onSurfaceVariant
    val accentColor = MaterialTheme.colorScheme.primary
    val votesByPost = remember(feed.leaderboard) { feed.leaderboard.associate { it.postId to it.votes } }
    val podium = remember(feed.leaderboard) { podiumOrder(feed.leaderboard) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(cleanHashtag, fontWeight = FontWeight.Bold, color = textColor) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = textColor)
                    }
                },
                actions = {
                    IconButton(onClick = {
                        val send = Intent(Intent.ACTION_SEND).apply {
                            type = "text/plain"
                            putExtra(Intent.EXTRA_TEXT, "Vote in $cleanHashtag on FirstApp")
                        }
                        context.startActivity(Intent.createChooser(send, "Share contest"))
                    }) {
                        Icon(Icons.Default.Share, contentDescription = "Share", tint = textColor)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = bgColor)
            )
        },
        containerColor = bgColor
    ) { padding ->
        if (feed.isLoading) {
            Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = accentColor)
            }
            return@Scaffold
        }
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(bottom = 24.dp)
        ) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth().padding(16.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = surfaceColor)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = "$cleanHashtag • ${contestWeekLabel(feed.contest?.createdAt)}",
                                color = accentColor,
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp
                            )
                            Text(
                                text = endsInLabel(feed.contest?.endsAt).ifBlank { "Live" },
                                color = GoldYellow,
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 13.sp
                            )
                        }
                        Spacer(modifier = Modifier.height(16.dp))
                        if (podium.isEmpty()) {
                            Text("No contestants yet.", color = secondaryTextColor, fontSize = 13.sp)
                        } else {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceEvenly,
                                verticalAlignment = Alignment.Bottom
                            ) {
                                podium.forEach { (rank, entry) ->
                                    PodiumContestant(
                                        name = entry.authorName.ifBlank { "Contestant" },
                                        rank = rank,
                                        votes = formatVotes(entry.votes),
                                        avatarUrl = entry.avatarUrl,
                                        onClick = { if (entry.authorId.isNotBlank()) onProfileClick(entry.authorId) }
                                    )
                                }
                            }
                        }
                    }
                }
            }
            item {
                Text(
                    text = "Contestant Submissions",
                    color = textColor,
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                )
            }
            if (feed.posts.isEmpty()) {
                item {
                    Text(
                        "No entries yet. Create a post with this hashtag and a manager code.",
                        color = secondaryTextColor,
                        modifier = Modifier.padding(horizontal = 16.dp)
                    )
                }
            }
            items(feed.posts, key = { it.id }) { post ->
                PostItem(
                    adManager = adManager,
                    post = post.copy(
                        isContest = true,
                        contestVotes = votesByPost[post.id] ?: post.contestVotes
                    ),
                    onClick = { onPostClick(post.id) },
                    onReplyClick = { _, _ -> onPostClick(post.id) },
                    onProfileClick = { onProfileClick(it) },
                    onMediaClick = { _, _, _ -> onPostClick(post.id) },
                    onShareClick = { },
                    onHashtagClick = onHashtagClick
                )
                HorizontalDivider(color = surfaceColor)
            }
        }
    }
}

private fun podiumOrder(entries: List<ApiLeaderboardEntry>): List<Pair<Int, ApiLeaderboardEntry>> {
    val top = entries.take(3)
    val indexes = when (top.size) {
        0 -> emptyList()
        1 -> listOf(0)
        2 -> listOf(1, 0)
        else -> listOf(1, 0, 2)
    }
    return indexes.map { index -> (index + 1) to top[index] }
}

@Composable
private fun PodiumContestant(
    name: String,
    rank: Int,
    votes: String,
    avatarUrl: String?,
    onClick: () -> Unit
) {
    val size = if (rank == 1) 72.dp else 56.dp
    val badgeColor = when (rank) {
        1 -> GoldYellow
        2 -> Color(0xFFC0C0C0)
        else -> Color(0xFFCD7F32)
    }
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.width(96.dp).clickable(onClick = onClick)
    ) {
        Box(contentAlignment = Alignment.BottomCenter) {
            if (!avatarUrl.isNullOrBlank()) {
                AsyncImage(
                    model = avatarUrl,
                    contentDescription = name,
                    modifier = Modifier.size(size).clip(CircleShape).border(2.dp, badgeColor, CircleShape),
                    contentScale = ContentScale.Crop
                )
            } else {
                Box(
                    modifier = Modifier
                        .size(size)
                        .clip(CircleShape)
                        .background(badgeColor.copy(alpha = 0.25f))
                        .border(2.dp, badgeColor, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Text(name.take(1).uppercase(), fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onBackground)
                }
            }
            Box(
                modifier = Modifier
                    .offset(y = 8.dp)
                    .background(badgeColor, RoundedCornerShape(10.dp))
                    .padding(horizontal = 8.dp, vertical = 2.dp)
            ) {
                Text("#$rank", color = Color.Black, fontSize = 10.sp, fontWeight = FontWeight.Bold)
            }
        }
        Spacer(modifier = Modifier.height(12.dp))
        Text(name, color = MaterialTheme.colorScheme.onBackground, fontSize = 12.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center, maxLines = 1)
        Text("$votes votes", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 11.sp)
    }
}
