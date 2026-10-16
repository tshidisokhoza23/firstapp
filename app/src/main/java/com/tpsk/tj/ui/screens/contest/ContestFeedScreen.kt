package com.tpsk.tj.ui.screens.contest

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.tpsk.tj.MainActivity
import com.tpsk.tj.common.data.ads.AdMobManagerImpl
import com.tpsk.tj.media.model.Post
import com.tpsk.tj.media.ui.components.PostItem

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ContestFeedScreen(
    hashtag: String,
    onNavigateBack: () -> Unit,
    onPostClick: (String) -> Unit = {}
) {
    val cleanHashtag = if (hashtag.startsWith("#")) hashtag else "#$hashtag"
    val context = LocalContext.current
    val adManager = remember(context) { (context as? MainActivity)?.adManager ?: AdMobManagerImpl() }

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
                    IconButton(onClick = { /* Share contest */ }) {
                        Icon(Icons.Default.Share, contentDescription = "Share", tint = textColor)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = bgColor)
            )
        },
        containerColor = bgColor
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            // Pinned Top 3 Leaderboard Banner
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
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
                                text = "$cleanHashtag • Week 1",
                                color = accentColor,
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp
                            )
                            Text(
                                text = "Ends in 3d 14h",
                                color = GoldYellow,
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 13.sp
                            )
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        // Top 3 Podium
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceEvenly,
                            verticalAlignment = Alignment.Bottom
                        ) {
                            // Rank 2
                            PodiumContestant(
                                name = "Lerato M.",
                                rank = 2,
                                votes = "1,420",
                                avatarUrl = "https://images.unsplash.com/photo-1534528741775-53994a69daeb"
                            )
                            // Rank 1 (Gold)
                            PodiumContestant(
                                name = "Bokang S.",
                                rank = 1,
                                votes = "2,150",
                                avatarUrl = "https://images.unsplash.com/photo-1517841905240-472988babdf9"
                            )
                            // Rank 3
                            PodiumContestant(
                                name = "Zinhle N.",
                                rank = 3,
                                votes = "980",
                                avatarUrl = "https://images.unsplash.com/photo-1524504388940-b1c1722653e1"
                            )
                        }
                    }
                }
            }

            // Contest Stream Header
            item {
                Text(
                    text = "Contestant Submissions",
                    color = textColor,
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                )
            }

            // Contest Posts rendered as normal PostItem
            itemsIndexed((1..10).toList()) { _, rank ->
                val contestPost = remember(rank, cleanHashtag) {
                    Post(
                        id = "contest_post_$rank",
                        authorId = "candidate_$rank",
                        authorName = "Candidate #$rank",
                        authorHandle = "candidate_$rank",
                        authorAvatarUrl = "https://images.unsplash.com/photo-1534528741775-53994a69daeb",
                        authorAvatarColor = Color(0xFF2563EB),
                        isVerifiedAuthor = true,
                        content = "Vote for me in $cleanHashtag! Representation matters. ✨",
                        mediaUrls = listOf("https://images.unsplash.com/photo-1534528741775-53994a69daeb"),
                        mediaType = "image",
                        timestamp = "${rank}h ago",
                        likes = (1500 - rank * 110).toLong(),
                        repliesCount = (12 + rank).toLong(),
                        repostCount = (5 + rank).toLong(),
                        views = (4200 - rank * 200).toLong(),
                        shares = (18 + rank).toLong(),
                        isContest = true
                    )
                }

                PostItem(
                    adManager = adManager,
                    post = contestPost,
                    onClick = { onPostClick(contestPost.id) }
                )
                HorizontalDivider(color = surfaceColor)
            }
        }
    }
}

@Composable
private fun PodiumContestant(
    name: String,
    rank: Int,
    votes: String,
    avatarUrl: String
) {
    val size = if (rank == 1) 72.dp else 56.dp
    val badgeColor = when (rank) {
        1 -> GoldYellow
        2 -> Color(0xFFC0C0C0)
        else -> Color(0xFFCD7F32)
    }

    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Box(contentAlignment = Alignment.BottomCenter) {
            AsyncImage(
                model = avatarUrl,
                contentDescription = null,
                modifier = Modifier
                    .size(size)
                    .clip(CircleShape)
                    .border(2.dp, badgeColor, CircleShape),
                contentScale = ContentScale.Crop
            )
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
        Text(name, color = MaterialTheme.colorScheme.onBackground, fontSize = 12.sp, fontWeight = FontWeight.Bold)
        Text("$votes votes", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 11.sp)
    }
}
