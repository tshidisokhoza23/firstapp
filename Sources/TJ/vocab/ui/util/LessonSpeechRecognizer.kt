package com.tpsk.tj.vocab.ui.util

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.util.Log
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalContext
import java.util.*

private const val TAG = "LessonSpeechRecognizer"

@Stable
class LessonSpeechRecognizer(
    val isListening: Boolean,
    val isProcessing: Boolean,
    val rmsDb: Float,
    val partialText: String?,
    val result: String?,
    val error: String?,
    private val startListeningAction: (String) -> Unit,
    private val stopListeningAction: () -> Unit
) {
    fun startListening(language: String) {
        startListeningAction(language)
    }

    fun stopListening() {
        stopListeningAction()
    }
}

@Composable
fun rememberLessonSpeechRecognizer(onResult: (String) -> Unit): LessonSpeechRecognizer {
    val context = LocalContext.current.applicationContext
    val currentOnResult by rememberUpdatedState(onResult)
    
    var isListening by remember { mutableStateOf(false) }
    var isProcessing by remember { mutableStateOf(false) }
    var rmsDb by remember { mutableStateOf(0f) }
    var partialText by remember { mutableStateOf<String?>(null) }
    var result by remember { mutableStateOf<String?>(null) }
    var error by remember { mutableStateOf<String?>(null) }

    val speechRecognizer = remember {
        SpeechRecognizer.createSpeechRecognizer(context).apply {
            setRecognitionListener(object : RecognitionListener {
                override fun onReadyForSpeech(params: Bundle?) {
                    isListening = true
                    isProcessing = false
                    error = null
                    partialText = null
                    result = null
                }
                override fun onBeginningOfSpeech() {
                    isProcessing = true
                }
                override fun onRmsChanged(rmsdB: Float) {
                    rmsDb = rmsdB
                }
                override fun onBufferReceived(buffer: ByteArray?) {}
                override fun onEndOfSpeech() {
                    isListening = false
                }
                override fun onError(err: Int) {
                    Log.e(TAG, "SpeechRecognizer error: $err")
                    isListening = false
                    isProcessing = false
                    rmsDb = 0f
                    val errorMessage = when (err) {
                        SpeechRecognizer.ERROR_AUDIO -> "Audio recording error"
                        SpeechRecognizer.ERROR_CLIENT -> "Client side error"
                        SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS -> "Insufficient permissions"
                        SpeechRecognizer.ERROR_NETWORK -> "Network error"
                        SpeechRecognizer.ERROR_NETWORK_TIMEOUT -> "Network timeout"
                        SpeechRecognizer.ERROR_NO_MATCH -> "No match found"
                        SpeechRecognizer.ERROR_RECOGNIZER_BUSY -> "Recognizer busy"
                        SpeechRecognizer.ERROR_SERVER -> "Server error"
                        SpeechRecognizer.ERROR_SPEECH_TIMEOUT -> "No speech detected"
                        else -> "Error: $err"
                    }
                    error = errorMessage
                }
                override fun onResults(results: Bundle?) {
                    isListening = false
                    isProcessing = false
                    rmsDb = 0f
                    val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                    val bestMatch = matches?.firstOrNull()
                    result = bestMatch
                    if (bestMatch != null) {
                        currentOnResult(bestMatch)
                    } else {
                        error = "No speech recognized"
                    }
                }
                override fun onPartialResults(partialResults: Bundle?) {
                    val matches = partialResults?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                    partialText = matches?.firstOrNull()
                }
                override fun onEvent(eventType: Int, params: Bundle?) {}
            })
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            speechRecognizer.destroy()
        }
    }

    return remember(isListening, isProcessing, rmsDb, partialText, result, error) {
        LessonSpeechRecognizer(
            isListening = isListening,
            isProcessing = isProcessing,
            rmsDb = rmsDb,
            partialText = partialText,
            result = result,
            error = error,
            startListeningAction = { language ->
                if (!SpeechRecognizer.isRecognitionAvailable(context)) {
                    error = "Speech recognition not available on this device"
                    return@LessonSpeechRecognizer
                }
                
                Handler(Looper.getMainLooper()).post {
                    val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                        putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                        val locale = getLocaleForLanguage(language)
                        putExtra(RecognizerIntent.EXTRA_LANGUAGE, locale.toString())
                        putExtra(RecognizerIntent.EXTRA_LANGUAGE_PREFERENCE, locale.toString())
                        putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
                        putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 3)
                    }
                    try {
                        speechRecognizer.startListening(intent)
                    } catch (e: Exception) {
                        error = "Could not start: ${e.message}"
                    }
                }
            },
            stopListeningAction = {
                speechRecognizer.stopListening()
            }
        )
    }
}

private fun getLocaleForLanguage(language: String): Locale {
    return when (language) {
        "Spanish" -> Locale("es", "ES")
        "French" -> Locale.FRANCE
        "German" -> Locale.GERMANY
        "Italian" -> Locale.ITALY
        "Portuguese" -> Locale("pt", "BR")
        "Afrikaans" -> Locale("af", "ZA")
        else -> Locale.ENGLISH
    }
}
