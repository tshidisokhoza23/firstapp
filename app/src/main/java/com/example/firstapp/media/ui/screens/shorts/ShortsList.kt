package com.example.firstapp.media.ui.screens.shorts

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.pager.VerticalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.Dp
import androidx.media3.common.util.UnstableApi
import com.example.firstapp.media.data.ShortsVideoCache
import com.example.firstapp.media.model.ShortMedia
import com.example.firstapp.media.model.ShortType
import com.example.firstapp.media.ui.viewmodel.shorts.ShortsViewModel
import java.util.ArrayList

@OptIn(ExperimentalFoundationApi::class)
@androidx.annotation.OptIn(UnstableApi::class)
@Composable
fun ShortsList(
    shorts: List<ShortMedia>,
    shortsViewModel: ShortsViewModel,
    bottomPadding: Dp,
    initialShortId: String? = null,
    onShortDisplayed: () -> Unit = {},
    onShowComments: (String) -> Unit,
    onShowMoreMenu: (ShortMedia) -> Unit,
    onCreatorClick: (String) -> Unit = {}
) {
    if (shorts.isEmpty()) return

    val shortsWithAds = shorts

    val context = LocalContext.current
    val pagerState = rememberPagerState(pageCount = { shortsWithAds.size })
    
    LaunchedEffect(initialShortId, shortsWithAds, pagerState.pageCount) {
        if (initialShortId != null && shortsWithAds.size > 0) {
            var foundIndex = -1
            for (i in 0..(shortsWithAds.size - 1)) {
                if (shortsWithAds[i].id == initialShortId) {
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
    
    LaunchedEffect(pagerState.currentPage, shortsWithAds) {
        val urls = ArrayList<String>()
        val start = pagerState.currentPage + 1
        val end = pagerState.currentPage + 3
        for (i in start..end) {
            if (i >= 0 && i < shortsWithAds.size) {
                urls.add(shortsWithAds[i].videoUrl)
            }
        }
        ShortsVideoCache.preload(context, urls)
    }

    VerticalPager(state = pagerState, modifier = Modifier.fillMaxSize()) { page ->
        val currentShort = shortsWithAds[page]
        ShortItem(
            short = currentShort, 
            isVisible = pagerState.currentPage == page, 
            bottomInset = bottomPadding, 
            onLikeClick = { if (currentShort.type != ShortType.AD) shortsViewModel.likeShort(it) }, 
            onSubscribeClick = { if (currentShort.type != ShortType.AD) shortsViewModel.subscribeToShortCreator(it) }, 
            onCommentClick = { if (currentShort.type != ShortType.AD) onShowComments(it) }, 
            onRepostClick = { if (currentShort.type != ShortType.AD) shortsViewModel.repostShort(it) }, 
            onBookmarkClick = { if (currentShort.type != ShortType.AD) shortsViewModel.bookmarkShort(it) }, 
            onShareClick = { shortsViewModel.trackShortShare(it) },
            onMoreClick = { onShowMoreMenu(currentShort) }, 
            onCreatorClick = { if (currentShort.type != ShortType.AD) onCreatorClick(it) }
        )
    }
}
