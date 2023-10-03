package com.example.geofications

import android.app.NotificationManager
import android.app.PendingIntent
import android.app.job.JobInfo
import android.app.job.JobScheduler
import android.content.BroadcastReceiver
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.os.PersistableBundle
import android.util.Log
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.core.content.getSystemService
import com.example.geofications.data.GeoficationDao
import com.example.geofications.data.GeoficationDatabase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.concurrent.TimeUnit

class AlarmReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        Log.i("MY TAG", "onReceive")

        val notificationId = intent.extras?.getInt("id") ?: 0
        val notificationTitle = intent.extras?.getString("title") ?: ""
        val notificationDescription = intent.extras?.getString("description") ?: ""

        val notificationManager = ContextCompat.getSystemService(
            context,
            NotificationManager::class.java
        ) as NotificationManager

        val jobScheduler = context.getSystemService(Context.JOB_SCHEDULER_SERVICE) as JobScheduler
        val jobInfo = JobInfo.Builder(notificationId, ComponentName(context, UpdateGeoficationJobService::class.java))
        val jobBundle = PersistableBundle()
        jobBundle.putLong("id", notificationId.toLong())
        jobBundle.putBoolean("isNotificationSet", false)
        val job = jobInfo.setRequiresCharging(false)
            .setOverrideDeadline(TimeUnit.SECONDS.toMillis(10))
            .setExtras(jobBundle)
            .build()
        jobScheduler.schedule(job)


//        val dataSource = GeoficationDatabase.getInstance(context).geoficationDAO
//
//        // Set isDateTimeAlarmSet in database to false after broadcast received
//        GlobalScope.launch {
//            updateIsDateTimeAlarmSetInDb(dataSource, notificationId.toLong(), false)
//        }


        notificationManager.sendNotification(notificationId,notificationTitle, notificationDescription, context)
        Log.i("MY TAG", "send")

    }

    /**
     * Set isTimeNotificationSet in database
     */
    private suspend fun updateIsDateTimeAlarmSetInDb(data: GeoficationDao, id: Long, isTimeNotificationSet: Boolean) {
        withContext(Dispatchers.IO) {
            data.updateIsTimeNotificationSetStatus(id, isTimeNotificationSet)
        }
    }
}