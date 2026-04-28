package com.example.firstapp.vocab.ui.util

import android.speech.tts.TextToSpeech
import android.util.Log
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import java.util.Locale

@Stable
class LessonSpeechPlayer(
    val isReady: Boolean,
    private val speakAction: (String) -> Unit
) {
    fun speak(text: String) {
        if (isReady && text.isNotBlank()) {
            speakAction(text)
        }
    }
}

@Composable
fun rememberLessonSpeechPlayer(targetLanguage: String): LessonSpeechPlayer {
    val context = LocalContext.current.applicationContext
    var isReady by remember { mutableStateOf(false) }
    val textToSpeechHolder = remember { mutableStateOf<TextToSpeech?>(null) }

    DisposableEffect(context, targetLanguage) {
        isReady = false
        val engine = TextToSpeech(context) { status ->
            if (status == TextToSpeech.SUCCESS) {
                val textToSpeech = textToSpeechHolder.value ?: return@TextToSpeech
                val selectedLocale = selectSupportedLocale(textToSpeech, targetLanguage)
                if (selectedLocale != null) {
                    textToSpeech.language = selectedLocale
                }
                textToSpeech.setSpeechRate(0.9f)
                isReady = true
            } else {
                Log.w("LessonSpeechPlayer", "TextToSpeech initialization failed with status=$status")
                isReady = false
            }
        }
        textToSpeechHolder.value = engine

        onDispose {
            isReady = false
            textToSpeechHolder.value?.stop()
            textToSpeechHolder.value?.shutdown()
            textToSpeechHolder.value = null
        }
    }

    return remember(isReady, targetLanguage) {
        LessonSpeechPlayer(
            isReady = isReady,
            speakAction = { text ->
                val textToSpeech = textToSpeechHolder.value ?: return@LessonSpeechPlayer
                val result = textToSpeech.speak(
                    text,
                    TextToSpeech.QUEUE_FLUSH,
                    null,
                    "lesson-audio-${text.hashCode()}"
                )
                if (result == TextToSpeech.ERROR) {
                    Log.w("LessonSpeechPlayer", "Failed to speak text for language=$targetLanguage")
                }
            }
        )
    }
}

private fun selectSupportedLocale(engine: TextToSpeech, language: String): Locale? {
    val candidates = localeCandidates(language) + listOf(Locale.getDefault(), Locale.US)
    return candidates.firstOrNull { locale ->
        engine.isLanguageAvailable(locale) >= TextToSpeech.LANG_AVAILABLE
    }
}

private fun localeCandidates(language: String): List<Locale> {
    return when (language) {
        "Spanish" -> listOf(Locale("es", "ES"), Locale("es", "US"), Locale("es"))
        "French" -> listOf(Locale("fr", "FR"), Locale.CANADA_FRENCH, Locale("fr"))
        "German" -> listOf(Locale("de", "DE"), Locale.GERMANY, Locale("de"))
        "Italian" -> listOf(Locale("it", "IT"), Locale.ITALY, Locale("it"))
        "Portuguese" -> listOf(Locale("pt", "BR"), Locale("pt", "PT"), Locale("pt"))
        "Afrikaans" -> listOf(Locale("af", "ZA"), Locale("af"), Locale("en", "ZA"))
        else -> listOf(Locale.US, Locale.UK)
    }
}
