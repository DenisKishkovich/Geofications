package com.example.geofications

import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.core.app.NotificationCompat
import androidx.navigation.NavDeepLinkBuilder

// Extension function to send messages
/**
 * Builds and delivers the notification.
 */
fun NotificationManager.sendNotification(notificationId: Long = 0L, messageTitle: String = "Notification", messageBody: String = "", applicationContext: Context) {

    val INTENT_ACTION_COMPLETED = "completed"

    val appbarTitleArg = applicationContext.getString(R.string.edit_notification)

    val argsBundle = Bundle()
    argsBundle.putLong("geoficationID", notificationId)
    argsBundle.putString("appbar_title", appbarTitleArg)

    val contentPendingIntent = NavDeepLinkBuilder(applicationContext)
        .setComponentName(MainActivity::class.java)
        .setGraph(R.navigation.nav_graph)
        .setDestination(R.id.geoficationDetailsFragment)
        .setArguments(argsBundle)
        .createPendingIntent()

    val completedStateIntent = Intent(applicationContext, CompletedStateReceiver::class.java).apply {
        action = INTENT_ACTION_COMPLETED
        putExtra("id", notificationId) // notificationId == geoficationId
    }

    val completedStatePendingIntent: PendingIntent = PendingIntent.getBroadcast(
        applicationContext,
        notificationId.toInt(), // REQUEST CODE for multiple pending intents from multiple geofications
        completedStateIntent,
        PendingIntent.FLAG_ONE_SHOT or PendingIntent.FLAG_IMMUTABLE
    )

    val builder = NotificationCompat.Builder(
        applicationContext,
        applicationContext.getString(R.string.on_time_notification_channel_id)
    )
        .setSmallIcon(R.drawable.ic_launcher_foreground)
        .setContentTitle(messageTitle)
        .setContentIntent(contentPendingIntent)
        .setAutoCancel(true)
        .setPriority(NotificationCompat.PRIORITY_HIGH)
        .addAction(R.drawable.ic_launcher_foreground, "Completed", completedStatePendingIntent)

    if (messageBody.isNotEmpty()) {
        builder.setContentText(messageBody)
    }

    notify(notificationId.toInt(), builder.build())
}

fun NotificationManager.cancelNotification(notificationId: Int) {
    cancel(notificationId)
}