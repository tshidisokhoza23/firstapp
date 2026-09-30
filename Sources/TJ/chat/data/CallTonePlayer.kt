package com.tpsk.tj.chat.data

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFocusRequest
import android.media.AudioManager
import android.media.ToneGenerator
import android.os.Build
import java.util.Timer
import java.util.TimerTask

class CallTonePlayer(context: Context) {
    private val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
    private var toneGenerator: ToneGenerator? = null
    private var timer: Timer? = null
    private var audioFocusRequest: AudioFocusRequest? = null

    private fun requestFocus() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val attributes = AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_VOICE_COMMUNICATION)
                .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                .build()
            audioFocusRequest = AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN_TRANSIENT_MAY_DUCK)
                .setAudioAttributes(attributes)
                .build()
            audioManager.requestAudioFocus(audioFocusRequest!!)
        } else {
            @Suppress("DEPRECATION")
            audioManager.requestAudioFocus(null, AudioManager.STREAM_VOICE_CALL, AudioManager.AUDIOFOCUS_GAIN_TRANSIENT_MAY_DUCK)
        }
    }

    private fun abandonFocus() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            audioFocusRequest?.let { audioManager.abandonAudioFocusRequest(it) }
        } else {
            @Suppress("DEPRECATION")
            audioManager.abandonAudioFocus(null)
        }
    }

    fun playOutgoingRingtone() {
        stop()
        requestFocus()
        try {
            toneGenerator = ToneGenerator(AudioManager.STREAM_VOICE_CALL, 80)
            timer = Timer()
            timer?.schedule(object : TimerTask() {
                override fun run() {
                    toneGenerator?.startTone(ToneGenerator.TONE_SUP_PIP)
                }
            }, 0, 2000)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun playBusyTone() {
        stop()
        requestFocus()
        try {
            toneGenerator = ToneGenerator(AudioManager.STREAM_VOICE_CALL, 80)
            toneGenerator?.startTone(ToneGenerator.TONE_SUP_BUSY, 2000)
            
            // Auto-stop after tone finishes (2s) to release focus
            timer = Timer()
            timer?.schedule(object : TimerTask() {
                override fun run() {
                    stop()
                }
            }, 2500)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun stop() {
        abandonFocus()
        timer?.cancel()
        timer = null
        toneGenerator?.stopTone()
        toneGenerator?.release()
        toneGenerator = null
    }
}
