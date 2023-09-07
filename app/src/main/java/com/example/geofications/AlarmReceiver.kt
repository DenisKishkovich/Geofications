package com.example.geofications

import android.app.NotificationManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import android.widget.Toast
import androidx.core.content.ContextCompat

class AlarmReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {

        val notificationId = intent.extras?.getInt("id") ?: 0

        val notificationManager = ContextCompat.getSystemService(
            context,
            NotificationManager::class.java
        ) as NotificationManager

        notificationManager.sendNotification(notificationId,"Test notification body", context)
        Log.i("MY TAG", "onRECEIVE")
    }
}