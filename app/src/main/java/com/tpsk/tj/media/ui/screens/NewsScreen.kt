package com.tpsk.tj.media.ui.screens

import android.content.Intent
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.ChatBubbleOutline
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.ThumbUp
import androidx.compose.material.icons.outlined.ThumbUp
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.tpsk.tj.media.util.NewsCustomTabsHelper

data class NewsArticle(
    val id: String,
    val title: String,
    val sourceName: String,
    val timeAgo: String,
    val imageUrl: String,
    val category: String,
    val summary: String = "",
    val originalUrl: String = "https://www.news24.com",
    val commentCount: Int = 0,
    var likeCount: Int = 0,
    var isLiked: Boolean = false
)

sealed class NewsFeedItem {
    data class ArticleItem(val article: NewsArticle) : NewsFeedItem()
    data class NativeAdItem(val adId: String) : NewsFeedItem()
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NewsScreen(
    onBack: () -> Unit,
    onArticleClick: (NewsArticle) -> Unit = {},
    onCommentsClick: (NewsArticle) -> Unit = {},
    onShareToChat: (NewsArticle) -> Unit = {}
) {
    val context = LocalContext.current
    var selectedCategory by remember { mutableStateOf("All") }
    val categories = listOf("All", "Local", "Sports", "Entertainment", "National", "World")

    val articlesList = remember {
        mutableStateListOf(
            NewsArticle(
                id = "news_1",
                title = "Wits University announces new campus safety & digital infrastructure measures",
                sourceName = "TimesLIVE",
                timeAgo = "2h ago",
                imageUrl = "https://images.unsplash.com/photo-1541339907198-e08756dedf3f?w=800",
                category = "Local",
                summary = "Wits University management has rolled out enhanced security and campus Wi-Fi upgrades.",
                originalUrl = "https://www.timeslive.co.za",
                commentCount = 24,
                likeCount = 142
            ),
            NewsArticle(
                id = "news_2",
                title = "Tembisa community launches new local entrepreneurship market",
                sourceName = "SowetanLIVE",
                timeAgo = "4h ago",
                imageUrl = "https://images.unsplash.com/photo-1533900298318-6b8da08a523e?w=800",
                category = "Local",
                summary = "Local business owners gather in Tembisa for the weekend community showcase.",
                originalUrl = "https://www.sowetanlive.co.za",
                commentCount = 18,
                likeCount = 98
            ),
            NewsArticle(
                id = "news_3",
                title = "South Africa national team secures crucial victory in international qualifier",
                sourceName = "SuperSport",
                timeAgo = "6h ago",
                imageUrl = "https://images.unsplash.com/photo-1508098682722-e99c43a406b2?w=800",
                category = "Sports",
                summary = "A dramatic late goal seals victory in front of a packed stadium crowd.",
                originalUrl = "https://supersport.com",
                commentCount = 56,
                likeCount = 310
            ),
            NewsArticle(
                id = "news_4",
                title = "Johannesburg Stock Exchange sees surge in tech and energy equities",
                sourceName = "News24",
                timeAgo = "8h ago",
                imageUrl = "https://images.unsplash.com/photo-1611974789855-9c2a0a7236a3?w=800",
                category = "National",
                summary = "Markets rally as clean energy investments gain momentum across South Africa.",
                originalUrl = "https://www.news24.com",
                commentCount = 32,
                likeCount = 184
            ),
            NewsArticle(
                id = "news_5",
                title = "South African music festival unveils stellar international lineup",
                sourceName = "TimesLIVE",
                timeAgo = "10h ago",
                imageUrl = "https://images.unsplash.com/photo-1470225620780-dba8ba36b745?w=800",
                category = "Entertainment",
                summary = "Top Amapiano and Afrobeats artists prepare for the summer stadium showcase.",
                originalUrl = "https://www.timeslive.co.za",
                commentCount = 41,
                likeCount = 275
            ),
            NewsArticle(
                id = "news_6",
                title = "Global climate summit agrees on new renewable energy initiatives",
                sourceName = "News24",
                timeAgo = "12h ago",
                imageUrl = "https://images.unsplash.com/photo-1497435334941-8c899ee9e8e9?w=800",
                category = "World",
                summary = "World leaders unite on clean energy targets and sustainable infrastructure funding.",
                originalUrl = "https://www.news24.com",
                commentCount = 15,
                likeCount = 89
            )
        )
    }

    // Filter and Interleave Native Ads every 5 news articles
    val feedItems = remember(selectedCategory, articlesList.toList()) {
        val filtered = if (selectedCategory == "All") articlesList
        else articlesList.filter { it.category.equals(selectedCategory, ignoreCase = true) }

        val items = mutableListOf<NewsFeedItem>()
        filtered.forEachIndexed { index, article ->
            items.add(NewsFeedItem.ArticleItem(article))
            if ((index + 1) % 5 == 0) {
                items.add(NewsFeedItem.NativeAdItem("ad_news_${index + 1}"))
            }
        }
        items
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text("News", fontWeight = FontWeight.Bold, fontSize = 22.sp)
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = { /* Search */ }) {
                        Icon(Icons.Default.Search, contentDescription = "Search")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background)
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            // Horizontal Categories Bar
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp)
            ) {
                items(categories) { cat ->
                    val isSelected = selectedCategory == cat
                    Surface(
                        shape = RoundedCornerShape(20.dp),
                        color = if (isSelected) Color(0xFF00B4D8) else MaterialTheme.colorScheme.surfaceVariant,
                        modifier = Modifier.clickable { selectedCategory = cat }
                    ) {
                        Text(
                            text = cat,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (isSelected) Color.Black else MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                        )
                    }
                }
            }

            // News List with In-Feed Native Ads
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                itemsIndexed(feedItems, key = { index, item ->
                    when (item) {
                        is NewsFeedItem.ArticleItem -> "article_${item.article.id}"
                        is NewsFeedItem.NativeAdItem -> "ad_${item.adId}_$index"
                    }
                }) { _, item ->
                    when (item) {
                        is NewsFeedItem.ArticleItem -> {
                            val article = item.article
                            NewsCard(
                                article = article,
                                onArticleClick = {
                                    NewsCustomTabsHelper.launchNewsUrl(context, article.originalUrl) {
                                        onArticleClick(article)
                                    }
                                },
                                onCommentsClick = { onCommentsClick(article) },
                                onLike = {
                                    val index = articlesList.indexOfFirst { it.id == article.id }
                                    if (index != -1) {
                                        val current = articlesList[index]
                                        val newLiked = !current.isLiked
                                        val newLikes = if (newLiked) current.likeCount + 1 else current.likeCount - 1
                                        articlesList[index] = current.copy(isLiked = newLiked, likeCount = newLikes)
                                    }
                                },
                                onShareToChat = {
                                    onShareToChat(article)
                                    val sendIntent = Intent().apply {
                                        action = Intent.ACTION_SEND
                                        putExtra(
                                            Intent.EXTRA_TEXT,
                                            "${article.title}\n${article.sourceName} • ${article.timeAgo}\n${article.originalUrl}\nRead on TJ News!"
                                        )
                                        type = "text/plain"
                                    }
                                    context.startActivity(Intent.createChooser(sendIntent, "Share to Chat"))
                                }
                            )
                        }
                        is NewsFeedItem.NativeAdItem -> {
                            NewsNativeAdCard(adId = item.adId)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun NewsCard(
    article: NewsArticle,
    onArticleClick: () -> Unit = {},
    onCommentsClick: () -> Unit = {},
    onLike: () -> Unit = {},
    onShareToChat: () -> Unit = {}
) {
    Surface(
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 2.dp,
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onArticleClick() }
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            // Cover Image
            AsyncImage(
                model = article.imageUrl,
                contentDescription = article.title,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(200.dp)
                    .clip(RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp)),
                contentScale = ContentScale.Crop
            )

            Column(modifier = Modifier.padding(16.dp)) {
                // Article Title
                Text(
                    text = article.title,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    lineHeight = 24.sp,
                    color = MaterialTheme.colorScheme.onSurface
                )

                Spacer(modifier = Modifier.height(6.dp))

                // Source & Relative Time
                Text(
                    text = "${article.sourceName} • ${article.timeAgo}",
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Bottom Action Row
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    // Comments Count Button (Launches Full-Screen Discussion)
                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                        modifier = Modifier.clickable { onCommentsClick() }
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.ChatBubbleOutline,
                                contentDescription = "Comments",
                                tint = MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "${article.commentCount} Comments",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    // Like Button
                    IconButton(
                        onClick = onLike,
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            imageVector = if (article.isLiked) Icons.Default.ThumbUp else Icons.Outlined.ThumbUp,
                            contentDescription = "Like",
                            tint = if (article.isLiked) Color(0xFF00B4D8) else MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.size(18.dp)
                        )
                    }

                    Spacer(modifier = Modifier.weight(1f))

                    // Cyan "Share to Chat ➔" Button
                    Button(
                        onClick = onShareToChat,
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00B4D8)),
                        shape = RoundedCornerShape(20.dp),
                        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp)
                    ) {
                        Text(
                            text = "Share to Chat",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.Black
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Icon(
                            imageVector = Icons.Default.ArrowForward,
                            contentDescription = null,
                            tint = Color.Black,
                            modifier = Modifier.size(14.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun NewsNativeAdCard(adId: String) {
    Surface(
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Surface(
                    shape = RoundedCornerShape(4.dp),
                    color = Color(0xFF00B4D8)
                ) {
                    Text(
                        text = "SPONSORED",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.Black,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
                Spacer(modifier = Modifier.weight(1f))
                Icon(
                    imageVector = Icons.Default.Info,
                    contentDescription = "AdChoices",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(14.dp)
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            Text(
                text = "Discover Top South African Tech & Local Offers",
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = "Upgrade your mobile experience with high-speed fiber, exclusive device deals, and premium digital rewards.",
                fontSize = 13.sp,
                lineHeight = 18.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(12.dp))

            Button(
                onClick = { /* Open Ad Link */ },
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00B4D8)),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.align(Alignment.End)
            ) {
                Text("Visit Website", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color.Black)
            }
        }
    }
}
