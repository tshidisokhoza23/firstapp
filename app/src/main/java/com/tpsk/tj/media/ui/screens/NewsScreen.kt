package com.tpsk.tj.media.ui.screens

import android.content.Intent
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.ChatBubbleOutline
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import com.tpsk.tj.common.ui.components.NativeAdCard
import com.tpsk.tj.media.ui.viewmodel.NewsViewModel
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

sealed interface NewsFeedItem {
    data class ArticleItem(val article: NewsArticle) : NewsFeedItem
    data class NativeAdItem(val adObject: Any) : NewsFeedItem
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NewsScreen(
    onBack: () -> Unit,
    onArticleClick: (NewsArticle) -> Unit = {},
    onCommentsClick: (NewsArticle) -> Unit = {},
    onShareToChat: (NewsArticle) -> Unit = {},
    newsViewModel: NewsViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val feedItems by newsViewModel.feedItems.collectAsState()
    val isLoading by newsViewModel.isLoading.collectAsState()
    val isPaginationLoading by newsViewModel.isPaginationLoading.collectAsState()

    val listState = rememberLazyListState()

    val shouldLoadMore by remember {
        derivedStateOf {
            val lastVisibleItem = listState.layoutInfo.visibleItemsInfo.lastOrNull() ?: return@derivedStateOf false
            lastVisibleItem.index >= feedItems.size - 4
        }
    }

    LaunchedEffect(shouldLoadMore) {
        if (shouldLoadMore) {
            newsViewModel.loadMoreNews()
        }
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
            if (isLoading && feedItems.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(color = Color(0xFF00B4D8))
                }
            } else {
                LazyColumn(
                    state = listState,
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    itemsIndexed(feedItems, key = { index, item ->
                        when (item) {
                            is NewsFeedItem.ArticleItem -> "article_${item.article.id}"
                            is NewsFeedItem.NativeAdItem -> "ad_${item.adObject.hashCode()}_$index"
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
                                    onLike = { newsViewModel.toggleLike(article.id) },
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
                                NativeAdCard(adObject = item.adObject)
                            }
                        }
                    }

                    if (isPaginationLoading) {
                        item {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                CircularProgressIndicator(color = Color(0xFF00B4D8), modifier = Modifier.size(28.dp))
                            }
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
                Text(
                    text = article.title,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    lineHeight = 24.sp,
                    color = MaterialTheme.colorScheme.onSurface
                )

                Spacer(modifier = Modifier.height(6.dp))

                Text(
                    text = "${article.sourceName} • ${article.timeAgo}",
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.height(16.dp))

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
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
