package com.example.firstapp.data.realtime

import android.media.AudioManager
import android.media.ToneGenerator
import java.util.Timer
import java.util.TimerTask

class CallTonePlayer {
    private var toneGenerator: ToneGenerator? = null
    private var timer: Timer? = null

    fun playOutgoingRingtone() {
        stop()
        try {
            toneGenerator = ToneGenerator(AudioManager.STREAM_VOICE_CALL, 100)
            timer = Timer()
            timer?.schedule(object : TimerTask() {
                override fun run() {
                    toneGenerator?.startTone(ToneGenerator.TONE_SUP_RINGTONE)
                }
            }, 0, 3000)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun stop() {
        timer?.cancel()
        timer = null
        toneGenerator?.stopTone()
        toneGenerator?.release()
        toneGenerator = null
    }
}
