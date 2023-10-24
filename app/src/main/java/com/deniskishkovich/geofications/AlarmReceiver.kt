package com.deniskishkovich.geofications

import android.app.NotificationManager
import android.app.job.JobInfo
import android.app.job.JobScheduler
import android.content.BroadcastReceiver
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.os.PersistableBundle
import androidx.core.content.ContextCompat
import java.util.concurrent.TimeUnit

class AlarmReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val intentAction = intent.action ?: ""
        val geoficationId = intent.extras?.getLong("id") ?: 0
        val notificationTitle = intent.extras?.getString("title") ?: ""
        val notificationDescription = intent.extras?.getString("description") ?: ""

        val notificationManager = ContextCompat.getSystemService(
            context,
            NotificationManager::class.java
        ) as NotificationManager

        // Send notification
        notificationManager.sendNotification(geoficationId,notificationTitle, notificationDescription, context)

        // Change isTimeNotificationSet status in db
        createJob(context, geoficationId, false, intentAction)
    }

    /**
     * Creating job to change isTimeNotificationSet status in db
     */
    private fun createJob(context: Context, id: Long, isNotificationSet: Boolean, intentAction: String) {
        val jobScheduler = context.getSystemService(Context.JOB_SCHEDULER_SERVICE) as JobScheduler
        val jobInfo = JobInfo.Builder(hashCode(), ComponentName(context, UpdateGeoficationJobService::class.java))

        // Passing extras to JobService
        val jobBundle = PersistableBundle().apply {
            putLong("id", id)
            putBoolean("isNotificationSet", isNotificationSet)
            putString("intentAction", intentAction)
        }

        // Building a job
        val job = jobInfo.setRequiresCharging(false)
            .setOverrideDeadline(TimeUnit.SECONDS.toMillis(10))
            .setExtras(jobBundle)
            .build()

        jobScheduler.schedule(job)
    }
}