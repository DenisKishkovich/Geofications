package com.example.geofications

import android.app.Notification
import android.app.NotificationManager
import android.content.Context
import androidx.core.app.NotificationCompat

private val NOTIFICATION_ID = 0

// Extension function to send messages
/**
 * Builds and delivers the notification.
 *
 * @param context, activity context.
 */
fun NotificationManager.sendNotification(messageBody: String, appliationContext: Context) {
    val builder = NotificationCompat.Builder(
        appliationContext,
        appliationContext.getString(R.string.on_time_notification_channel_id)
    )
        .setSmallIcon(R.drawable.ic_launcher_foreground)
        .setContentTitle("Test title") //TODO set geof. title
        .setContentText(messageBody)

    notify(NOTIFICATION_ID, builder.build())
}