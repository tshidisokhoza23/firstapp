package com.tpsk.tj.ui.screens.contest

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ContestFeedScreen(
    hashtag: String,
    onNavigateBack: () -> Unit,
    onPostClick: (String) -> Unit = {}
) {
    val cleanHashtag = if (hashtag.startsWith("#")) hashtag else "#$hashtag"

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(cleanHashtag, fontWeight = FontWeight.Bold, color = Color.White) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = Color.White)
                    }
                },
                actions = {
                    IconButton(onClick = { /* Share contest */ }) {
                        Icon(Icons.Default.Share, contentDescription = "Share", tint = Color.White)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = DarkBg)
            )
        },
        containerColor = DarkBg
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
                    colors = CardDefaults.cardColors(containerColor = DarkSurface)
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
                                color = AccentBlue,
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
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                )
            }

            // Contest Posts
            items((1..10).toList()) { rank ->
                ContestPostCard(
                    rank = rank,
                    candidateName = "Candidate #$rank",
                    managerHandle = "@mgr_thembisa",
                    caption = "Vote for me in $cleanHashtag! Representation matters. ✨",
                    photoUrl = "https://images.unsplash.com/photo-1534528741775-53994a69daeb",
                    initialVotes = 1500 - (rank * 110)
                )
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
        Text(name, color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
        Text("$votes votes", color = Color.Gray, fontSize = 11.sp)
    }
}

@Composable
private fun ContestPostCard(
    rank: Int,
    candidateName: String,
    managerHandle: String,
    caption: String,
    photoUrl: String,
    initialVotes: Int
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = DarkSurface)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Header Row: Rank Badge & Manager
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.EmojiEvents, contentDescription = null, tint = GoldYellow, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "Rank #$rank",
                        color = GoldYellow,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp
                    )
                }
                Text(managerHandle, color = Color.Gray, fontSize = 12.sp)
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Caption
            Text(caption, color = Color.White, fontSize = 14.sp)

            Spacer(modifier = Modifier.height(12.dp))

            // Candidate Photo
            AsyncImage(
                model = photoUrl,
                contentDescription = null,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(280.dp)
                    .clip(RoundedCornerShape(12.dp)),
                contentScale = ContentScale.Crop
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Bottom Action Bar with Voting Button
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(candidateName, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)

                ContestVoteButton(
                    initialVotes = initialVotes,
                    onCastVote = { isAd ->
                        // Vote committed
                    }
                )
            }
        }
    }
}
