package com.footballpluse.footballapp.util

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.footballpluse.footballapp.MainActivity
import com.footballpluse.footballapp.R

object LiveMatchNotificationManager {

    const val CHANNEL_ID = "channel_live_match_ticker"
    const val CHANNEL_NAME = "Live Match Ticker"
    const val CHANNEL_DESC = "Ongoing match notifications with live scores and elapsed time"

    fun createNotificationChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                CHANNEL_NAME,
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = CHANNEL_DESC
                setShowBadge(false)
            }
            val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
            manager?.createNotificationChannel(channel)
        }
    }

    fun showLiveMatchNotification(
        context: Context,
        matchId: Int,
        homeTeam: String,
        awayTeam: String,
        homeScore: Int?,
        awayScore: Int?,
        status: String,
        elapsed: Int?
    ) {
        createNotificationChannel(context)

        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra("match_id", matchId.toString())
        }

        val pendingIntent = PendingIntent.getActivity(
            context,
            matchId,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val scoreText = if (homeScore != null && awayScore != null) {
            "$homeTeam $homeScore - $awayScore $awayTeam"
        } else {
            "$homeTeam vs $awayTeam"
        }

        val statusText = when {
            elapsed != null && elapsed > 0 -> "$elapsed' LIVE"
            status.isNotBlank() -> status
            else -> "LIVE"
        }

        val isOngoing = status.equals("LIVE", ignoreCase = true) || (elapsed != null && elapsed > 0)

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.mipmap.icon512)
            .setContentTitle("⚽ $scoreText")
            .setContentText("Match Status: $statusText")
            .setContentIntent(pendingIntent)
            .setOngoing(isOngoing)
            .setOnlyAlertOnce(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setAutoCancel(!isOngoing)
            .build()

        try {
            NotificationManagerCompat.from(context).notify(matchId, notification)
        } catch (_: SecurityException) {
            // Android 13+ POST_NOTIFICATIONS permission not granted yet
        }
    }

    fun cancelLiveMatchNotification(context: Context, matchId: Int) {
        try {
            NotificationManagerCompat.from(context).cancel(matchId)
        } catch (_: Exception) {}
    }
}
