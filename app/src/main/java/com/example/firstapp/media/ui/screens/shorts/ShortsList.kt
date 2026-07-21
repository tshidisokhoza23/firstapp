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

    val context = LocalContext.current
    val pagerState = rememberPagerState(pageCount = { shorts.size })
    
    LaunchedEffect(initialShortId, shorts, pagerState.pageCount) {
        if (initialShortId != null && shorts.size > 0) {
            var foundIndex = -1
            for (i in 0..(shorts.size - 1)) {
                if (shorts[i].id == initialShortId) {
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
    
    LaunchedEffect(pagerState.currentPage, shorts) {
        val urls = ArrayList<String>()
        val start = pagerState.currentPage + 1
        val end = pagerState.currentPage + 3
        for (i in start..end) {
            if (i >= 0 && i < shorts.size) {
                urls.add(shorts[i].videoUrl)
            }
        }
        ShortsVideoCache.preload(context, urls)
    }

    VerticalPager(state = pagerState, modifier = Modifier.fillMaxSize()) { page ->
        val currentShort = shorts[page]
        val isInteractiveShort = currentShort.type == ShortType.REGULAR
        ShortItem(
            short = currentShort, 
            isVisible = pagerState.currentPage == page, 
            bottomInset = bottomPadding, 
            onLikeClick = { if (isInteractiveShort) shortsViewModel.likeShort(it) }, 
            onSubscribeClick = { if (isInteractiveShort) shortsViewModel.subscribeToShortCreator(it) }, 
            onCommentClick = { if (isInteractiveShort) onShowComments(it) }, 
            onBookmarkClick = { if (isInteractiveShort) shortsViewModel.bookmarkShort(it) }, 
            onShareClick = { shortsViewModel.trackShortShare(it) },
            onMoreClick = { onShowMoreMenu(currentShort) }, 
            onCreatorClick = { if (isInteractiveShort) onCreatorClick(it) }
        )
    }
}
