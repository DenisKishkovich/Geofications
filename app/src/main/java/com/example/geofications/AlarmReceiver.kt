package com.example.geofications

import android.app.NotificationManager
import android.app.job.JobInfo
import android.app.job.JobScheduler
import android.content.BroadcastReceiver
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.os.PersistableBundle
import android.util.Log
import androidx.core.content.ContextCompat
import java.util.concurrent.TimeUnit

class AlarmReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {

        val notificationId = intent.extras?.getLong("id") ?: 0
        val notificationTitle = intent.extras?.getString("title") ?: ""
        val notificationDescription = intent.extras?.getString("description") ?: ""

        val notificationManager = ContextCompat.getSystemService(
            context,
            NotificationManager::class.java
        ) as NotificationManager

        // Change isTimeNotificationSet status in db
        createJob(context, notificationId, false)

        notificationManager.sendNotification(notificationId,notificationTitle, notificationDescription, context)
    }

    /**
     * Creating job to change isTimeNotificationSet status in db
     */
    private fun createJob(context: Context, id: Long, isNotificationSet: Boolean) {
        val jobScheduler = context.getSystemService(Context.JOB_SCHEDULER_SERVICE) as JobScheduler
        val jobInfo = JobInfo.Builder(id.toInt(), ComponentName(context, UpdateGeoficationJobService::class.java))

        val jobBundle = PersistableBundle()  // Passing extras to JobService
        jobBundle.putLong("id", id)
        jobBundle.putBoolean("isNotificationSet", isNotificationSet)

        // Building a job
        val job = jobInfo.setRequiresCharging(false)
            .setOverrideDeadline(TimeUnit.SECONDS.toMillis(10))
            .setExtras(jobBundle)
            .build()

        jobScheduler.schedule(job)
    }
}