package com.tpsk.tj.common.data.repository

import com.tpsk.tj.common.data.api.AiApi
import com.tpsk.tj.common.data.api.AiAssistRequest
import com.tpsk.tj.common.data.api.AiAssistResponse
import com.tpsk.tj.common.data.api.ArtistHelperRequest
import com.tpsk.tj.common.data.api.ArtistHelperResponse
import com.tpsk.tj.common.util.Resource
import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

@EntryPoint
@InstallIn(SingletonComponent::class)
interface AiRepositoryEntryPoint {
    fun aiRepository(): AiRepository
}

@Composable
fun rememberAiRepository(): AiRepository {
    val context = LocalContext.current.applicationContext
    return remember(context) { aiRepository(context) }
}

fun aiRepository(context: Context): AiRepository {
    return EntryPointAccessors.fromApplication(context, AiRepositoryEntryPoint::class.java).aiRepository()
}

@Singleton
class AiRepository @Inject constructor(
    private val aiApi: AiApi?
) {
    suspend fun runAiAssist(action: String, text: String, tone: String = "Casual"): Resource<AiAssistResponse> {
        return withContext(Dispatchers.IO) {
            try {
                val api = aiApi ?: return@withContext Resource.Error("AI is not connected")
                val response = api.runAiAssist(AiAssistRequest(action, text, tone))
                val body = response.body()
                if (response.isSuccessful && body != null) {
                    Resource.Success(body)
                } else {
                    Resource.Error(apiError(response))
                }
            } catch (e: Exception) {
                Resource.Error(e.message ?: "AI request failed")
            }
        }
    }

    suspend fun getSmartReplies(postId: String, postContent: String): Resource<List<String>> {
        return withContext(Dispatchers.IO) {
            try {
                val api = aiApi ?: return@withContext Resource.Error("AI is not connected")
                val response = api.getSmartReplies(postId)
                val body = response.body()
                if (response.isSuccessful && body != null) {
                    Resource.Success(body.suggestions)
                } else {
                    Resource.Error(apiError(response))
                }
            } catch (e: Exception) {
                Resource.Error(e.message ?: "Could not load replies")
            }
        }
    }

    suspend fun runArtistHelper(
        taskType: String,
        title: String? = null,
        currentStage: String? = null,
        theme: String? = null,
        medium: String? = null,
        price: String? = null,
        story: String? = null
    ): Resource<ArtistHelperResponse> {
        return withContext(Dispatchers.IO) {
            try {
                val api = aiApi ?: return@withContext Resource.Error("AI is not connected")
                val request = ArtistHelperRequest(taskType, title, currentStage, theme, medium, price, story)
                val response = api.runArtistHelper(request)
                val body = response.body()
                if (response.isSuccessful && body != null) {
                    Resource.Success(body)
                } else {
                    Resource.Error(apiError(response))
                }
            } catch (e: Exception) {
                Resource.Error(e.message ?: "Artist helper failed")
            }
        }
    }

    private fun apiError(response: retrofit2.Response<*>): String {
        val raw = response.errorBody()?.string().orEmpty()
        val message = Regex("\"error\"\\s*:\\s*\"([^\"]+)\"").find(raw)?.groupValues?.getOrNull(1)
        return message ?: raw.take(180).ifBlank { "Request failed (${response.code()})" }
    }

}
