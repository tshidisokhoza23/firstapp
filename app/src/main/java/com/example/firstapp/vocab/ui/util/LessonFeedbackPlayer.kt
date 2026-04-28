package com.example.firstapp.vocab.ui.util

import android.content.Context
import android.media.AudioManager
import android.media.ToneGenerator
import android.util.Log
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext

class LessonFeedbackPlayer(private val context: Context) {
    private var toneGenerator: ToneGenerator? = null

    init {
        try {
            toneGenerator = ToneGenerator(AudioManager.STREAM_MUSIC, 100)
        } catch (e: Exception) {
            Log.e("LessonFeedbackPlayer", "Failed to initialize feedback player", e)
        }
    }

    fun playCorrect() {
        playFallbackTone(ToneGenerator.TONE_PROP_ACK)
    }

    fun playError() {
        playFallbackTone(ToneGenerator.TONE_PROP_NACK)
    }

    private fun playFallbackTone(toneType: Int) {
        try {
            toneGenerator?.startTone(toneType, 200)
        } catch (e: Exception) {
            Log.e("LessonFeedbackPlayer", "Failed to play fallback tone", e)
        }
    }

    fun release() {
        try {
            toneGenerator?.release()
        } catch (e: Exception) {
            Log.e("LessonFeedbackPlayer", "Error releasing resources", e)
        } finally {
            toneGenerator = null
        }
    }
}

@Composable
fun rememberLessonFeedbackPlayer(): LessonFeedbackPlayer {
    val context = LocalContext.current.applicationContext
    val player = remember { LessonFeedbackPlayer(context) }

    DisposableEffect(Unit) {
        onDispose {
            player.release()
        }
    }

    return player
}
