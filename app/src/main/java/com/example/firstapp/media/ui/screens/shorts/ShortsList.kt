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
import com.example.firstapp.media.data.ShortsVideoCache
import com.example.firstapp.media.model.ShortMedia
import com.example.firstapp.media.model.ShortType
import com.example.firstapp.media.ui.viewmodel.shorts.ShortsViewModel
import java.util.ArrayList

@OptIn(ExperimentalFoundationApi::class)
@androidx.annotation.OptIn(UnstableApi::class)
@Composable
fun ShortsList(
    adManager: com.example.firstapp.common.data.ads.AdManager,
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
    
    LaunchedEffect(initialShortId) {
        if (initialShortId != null && shorts.size > 0) {
            var foundIndex = -1
            var i = 0
            while (i < shorts.size) {
                if (shorts.get(i).id == initialShortId) {
                    foundIndex = i
                    break
                }
                i++
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

    VerticalPager(
        state = pagerState, 
        modifier = Modifier.fillMaxSize(),
        key = { index -> 
            if (index < shorts.size) shorts[index].id else index.toString() 
        }
    ) { page ->
        val currentShort = shorts[page]
        
        // Show an ad every 5 shorts
        if ((page + 1) % 5 == 0) {
            Box(
                modifier = Modifier.fillMaxSize().background(Color.Black)
            ) {
                com.example.firstapp.common.ui.components.DynamicAdCard(
                    adManager = adManager,
                    adUnitId = "ca-app-pub-3940256099942544/2247696110", // AdMob Native Test ID
                    modifier = Modifier.fillMaxSize(),
                    isShortsAd = true
                )
            }
        } else {
            val isInteractiveShort = currentShort.type == ShortType.REGULAR
            ShortItem(
                adManager = adManager,
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
}
