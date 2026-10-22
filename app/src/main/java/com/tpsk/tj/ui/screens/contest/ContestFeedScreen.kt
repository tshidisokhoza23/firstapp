package com.tpsk.tj.ui.screens.contest

import android.content.Intent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.tpsk.tj.MainActivity
import com.tpsk.tj.common.data.ads.AdMobManagerImpl
import com.tpsk.tj.media.ui.components.PostItem

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ContestFeedScreen(
    hashtag: String,
    onNavigateBack: () -> Unit,
    onPostClick: (String) -> Unit = {},
    onProfileClick: (String) -> Unit = {},
    viewModel: ContestViewModel = hiltViewModel()
) {
    val cleanHashtag = if (hashtag.startsWith("#")) hashtag else "#$hashtag"
    val context = LocalContext.current
    val adManager = remember(context) { (context as? MainActivity)?.adManager ?: AdMobManagerImpl() }
    val feed by viewModel.feedState.collectAsState()

    LaunchedEffect(hashtag) { viewModel.loadFeed(hashtag) }

    val bgColor = MaterialTheme.colorScheme.background
    val surfaceColor = MaterialTheme.colorScheme.surfaceVariant
    val textColor = MaterialTheme.colorScheme.onBackground
    val secondaryTextColor = MaterialTheme.colorScheme.onSurfaceVariant
    val accentColor = MaterialTheme.colorScheme.primary

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
                    Column(Modifier.padding(16.dp)) {
                        Text(feed.contest?.title ?: cleanHashtag, color = textColor, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                        Text(
                            feed.contest?.description?.ifBlank { "Enter with a manager code, then ask people to vote." }
                                ?: feed.error
                                ?: "This contest is not live yet.",
                            color = secondaryTextColor,
                            fontSize = 13.sp,
                            modifier = Modifier.padding(top = 6.dp)
                        )
                        Text(
                            "${feed.contest?.voteCount ?: 0} votes · ${feed.points} points left",
                            color = GoldYellow,
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.padding(top = 8.dp)
                        )
                        feed.leaderboard.take(3).forEachIndexed { index, entry ->
                            Text(
                                "${index + 1}. ${entry.authorName.ifBlank { "Contestant" }} · ${entry.votes} votes",
                                color = textColor,
                                modifier = Modifier.padding(top = 4.dp)
                            )
                        }
                    }
                }
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
                    post = post.copy(isContest = true),
                    onClick = { onPostClick(post.id) },
                    onReplyClick = { _, _ -> onPostClick(post.id) },
                    onProfileClick = { onProfileClick(it) },
                    onMediaClick = { _, _, _ -> onPostClick(post.id) },
                    onShareClick = { _ -> },
                    onHashtagClick = { tag -> viewModel.loadFeed(tag) }
                )
            }
        }
    }
}
