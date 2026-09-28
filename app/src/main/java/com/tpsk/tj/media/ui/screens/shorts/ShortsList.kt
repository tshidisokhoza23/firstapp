package com.tpsk.tj.media.ui.screens.shorts

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
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
import com.google.android.gms.ads.nativead.NativeAd
import androidx.paging.compose.LazyPagingItems
import com.tpsk.tj.media.data.ShortsVideoCache
import com.tpsk.tj.media.model.Post
import com.tpsk.tj.media.model.PostType
import com.tpsk.tj.media.ui.viewmodel.MediaViewModel
import java.util.ArrayList
import kotlin.collections.*
import kotlin.ranges.*

import com.tpsk.tj.common.data.ads.AdUnitConfig

@OptIn(ExperimentalFoundationApi::class)
@androidx.annotation.OptIn(UnstableApi::class)
@Composable
fun ShortsList(
    adManager: com.tpsk.tj.common.data.ads.AdManager,
    shorts: LazyPagingItems<com.tpsk.tj.media.model.Post>,
    shortsViewModel: com.tpsk.tj.media.ui.viewmodel.MediaViewModel,
    preloadedAds: List<Any> = emptyList(),
    currentUserAvatarUrl: String? = null,
    ownUserId: String? = null,
    bottomPadding: Dp,
    initialShortId: String? = null,
    onShortDisplayed: () -> Unit = {},
    onShowComments: (String) -> Unit,
    onShowMoreMenu: (com.tpsk.tj.media.model.Post) -> Unit,
    onCreatorClick: (String) -> Unit = {},
    isGlobalPaused: Boolean = false
) {
    if (shorts.itemCount == 0) return

    val context = LocalContext.current
    val validPreloadedAds = preloadedAds.filterIsInstance<NativeAd>()
    val shortsBetweenAds = 8
    val adStride = shortsBetweenAds + 1
    val adCount = minOf(validPreloadedAds.size, shorts.itemCount / shortsBetweenAds)
    val totalPageCount = shorts.itemCount + adCount

    val pagerState = rememberPagerState(pageCount = { totalPageCount })
    
    fun isAdPage(page: Int): Boolean {
        if (adCount == 0) return false
        val adIndex = page / adStride
        return page % adStride == shortsBetweenAds && adIndex < adCount
    }

    fun getShortIndex(page: Int): Int {
        if (adCount == 0) return page
        val adsBefore = minOf(page / adStride, adCount)
        return page - adsBefore
    }

    LaunchedEffect(initialShortId) {
        if (initialShortId != null && shorts.itemCount > 0) {
            var foundShortIndex = -1
            val count = shorts.itemCount
            for (i in 0 until count) {
                if (shorts[i]?.id == initialShortId) {
                    foundShortIndex = i
                    break
                }
            }
            if (foundShortIndex != -1) {
                // Map short index back to page index
                val adsBefore = if (adCount > 0) foundShortIndex / shortsBetweenAds else 0
                val pageIndex = foundShortIndex + minOf(adsBefore, adCount)
                if (pageIndex < totalPageCount) {
                    pagerState.scrollToPage(pageIndex)
                    onShortDisplayed()
                }
            }
        }
    }
    
    LaunchedEffect(pagerState.currentPage, shorts.itemCount) {
        val urls = ArrayList<String>()
        val startPage = pagerState.currentPage + 1
        val endPage = pagerState.currentPage + 3
        
        for (p in startPage..endPage) {
            if (p >= 0 && p < totalPageCount && !isAdPage(p)) {
                val shortIndex = getShortIndex(p)
                if (shortIndex < shorts.itemCount) {
                    val post = shorts[shortIndex]
                    if (post != null && post.mediaUrls.isNotEmpty()) {
                        urls.add(post.mediaUrls[0])
                    }
                }
            }
        }
        ShortsVideoCache.preload(context, urls)
    }

    VerticalPager(
        state = pagerState, 
        beyondViewportPageCount = 1,
        modifier = Modifier.fillMaxSize(),
        key = { page -> 
            if (isAdPage(page)) "ad_$page"
            else {
                val idx = getShortIndex(page)
                if (idx < shorts.itemCount) shorts[idx]?.id ?: "short_$page"
                else "empty_$page"
            }
        }
    ) { page ->
        if (isAdPage(page)) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black)
                    .padding(bottom = bottomPadding) // REQUIREMENT: Avoid nav bar clipping
            ) {
                val adIndex = page / adStride
                if (adIndex < validPreloadedAds.size) {
                    com.tpsk.tj.common.ui.components.StaticAdCard(
                    nativeAd = validPreloadedAds[adIndex],
                        modifier = Modifier.fillMaxSize(),
                        layoutResId = com.tpsk.tj.R.layout.item_short_native_ad
                    )
                }
            }
        } else {
            val shortIndex = getShortIndex(page)
            val currentShort = if (shortIndex < shorts.itemCount) shorts[shortIndex] else null
            
            if (currentShort != null) {
                // Allow interaction for all types that are rendered as ShortItem
                val isInteractiveShort = currentShort.type != com.tpsk.tj.media.model.PostType.AD_UNIT && currentShort.type != com.tpsk.tj.media.model.PostType.EXTERNAL_AD_PLACEHOLDER
                
                val isOwn = currentShort.authorId == ownUserId
                android.util.Log.d("ShortsList", "Page $page: authorId=${currentShort.authorId}, ownUserId=$ownUserId, isOwn=$isOwn")

                ShortItem(
                    adManager = adManager,
                    short = currentShort,
                    currentUserAvatarUrl = currentUserAvatarUrl,
                    ownUserId = ownUserId,
                    isOwnPost = isOwn,
                    isVisible = pagerState.currentPage == page, 
                    bottomInset = bottomPadding, 
                    onLikeClick = { if (isInteractiveShort) shortsViewModel.likePost(it, currentShort.likes, currentShort.isLiked) }, 
                    onSubscribeClick = { id -> shortsViewModel.followUser(id) }, 
                    onCommentClick = { if (isInteractiveShort) onShowComments(it) }, 
                    onBookmarkClick = { if (isInteractiveShort) shortsViewModel.bookmarkPost(it) }, 
                    onShareClick = { id, count, liked -> 
                        shortsViewModel.repostPost(id, count, liked)
                    },
                    onExternalShareClick = { id, count ->
                        shortsViewModel.sharePost(id, count)
                    },
                    onMoreClick = { onShowMoreMenu(currentShort) }, 
                    onCreatorClick = onCreatorClick,
                    onEngagement = { pid, type, time, comp -> 
                        if (type == "view") {
                            shortsViewModel.viewPost(pid)
                        } else {
                            shortsViewModel.trackEngagement(pid, type, currentShort.category, time, comp)
                        }
                    },
                    isGlobalPaused = isGlobalPaused
                )
            }
        }
    }
}
