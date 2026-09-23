# Shorts Module: Technical Specification & Implementation Guide

## 1. Overview & Architecture

The Shorts module delivers a high-performance, vertical full-screen video feed with smooth transitions, seamless scrolling, non-intrusive metadata overlays, interactive comment sheets, and zero-flicker caching.

---

## 2. UI / UX Layout Polish & Edge-to-Edge Design

- **Edge-to-Edge Scrim & Legibility:**
  - Remove solid navigation/status bar cutoffs using `WindowCompat.setDecorFitsSystemWindows(window, false)` with light status bar icons.
  - Apply a vertical scrim gradient (`Brush.verticalGradient(listOf(Color.Transparent, Color.Black.copy(alpha = 0.75f)))`) behind the bottom metadata (handle, caption, sound/tags) and right-rail actions (like, comments, share, options).
- **Minimalist Overlays:**
  - Remove redundant audio tags (e.g. "Original Audio - pikaso") if the platform does not feature audio remixing/sound libraries, maximizing visible video area.
  - Add expandable video captions/hashtags (`...more`) with proper contrast drop-shadow.
- **Gesture Handling:**
  - **Single tap:** Smoothly toggle play/pause with a brief centered icon indicator flash.
  - **Double tap:** Trigger animated heart popup and like action without needing to tap the side heart icon.
  - **Bottom sheet dragging:** Nested scrolling ensures dragging down dismisses the sheet rather than switching video pages in the `VerticalPager`.

---

## 3. Playback Performance & Reload Prevention

### 3.1 Eliminating Video Buffering on Scroll Back
Reloading occurs when composables leave the viewport and recreate the underlying ExoPlayer/Media3 instance or discard cache.

1. **Persistent Disk Caching (`SimpleCache` + `CacheDataSource`):**
   ```kotlin
   val cache = SimpleCache(
       File(context.cacheDir, "shorts_media_cache"),
       LeastRecentlyUsedCacheEvictor(200 * 1024 * 1024L), // 200 MB disk cache
       StandaloneDatabaseProvider(context)
   )
   val cacheDataSourceFactory = CacheDataSource.Factory()
       .setCache(cache)
       .setUpstreamDataSourceFactory(DefaultHttpDataSource.Factory())
   ```
2. **ExoPlayer Instance Pooling / Scoped Controller:**
   - Maintain a pool of 2–3 reusable player instances in a playback `ViewModel`/service instead of instantiating `remember { ExoPlayer.Builder(...) }` inside individual composable items.
3. **Pager Windowing & Key Preservation:**
   - Set `beyondViewportPageCount = 1` on `VerticalPager` to keep adjacent pages pre-buffered.
   - Use stable unique keys: `key = { index -> shorts[index].id }` to prevent UI state resets.

---

## 4. "Add New Short" Flow (Optimistic Feed Insertion)

**Never reload or re-fetch the entire feed on upload.**

1. **Optimistic Local Prepend:**
   ```kotlin
   _shortsState.update { currentList ->
       listOf(newCreatedShort) + currentList
   }
   ```
2. **Scroll & Notification Strategy:**
   - **Post & View Now:** Animate immediately to page 0: `pagerState.animateScrollToPage(0)`.
   - **Background Upload:** Keep current watching position intact and show a top pill banner: *"Short posted • Tap to view"*.
3. **Index Stability:**
   - `key = { it.id }` guarantees that adding an item at index 0 shifts subsequent indices cleanly without rebinding or restarting the actively playing video.

---

## 5. Comment Sheet & Bottom Sheets Redesign

### 5.1 Minimalist Comments Sheet Structure
```
+--------------------------------------------+
|  Comments (count)                        X |
+--------------------------------------------+
|  (Avatar)  username  •  2w         (Heart) |
|            Comment text content         12 |
|            Reply                           |
+--------------------------------------------+
| (Avatar) [ Add a comment...         ][Send]|
+--------------------------------------------+
```
- **Theme Consistency:** Match the media player's dark translucent aesthetic rather than jarring bright white sheets.
- **Rounded Input Bar:** Use `RoundedCornerShape(24.dp)` / `RoundedCornerShape(50)` pill input.
- **Removed Clutter:** Eliminate excessive floating emoji strips/GIF bars; leverage device IME keyboard support.
- **Inset Padding:** Apply `Modifier.navigationBarsPadding()` so system navigation gestures do not collide with comment inputs or send actions.

---

## 6. Lifecycle, Audio Focus & Battery Management

- **Audio Focus:** Observe `AudioManager.AUDIOFOCUS_LOSS_TRANSIENT` to pause automatically during calls or other media playback.
- **Lifecycle Pausing:** Automatically pause decoders and detach video surfaces on `ON_PAUSE` / `ON_STOP` or when switching away from the Shorts navigation tab.
- **Pre-buffering Strategy:** Pre-buffer 1–2 seconds of video $N+1$ while video $N$ plays, and paginate when user is within 3–4 items of the feed end.
