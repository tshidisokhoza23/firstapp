package com.example.firstapp.media.ui.screens.shorts

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.pager.VerticalPager
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.runtime.*
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.Dp
import androidx.media3.common.util.UnstableApi
import androidx.paging.compose.LazyPagingItems
import com.example.firstapp.media.data.ShortsVideoCache
import com.example.firstapp.media.model.Post
import com.example.firstapp.media.model.PostType
import com.example.firstapp.media.ui.viewmodel.MediaViewModel
import java.util.ArrayList
import kotlin.collections.*
import kotlin.ranges.*

import com.example.firstapp.common.data.ads.AdUnitConfig

@OptIn(ExperimentalFoundationApi::class)
@androidx.annotation.OptIn(UnstableApi::class)
@Composable
fun ShortsList(
    adManager: com.example.firstapp.common.data.ads.AdManager,
    shorts: LazyPagingItems<com.example.firstapp.media.model.Post>,
    shortsViewModel: com.example.firstapp.media.ui.viewmodel.MediaViewModel,
    currentUserAvatarUrl: String? = null,
    bottomPadding: Dp,
    initialShortId: String? = null,
    onShortDisplayed: () -> Unit = {},
    onShowComments: (String) -> Unit,
    onShowMoreMenu: (com.example.firstapp.media.model.Post) -> Unit,
    onCreatorClick: (String) -> Unit = {}
) {
    if (shorts.itemCount == 0) return

    val context = LocalContext.current
    val pagerState = rememberPagerState(pageCount = { shorts.itemCount })
    
    LaunchedEffect(initialShortId) {
        if (initialShortId != null && shorts.itemCount > 0) {
            var foundIndex = -1
            val count = shorts.itemCount
            for (i in 0..count - 1) {
                if (shorts[i]?.id == initialShortId) {
                    foundIndex = i
                    break
                }
            }
            if (foundIndex != -1 && foundIndex < pagerState.pageCount) {
                pagerState.scrollToPage(foundIndex)
                onShortDisplayed()
            }
        }
    }
    
    LaunchedEffect(pagerState.currentPage, shorts.itemCount) {
        val urls = ArrayList<String>()
        val start = pagerState.currentPage + 1
        val end = pagerState.currentPage + 3
        val count = shorts.itemCount
        for (i in start..end) {
            if (i >= 0 && i < count) {
                val post = shorts[i]
                if (post != null && post.mediaUrls.size > 0) {
                    urls.add(post.mediaUrls[0])
                }
            }
        }
        ShortsVideoCache.preload(context, urls)
    }

    VerticalPager(
        state = pagerState, 
        modifier = Modifier.fillMaxSize(),
        key = { index -> 
            if (index < shorts.itemCount) shorts[index]?.id ?: index.toString() else index.toString() 
        }
    ) { page ->
        val currentShort = shorts[page] ?: return@VerticalPager
        
        // Show an ad every 4 content posts
        if ((page + 1) % 4 == 0) {
            Box(
                modifier = Modifier.fillMaxSize().background(Color.Black)
            ) {
                com.example.firstapp.common.ui.components.DynamicAdCard(
                    adManager = adManager,
                    adUnitId = AdUnitConfig.NATIVE_SHORTS_ID,
                    modifier = Modifier.fillMaxSize(),
                    isShortsAd = true
                )
            }
        } else {
            // Allow interaction for all types that are rendered as ShortItem
            val isInteractiveShort = currentShort.type != com.example.firstapp.media.model.PostType.AD_UNIT && currentShort.type != com.example.firstapp.media.model.PostType.EXTERNAL_AD_PLACEHOLDER
            
            ShortItem(
                adManager = adManager,
                short = currentShort,
                currentUserAvatarUrl = currentUserAvatarUrl,
                isVisible = pagerState.currentPage == page, 
                bottomInset = bottomPadding, 
                onLikeClick = { if (isInteractiveShort) shortsViewModel.likePost(it, currentShort.likes, currentShort.isLiked) }, 
                onSubscribeClick = { /* id -> shortsViewModel.subscribeToShortCreator(id) */ }, 
                onCommentClick = { if (isInteractiveShort) onShowComments(it) }, 
                onBookmarkClick = { if (isInteractiveShort) shortsViewModel.bookmarkPost(it) }, 
                onShareClick = { shortsViewModel.repostPost(it) },
                onMoreClick = { onShowMoreMenu(currentShort) }, 
                onCreatorClick = onCreatorClick,
                onEngagement = { pid, type, time, comp -> 
                    shortsViewModel.trackEngagement(pid, type, currentShort.category, time, comp)
                }
            )
        }
    }
}
