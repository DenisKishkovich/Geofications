package com.deniskishkovich.geofications

import android.app.job.JobInfo
import android.app.job.JobScheduler
import android.content.BroadcastReceiver
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.os.PersistableBundle
import java.util.concurrent.TimeUnit

class BootReceiver: BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val intentAction = intent.action ?: ""
        if (intentAction != Intent.ACTION_BOOT_COMPLETED) {
            return
        }
        createJob(context, intentAction)
    }

    private fun createJob(context: Context, intentAction: String) {
        val jobScheduler = context.getSystemService(Context.JOB_SCHEDULER_SERVICE) as JobScheduler
        val jobInfo = JobInfo.Builder(hashCode(), ComponentName(context, BootJobService::class.java))

        // Passing extras to JobService
        val jobBundle = PersistableBundle().apply {
            putString("intentAction", intentAction)
        }

        val job = jobInfo.setRequiresCharging(false)
            .setOverrideDeadline(TimeUnit.SECONDS.toMillis(60))
            .setExtras(jobBundle)
            .build()

        jobScheduler.schedule(job)
    }
}