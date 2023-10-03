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
 *
 * @param context, activity context.
 */
fun NotificationManager.sendNotification(notificationId: Int = 0, messageTitle: String = "Notification", messageBody: String = "", applicationContext: Context) {

    val appbarTitleArg = applicationContext.getString(R.string.edit_notification)

    val argsBundle = Bundle()
    argsBundle.putLong("geoficationID", notificationId.toLong())
    argsBundle.putString("appbar_title", appbarTitleArg)

    val contentPendingIntent = NavDeepLinkBuilder(applicationContext)
        .setComponentName(MainActivity::class.java)
        .setGraph(R.navigation.nav_graph)
        .setDestination(R.id.geoficationDetailsFragment)
        .setArguments(argsBundle)
        .createPendingIntent()

    val builder = NotificationCompat.Builder(
        applicationContext,
        applicationContext.getString(R.string.on_time_notification_channel_id)
    )
        .setSmallIcon(R.drawable.ic_launcher_foreground)
        .setContentTitle(messageTitle)
        .setContentIntent(contentPendingIntent)
        .setAutoCancel(true)
        .setPriority(NotificationCompat.PRIORITY_HIGH)

    if (messageBody.isNotEmpty()) {
        builder.setContentText(messageBody)
    }



    notify(notificationId, builder.build())
}

fun NotificationManager.cancelNotification(notificationId: Int) {
    cancel(notificationId)
}