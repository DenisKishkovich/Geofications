package com.example.geofications

import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat

// Extension function to send messages
/**
 * Builds and delivers the notification.
 *
 * @param context, activity context.
 */
fun NotificationManager.sendNotification(notificationId: Int, messageBody: String, applicationContext: Context) {

    val contentIntent = Intent(applicationContext, MainActivity::class.java)

    val contentPendingIntent = PendingIntent.getActivity(
        applicationContext,
        notificationId,
        contentIntent,
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
    )

    val builder = NotificationCompat.Builder(
        applicationContext,
        applicationContext.getString(R.string.on_time_notification_channel_id)
    )
        .setSmallIcon(R.drawable.ic_launcher_foreground)
        .setContentTitle("Test title") //TODO set geof. title
        .setContentText(messageBody)
        .setContentIntent(contentPendingIntent)
        .setAutoCancel(true)
        .setPriority(NotificationCompat.PRIORITY_HIGH)

    notify(notificationId, builder.build())
}

fun NotificationManager.cancelNotification(notificationId: Int) {
    cancel(notificationId)
}