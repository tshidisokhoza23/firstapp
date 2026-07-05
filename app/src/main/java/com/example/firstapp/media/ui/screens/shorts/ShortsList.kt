package com.example.firstapp.media.ui.screens.shorts

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.pager.VerticalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.Dp
import androidx.media3.common.util.UnstableApi
import com.example.firstapp.media.data.ShortsVideoCache
import com.example.firstapp.media.model.ShortMedia
import com.example.firstapp.media.ui.viewmodel.shorts.ShortsViewModel

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
    val pagerState = rememberPagerState { shorts.size }
    
    LaunchedEffect(initialShortId, shorts, pagerState.pageCount) {
        if (initialShortId != null && shorts.size > 0 && pagerState.pageCount >= shorts.size) {
            var index = -1
            var k = 0
            while (k < shorts.size) {
                if (shorts[k].id == initialShortId) {
                    index = k
                    break
                }
                k++
            }
            if (index != -1 && index < pagerState.pageCount) {
                pagerState.scrollToPage(index)
                onShortDisplayed()
            }
        }
    }
    
    LaunchedEffect(pagerState.currentPage, shorts) {
        val sSize = shorts.size
        val nextIndices = java.util.ArrayList<Int>()
        val startIdx = pagerState.currentPage + 1
        val endIdx = pagerState.currentPage + 3
        var i = startIdx
        while (i <= endIdx) {
            if (i >= 0 && i < sSize) nextIndices.add(i)
            i++
        }
        val urls = java.util.ArrayList<String>()
        var j = 0
        while (j < nextIndices.size) {
            urls.add(shorts.get(nextIndices.get(j)).videoUrl)
            j++
        }
        ShortsVideoCache.preload(context, urls)
    }

    VerticalPager(state = pagerState, modifier = Modifier.fillMaxSize()) { page ->
        ShortItem(
            short = shorts[page], 
            isVisible = pagerState.currentPage == page, 
            bottomInset = bottomPadding, 
            onLikeClick = { shortsViewModel.likeShort(it) }, 
            onSubscribeClick = { shortsViewModel.subscribeToShortCreator(it) }, 
            onCommentClick = { onShowComments(it) }, 
            onRepostClick = { shortsViewModel.repostShort(it) }, 
            onBookmarkClick = { shortsViewModel.bookmarkShort(it) }, 
            onShareClick = { shortsViewModel.trackShortShare(it) },
            onMoreClick = { onShowMoreMenu(shorts[page]) }, 
            onCreatorClick = onCreatorClick
        )
    }
}
