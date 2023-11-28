package com.deniskishkovich.geofications

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
import com.google.android.gms.location.Geofence
import com.google.android.gms.location.GeofenceStatusCodes
import com.google.android.gms.location.GeofencingEvent
import com.google.android.gms.location.LocationServices
import java.util.concurrent.TimeUnit

class GeofenceBroadcastReceiver: BroadcastReceiver() {

    private val INTENT_ACTION_LOCATION = "com.deniskishkovich.action.geofence"
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != INTENT_ACTION_LOCATION) {
            return
        }

        val geofencingEvent = GeofencingEvent.fromIntent(intent)

        if (geofencingEvent!!.hasError()) {
            val errorMessage = GeofenceStatusCodes
                .getStatusCodeString(geofencingEvent.errorCode)
            Log.e("GeofenceReceiver", errorMessage)
            return
        }

        if (geofencingEvent.geofenceTransition != Geofence.GEOFENCE_TRANSITION_ENTER) {
            return
        }

        val geofencingClient = LocationServices.getGeofencingClient(context)

        val intentAction = intent.action ?: ""
        val geoficationId = intent.extras?.getLong("id") ?: 0
        val notificationTitle = intent.extras?.getString("title") ?: ""
        val notificationDescription = intent.extras?.getString("description") ?: ""


        val notificationManager = ContextCompat.getSystemService(
            context,
            NotificationManager::class.java
        ) as NotificationManager

        notificationManager.sendNotification(geoficationId, notificationTitle, notificationDescription, context)

        geofencingClient.removeGeofences(listOf(geoficationId.toString())).run {
            addOnSuccessListener {
                Log.i("GeofenceReceiver", "Geofence is removed")
            }
            addOnFailureListener {
                Log.e("GeofenceReceiver", "Failed to remove the geofence")
            }
        }

        createJob(context, geoficationId, false, intentAction)
    }

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