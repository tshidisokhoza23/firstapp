package com.tpsk.tj.media.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.paging.compose.collectAsLazyPagingItems
import androidx.paging.compose.itemContentType
import androidx.paging.compose.itemKey
import com.tpsk.tj.common.data.ads.AdManager
import com.tpsk.tj.media.model.Post
import com.tpsk.tj.media.ui.components.PostItem
import com.tpsk.tj.media.ui.viewmodel.MediaViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SavedPostsScreen(
    onBack: () -> Unit,
    adManager: AdManager,
    mediaViewModel: MediaViewModel = hiltViewModel(),
    onPostClick: (String) -> Unit,
    onProfileClick: (String) -> Unit,
    onMediaClick: (String, String, Long) -> Unit,
    onShareClick: (Post) -> Unit
) {
    val savedPostsFlow = remember(mediaViewModel) {
        mediaViewModel.getSavedPostsPaging()
    }
    val lazySavedPosts = savedPostsFlow.collectAsLazyPagingItems()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Saved Posts", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    titleContentColor = MaterialTheme.colorScheme.onSurface,
                    navigationIconContentColor = MaterialTheme.colorScheme.onSurface
                )
            )
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(MaterialTheme.colorScheme.background)
        ) {
            if (lazySavedPosts.itemCount == 0 && lazySavedPosts.loadState.refresh is androidx.paging.LoadState.NotLoading) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "No saved posts yet",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize()
                ) {
                    items(
                        count = lazySavedPosts.itemCount,
                        key = lazySavedPosts.itemKey { it.id },
                        contentType = lazySavedPosts.itemContentType { "post" }
                    ) { index ->
                        val post = lazySavedPosts[index]
                        if (post != null) {
                            PostItem(
                                adManager = adManager,
                                post = post,
                                onClick = { onPostClick(post.id) },
                                onLike = { id, l, liked -> mediaViewModel.likePost(id, l, liked) },
                                onRepostClick = { mediaViewModel.repostPost(it.id, it.repostCount, it.isReposted) },
                                onBookmark = { mediaViewModel.bookmarkPost(it) },
                                onReplyClick = { p, _ -> onPostClick(p.id) },
                                onProfileClick = { onProfileClick(it) },
                                onMediaClick = onMediaClick,
                                onShareClick = { onShareClick(it) }
                            )
                        }
                    }
                }
            }
        }
    }
}
