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

class CompletedStateReceiver: BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val intentAction = intent.action ?: ""
        val geoficationId = intent.extras?.getLong("id") ?: 0

        val notificationManager = ContextCompat.getSystemService(
            context,
            NotificationManager::class.java
        ) as NotificationManager

        // Cancel notification when it's action is clicked
        notificationManager.cancel(geoficationId.toInt())

        // Update completed state of geofication in database
        createJob(context, geoficationId, true, intentAction)
    }

    private fun createJob(context: Context, id: Long, isCompleted: Boolean, intentAction: String) {
        val jobScheduler = context.getSystemService(Context.JOB_SCHEDULER_SERVICE) as JobScheduler
        val jobInfo = JobInfo.Builder(hashCode(), ComponentName(context, UpdateGeoficationJobService::class.java))

        // Passing extras to JobService
        val jobBundle = PersistableBundle().apply {
            putLong("id", id)
            putString("intentAction", intentAction)
            putBoolean("isCompleted", isCompleted)
        }

        // Building a job
        val job = jobInfo.setRequiresCharging(false)
            .setOverrideDeadline(TimeUnit.SECONDS.toMillis(10))
            .setExtras(jobBundle)
            .build()

        jobScheduler.schedule(job)
    }
}