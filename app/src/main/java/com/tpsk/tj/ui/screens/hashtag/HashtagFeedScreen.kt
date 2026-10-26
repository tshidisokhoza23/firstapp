package com.tpsk.tj.ui.screens.hashtag

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tpsk.tj.MainActivity
import com.tpsk.tj.common.data.ads.AdMobManagerImpl
import com.tpsk.tj.media.data.api.ContestApi
import com.tpsk.tj.media.data.api.SocialApi
import com.tpsk.tj.media.data.repository.mapApiPost
import com.tpsk.tj.media.model.Post
import com.tpsk.tj.media.ui.components.ContestTags
import com.tpsk.tj.media.ui.components.PostItem
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class HashtagFeedState(
    val isLoading: Boolean = true,
    val posts: List<Post> = emptyList(),
    val error: String? = null,
    val openContest: Boolean = false
)

@HiltViewModel
class HashtagFeedViewModel @Inject constructor(
    private val socialApi: SocialApi,
    private val contestApi: ContestApi
) : ViewModel() {
    private val _state = MutableStateFlow(HashtagFeedState())
    val state: StateFlow<HashtagFeedState> = _state.asStateFlow()

    fun load(tag: String) {
        viewModelScope.launch {
            val clean = tag.removePrefix("#").trim()
            _state.value = HashtagFeedState()
            val contest = runCatching { contestApi.get(clean).body()?.contest }.getOrNull()
            if (contest != null) {
                ContestTags.add(contest.hashtag.ifBlank { clean })
                _state.value = HashtagFeedState(isLoading = false, openContest = true)
                return@launch
            }
            val response = runCatching {
                socialApi.getPosts(
                    page = 1,
                    following = false,
                    authorId = "",
                    feedType = "",
                    quality = com.tpsk.tj.media.ui.components.ActiveQuality.mode,
                    hashtag = clean
                )
            }.getOrNull()
            if (response == null || !response.isSuccessful) {
                _state.value = HashtagFeedState(isLoading = false, error = "Could not load #$clean")
            } else {
                _state.value = HashtagFeedState(
                    isLoading = false,
                    posts = response.body().orEmpty().map { mapApiPost(it) }
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HashtagFeedScreen(
    hashtag: String,
    onNavigateBack: () -> Unit,
    onOpenContest: () -> Unit,
    onPostClick: (String) -> Unit,
    onProfileClick: (String) -> Unit,
    onHashtagClick: (String) -> Unit,
    viewModel: HashtagFeedViewModel = hiltViewModel()
) {
    val clean = if (hashtag.startsWith("#")) hashtag else "#$hashtag"
    val state by viewModel.state.collectAsState()
    val context = LocalContext.current
    val adManager = remember(context) { (context as? MainActivity)?.adManager ?: AdMobManagerImpl() }

    LaunchedEffect(hashtag) { viewModel.load(hashtag) }
    LaunchedEffect(state.openContest) {
        if (state.openContest) onOpenContest()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(clean, fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background)
            )
        }
    ) { padding ->
        when {
            state.isLoading || state.openContest -> {
                Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            }
            state.error != null -> {
                Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                    Text(state.error ?: "Could not load posts")
                }
            }
            state.posts.isEmpty() -> {
                Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                    Text("No posts for $clean yet", modifier = Modifier.padding(24.dp))
                }
            }
            else -> {
                LazyColumn(Modifier.fillMaxSize().padding(padding)) {
                    items(state.posts, key = { it.id }) { post ->
                        PostItem(
                            adManager = adManager,
                            post = post,
                            onClick = { onPostClick(post.id) },
                            onReplyClick = { _, _ -> onPostClick(post.id) },
                            onProfileClick = onProfileClick,
                            onMediaClick = { _, _, _ -> onPostClick(post.id) },
                            onShareClick = { _ -> },
                            onHashtagClick = onHashtagClick
                        )
                    }
                }
            }
        }
    }
}
