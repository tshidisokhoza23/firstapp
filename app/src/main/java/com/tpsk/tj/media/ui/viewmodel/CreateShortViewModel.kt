package com.tpsk.tj.media.ui.viewmodel

import android.content.Context
import android.net.Uri
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tpsk.tj.account.domain.repository.AccountRepository
import com.tpsk.tj.common.util.MediaUtils
import com.tpsk.tj.common.util.Resource
import com.tpsk.tj.media.domain.repository.SocialRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch
import javax.inject.Inject
import kotlin.time.Duration.Companion.milliseconds

sealed interface PostShortUiState {
    object Idle : PostShortUiState
    data class Progress(val stage: String, val progressFraction: Float) : PostShortUiState
    object Success : PostShortUiState
    data class Error(val message: String) : PostShortUiState
}

@HiltViewModel
class CreateShortViewModel @Inject constructor(
    private val socialRepository: SocialRepository,
    private val accountRepository: AccountRepository,
    @param:ApplicationContext private val context: Context,
) : ViewModel() {

    var uiState by mutableStateOf<PostShortUiState>(PostShortUiState.Idle)
        private set

    fun uploadAndPublishShort(
        videoUri: Uri,
        caption: String,
        isPublic: Boolean = true,
        allowComments: Boolean = true,
        onComplete: () -> Unit
    ) {
        viewModelScope.launch {
            try {
                // Stage 1: Local compression & thumbnail preparation
                uiState = PostShortUiState.Progress("Compressing video...", 0.2f)

                val thumbFile = MediaUtils.getVideoThumbnail(context, videoUri)
                var thumbnailUrl: String? = null
                if (thumbFile != null) {
                    accountRepository.uploadFile(thumbFile, "thumbnails")
                        .firstOrNull { it !is Resource.Loading }?.let { thumbRes ->
                            if (thumbRes is Resource.Success && thumbRes.data != null) {
                                thumbnailUrl = thumbRes.data.url
                            }
                        }
                    if (thumbFile.exists()) thumbFile.delete()
                }

                // Stage 2: Direct Storage upload
                uiState = PostShortUiState.Progress("Uploading to server...", 0.65f)
                var uploadedMediaUrl: String? = null
                var uploadError: String? = null

                // posts is accepted by the live upload allowlist. post_type stays short, so this is not a feed post.
                accountRepository.uploadUri(context, videoUri, "posts")
                    .firstOrNull { it !is Resource.Loading }?.let { res ->
                        when (res) {
                            is Resource.Success -> uploadedMediaUrl = res.data?.url
                            is Resource.Error -> uploadError = res.message ?: "Failed to upload video"
                            else -> {}
                        }
                    }

                val mediaUrl = uploadedMediaUrl
                if (mediaUrl == null) {
                    uiState = PostShortUiState.Error(uploadError ?: "Failed to upload video to server")
                    return@launch
                }

                // Stage 3: Backend API Registration
                uiState = PostShortUiState.Progress("Publishing short...", 0.9f)
                val response = socialRepository.createPost(
                    content = caption,
                    mediaUrls = listOf(mediaUrl),
                    postType = "short",
                    mediaType = "VIDEO",
                    thumbnailUrl = thumbnailUrl
                ).firstOrNull { it !is Resource.Loading }

                if (response is Resource.Success) {
                    uiState = PostShortUiState.Progress("Short published!", 1.0f)
                    uiState = PostShortUiState.Success
                    delay(800.milliseconds) // Brief pause to show completion checkmark
                    onComplete() // Safe to exit screen strictly on success
                } else {
                    val errMsg = (response as? Resource.Error)?.message ?: "Failed to publish short"
                    uiState = PostShortUiState.Error(errMsg)
                }
            } catch (e: Exception) {
                if (e is CancellationException) {
                    throw e
                }
                uiState = PostShortUiState.Error(e.localizedMessage ?: "Network error occurred")
            }
        }
    }

    fun dismissError() {
        uiState = PostShortUiState.Idle
    }
}
