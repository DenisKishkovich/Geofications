package com.deniskishkovich.geofications

import android.Manifest
import android.app.AlarmManager
import android.app.PendingIntent
import android.app.job.JobParameters
import android.app.job.JobService
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.ActivityCompat
import androidx.core.app.AlarmManagerCompat
import com.deniskishkovich.geofications.data.Geofication
import com.deniskishkovich.geofications.data.GeoficationDao
import com.deniskishkovich.geofications.data.GeoficationDatabase
import com.google.android.gms.location.Geofence
import com.google.android.gms.location.GeofencingClient
import com.google.android.gms.location.GeofencingRequest
import com.google.android.gms.location.LocationServices
import com.google.android.gms.maps.model.LatLng
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.concurrent.TimeUnit

class BootJobService: JobService() {

    private val INTENT_ACTION_DATE_TIME = "com.deniskishkovich.action.datetime"
    private val INTENT_ACTION_LOCATION = "com.deniskishkovich.action.geofence"

    // Params to pass to jobFinished()
    private lateinit var params: JobParameters

    // Scope for coroutine in which database is worked on
    private val supervisorJob = SupervisorJob()
    private val serviceScope = CoroutineScope(Dispatchers.IO + supervisorJob)

    override fun onStartJob(jobParameters: JobParameters): Boolean {
        params = jobParameters

        val intentAction = jobParameters.extras.getString("intentAction")

        // DAO
        val dataSource = GeoficationDatabase.getInstance(applicationContext).geoficationDAO

        return when (intentAction) {
            Intent.ACTION_BOOT_COMPLETED -> {
                serviceScope.launch {
                    loadGeoficationListAndRegisterNotifications(dataSource)
                }
                true
            }
            else -> false
        }
    }

    override fun onStopJob(jobParameters: JobParameters): Boolean {
        return false
    }

    /**
     * Check if notification's "is set" value is true in database and re-register processes for notifying
     */
    private suspend fun loadGeoficationListAndRegisterNotifications(dataSource: GeoficationDao) {
        withContext(Dispatchers.IO) {
            val alarmManager = applicationContext.getSystemService(Context.ALARM_SERVICE) as AlarmManager
            val geofencingClient = LocationServices.getGeofencingClient(applicationContext)

            val geoficationList = dataSource.getAllGeoficationsSuspend()

            for (geofication in geoficationList) {
                if (geofication.isTimeNotificationSet) {
                    val notifyPendingIntentAlarm = createPendingIntentForDateTimeAlarm(geofication)

                    geofication.timestampToNotify?.let {
                        AlarmManagerCompat.setExactAndAllowWhileIdle(
                            alarmManager,
                            AlarmManager.RTC_WAKEUP,
                            it,
                            notifyPendingIntentAlarm
                        )
                    }

                }
                if (geofication.isLocationNotificationSet) {
                    createGeofence(geofencingClient, geofication)
                }
            }

            // Notify the system when our work is finished, so that it can release the resources. It is used when "true" is returned from onStartJob
            jobFinished(params, false)
        }
    }

    /**
     * Create geofence for geofication
     */
    private fun createGeofence(geofencingClient: GeofencingClient, geofication: Geofication) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            if (ActivityCompat.checkSelfPermission(
                    applicationContext,
                    Manifest.permission.ACCESS_FINE_LOCATION
                ) != PackageManager.PERMISSION_GRANTED ||
                ActivityCompat.checkSelfPermission(
                    applicationContext,
                    Manifest.permission.ACCESS_BACKGROUND_LOCATION
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                return
            }
        } else {
            if (ActivityCompat.checkSelfPermission(
                    applicationContext,
                    Manifest.permission.ACCESS_FINE_LOCATION
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                return
            }
        }

        if (geofication.latitude == null || geofication.longitude == null || geofication.locationString == null) {
            return
        }

        val latLngWhereNotify = LatLng(geofication.latitude!!, geofication.longitude!!)

        val geofence = Geofence.Builder()
            // Set the request ID of the geofence. This is a string to identify this
            // geofence.
            .setRequestId(geofication.id.toString())
            // Set the circular region of this geofence.
            .setCircularRegion(
                latLngWhereNotify.latitude,
                latLngWhereNotify.longitude,
                150f
            )
            // Set the expiration duration of the geofence. This geofence gets automatically
            // removed after this period of time.
            .setExpirationDuration(TimeUnit.DAYS.toMillis(30))
            // Set the transition types of interest. Alerts are only generated for these
            // transition.
            .setTransitionTypes(Geofence.GEOFENCE_TRANSITION_ENTER)
            // Create the geofence.
            .build()

        val geofencingRequest = GeofencingRequest.Builder().apply {
            setInitialTrigger(GeofencingRequest.INITIAL_TRIGGER_ENTER)
            addGeofence(geofence)
        }.build()

        // Delete old geofence
        geofencingClient.removeGeofences(createPendingIntentForGeofence(geofication))

        // Create new geofence
        geofencingClient.addGeofences(geofencingRequest, createPendingIntentForGeofence(geofication))
    }

    private fun createPendingIntentForDateTimeAlarm(geofication: Geofication): PendingIntent {
        val notifyAlarmIntent = Intent(applicationContext, AlarmReceiver::class.java)
        notifyAlarmIntent.apply {
            action = INTENT_ACTION_DATE_TIME
            putExtra("id", geofication.id)
            putExtra("title", geofication.title)
            putExtra("description", geofication.description)
        }
        return PendingIntent.getBroadcast(
            applicationContext,
            geofication.id.toInt(), // REQUEST CODE for multiple pending intents from multiple geofications
            notifyAlarmIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    private fun createPendingIntentForGeofence(geofication: Geofication): PendingIntent {
        val notifyLocationIntent = Intent(applicationContext, GeofenceBroadcastReceiver::class.java)
        notifyLocationIntent.apply {
            action = INTENT_ACTION_LOCATION
            putExtra("id", geofication.id)
            putExtra("title", geofication.title)
            putExtra("description", geofication.description)
        }
        return PendingIntent.getBroadcast(
            applicationContext,
            geofication.id.toInt(),
            notifyLocationIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_MUTABLE
        )
    }

}