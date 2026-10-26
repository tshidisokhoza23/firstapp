package com.tpsk.tj.drive.ui

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.media.AudioAttributes
import android.media.Ringtone
import android.media.RingtoneManager
import android.os.Build
import android.os.PowerManager
import androidx.core.app.NotificationCompat
import com.tpsk.tj.MainActivity
import com.tpsk.tj.R
import com.tpsk.tj.drive.model.RideRequest

object RideRequestAlerts {
    private const val CHANNEL_ID = "ride_requests"
    private const val NOTIFICATION_ID = 2401
    private var ringingId: String? = null
    private var announcedId: String? = null
    private var ringtone: Ringtone? = null
    private var wakeLock: PowerManager.WakeLock? = null
    private val declined = mutableSetOf<String>()

    fun isDeclined(id: String): Boolean = id in declined

    fun decline(context: Context, id: String) {
        declined.add(id)
        if (announcedId == id || ringingId == id) stop(context)
    }

    fun alert(context: Context, request: RideRequest) {
        if (request.id.isBlank() || request.id in declined || announcedId == request.id) return
        stop(context)
        announcedId = request.id
        ringingId = request.id
        val app = context.applicationContext
        val uri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_RINGTONE)
        ringtone = RingtoneManager.getRingtone(app, uri)?.apply {
            audioAttributes = AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_NOTIFICATION_RINGTONE)
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .build()
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) isLooping = true
            play()
        }
        val pm = app.getSystemService(PowerManager::class.java)
        wakeLock = pm?.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "tj:ride-request")?.apply {
            setReferenceCounted(false)
            acquire(45_000)
        }
        val launch = Intent(app, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra("open_drive", true)
            putExtra("ride_request_id", request.id)
        }
        val pending = PendingIntent.getActivity(
            app,
            NOTIFICATION_ID,
            launch,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val manager = app.getSystemService(NotificationManager::class.java)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(CHANNEL_ID, "Ride requests", NotificationManager.IMPORTANCE_HIGH).apply {
                description = "Alerts when a rider requests a trip"
                enableVibration(true)
                setSound(null, null)
            }
            manager.createNotificationChannel(channel)
        }
        val notification = NotificationCompat.Builder(app, CHANNEL_ID)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle("New ride request")
            .setContentText("${request.passengerName}: ${request.pickupAddress} to ${request.destinationAddress}")
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setCategory(NotificationCompat.CATEGORY_CALL)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setOngoing(true)
            .setAutoCancel(false)
            .setContentIntent(pending)
            .setVibrate(longArrayOf(0, 800, 400, 800))
            .build()
        manager.notify(NOTIFICATION_ID, notification)
        android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
            silence(request.id)
        }, 45_000)
    }

    private fun silence(id: String) {
        if (ringingId != id) return
        ringtone?.stop()
        ringtone = null
        ringingId = null
        wakeLock?.let { if (it.isHeld) it.release() }
        wakeLock = null
    }

    fun stopIf(context: Context, id: String) {
        if (ringingId == id) stop(context)
    }

    fun stop(context: Context) {
        ringtone?.stop()
        ringtone = null
        ringingId = null
        announcedId = null
        wakeLock?.let { if (it.isHeld) it.release() }
        wakeLock = null
        context.applicationContext.getSystemService(NotificationManager::class.java).cancel(NOTIFICATION_ID)
    }
}
