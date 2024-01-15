package com.deniskishkovich.geofications.ui.main

import android.Manifest
import android.app.AlarmManager
import android.app.Application
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.util.Log
import androidx.core.app.ActivityCompat
import androidx.core.app.AlarmManagerCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.deniskishkovich.geofications.AlarmReceiver
import com.deniskishkovich.geofications.GeofenceBroadcastReceiver
import com.deniskishkovich.geofications.cancelNotification
import com.deniskishkovich.geofications.data.Geofication
import com.deniskishkovich.geofications.data.GeoficationDao
import com.google.android.gms.location.Geofence
import com.google.android.gms.location.GeofencingRequest
import com.google.android.gms.location.LocationServices
import com.google.android.gms.maps.model.LatLng
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.concurrent.TimeUnit

class MainViewModel(val database: GeoficationDao,private val application: Application) :
    AndroidViewModel(application) {

    private val INTENT_ACTION_DATE_TIME = "com.deniskishkovich.action.datetime"
    private val INTENT_ACTION_LOCATION = "com.deniskishkovich.action.geofence"

    private val notifyAlarmIntent = Intent(application, AlarmReceiver::class.java)
    private val alarmManager = application.getSystemService(Context.ALARM_SERVICE) as AlarmManager
    private val geofencingClient = LocationServices.getGeofencingClient(application)
    private val notifyLocationIntent = Intent(application, GeofenceBroadcastReceiver::class.java)


    /**
     * List of geofications from database
     */
    val geoficationList = database.getAllGeofications()

    /**
     * Variable that tells the Fragment to navigate to a specific GeoficationDeatails Fragment.
     * This is private because we don't want to expose setting this value to the Fragment.
     */
    private val _navigateToGeoficationDetails = MutableLiveData<Long?>()

    /**
     * If this is non-null, immediately navigate to GeoficationDeatails Fragment and call [onGeoficationNavigated]
     */
    val navigateToGeoficationDetails
        get() = _navigateToGeoficationDetails

    /**
     * Deletes all completed geofications
     */
    private suspend fun deleteCompletedFromDb() {
        withContext(Dispatchers.IO) {
            val geoficationListToDelete = database.selectCompletedGeofications()
            database.deleteCompletedGeofications()

            for (geofication in geoficationListToDelete) {
                if (geofication.isTimeNotificationSet) {
                    cancelDateTimeNotificationAndAlarm(geofication)
                }
                if (geofication.isLocationNotificationSet) {
                    removeGeofence(geofication)
                }
            }
        }
    }

    /**
     * Update completed state of geof. in db
     */
    private suspend fun updateIsCompletedInDb(geofication: Geofication, completed: Boolean) {
        withContext(Dispatchers.IO) {
            database.updateCompleted(geofication.id, completed)
        }
    }

    /**
     * Delete completed geofications
     */
    fun deleteCompleted() {
        viewModelScope.launch {
            deleteCompletedFromDb()
        }
    }

    /**
     * Update completed
     */
    fun completeGeofication(geofication: Geofication, completed: Boolean) {
        viewModelScope.launch {
            updateIsCompletedInDb(geofication, completed)
        }
    }

    /**
     * Delete geofication on swiped
     */
    fun swipeDeleteGeofication(geofication: Geofication) {
        val geoficationId = geofication.id
        if (geoficationId < 1) {
            return
        }

        if (geofication.isTimeNotificationSet) {
            cancelDateTimeNotificationAndAlarm(geofication)
        }
        if (geofication.isLocationNotificationSet) {
            removeGeofence(geofication)
        }
        viewModelScope.launch {
            deleteGeoficationFromDb(geoficationId)
        }
    }

    /**
     * Cancels time notification and alarm
     */
    private fun cancelDateTimeNotificationAndAlarm(geofication: Geofication) {
        val notificationManager = ContextCompat.getSystemService(
            application,
            NotificationManager::class.java
        ) as NotificationManager
        notificationManager.cancelNotification(geofication.id.toInt())
        alarmManager.cancel(createPendingIntentForDateTimeAlarm(geofication))
    }

    /**
     * Creates pending intent for time notification
     */
    private fun createPendingIntentForDateTimeAlarm(geofication: Geofication): PendingIntent {
        notifyAlarmIntent.apply {
            action = INTENT_ACTION_DATE_TIME
            putExtra("id", geofication.id)
            putExtra("title", geofication.title)
            putExtra("description", geofication.description)
        }
        return PendingIntent.getBroadcast(
            getApplication(),
            geofication.id.toInt(), // REQUEST CODE for multiple pending intents from multiple geofications
            notifyAlarmIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    /**
     * Removes geofication's geofence
     */
    private fun removeGeofence(geofication: Geofication) {
        geofencingClient.removeGeofences(createPendingIntentForGeofence(geofication)).run {
            addOnSuccessListener {
                Log.i("MainViewModel", "Geofence is removed")
            }
            addOnFailureListener {
                Log.e("MainViewModel", "Failed to remove the geofence")
            }
        }
    }

    /**
     * creates pending intent for geofence
     */
    private fun createPendingIntentForGeofence(geofication: Geofication): PendingIntent {
        notifyLocationIntent.apply {
            action = INTENT_ACTION_LOCATION
            putExtra("id", geofication.id)
            putExtra("title", geofication.title)
            putExtra("description", geofication.description)
        }
        return PendingIntent.getBroadcast(
            getApplication(),
            geofication.id.toInt(),
            notifyLocationIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_MUTABLE
        )
    }

    /**
     * Delete geofication from database by id
     */
    private suspend fun deleteGeoficationFromDb(geoficationId: Long) {
        withContext(Dispatchers.IO) {
            database.deleteGeoficationById(geoficationId)
        }
    }

    /**
     * Return deleted by swipe geofication to database
     */
    fun undoDeleteGeofication(geofication: Geofication) {
        viewModelScope.launch {
            insertGeofication(geofication)
        }
    }

    /**
     * Insert geofication into database
     */
    private suspend fun insertGeofication(geofication: Geofication) {
        withContext(Dispatchers.IO) {
            database.insertGeofication(geofication)
            if (geofication.isLocationNotificationSet) {
                startNotificationCountdown(geofication)
            }
            if (geofication.isLocationNotificationSet) {
                createGeofence(geofication)
            }
        }
    }

    /**
     * Starts timer for notification
     */
    private fun startNotificationCountdown(geofication: Geofication) {
        if (!geofication.isLocationNotificationSet) {
            return
        }

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

    /**
     * Creates geofence for geofication
     */
    private fun createGeofence(geofication: Geofication) {
        // Check permissions
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            if (ActivityCompat.checkSelfPermission(
                    application,
                    Manifest.permission.ACCESS_FINE_LOCATION
                ) != PackageManager.PERMISSION_GRANTED ||
                ActivityCompat.checkSelfPermission(
                    application,
                    Manifest.permission.ACCESS_BACKGROUND_LOCATION
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                return
            }
        } else {
            if (ActivityCompat.checkSelfPermission(
                    application,
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

        // Create new geofence
        geofencingClient.addGeofences(geofencingRequest, createPendingIntentForGeofence(geofication))
    }

    /**
     * Here goes Geofication id from recycler view adapter's click listener
     */
    fun onGeoficationClicked(id: Long) {
        _navigateToGeoficationDetails.value = id
    }

    /**
     * Method to navigate and transfer value -1 to GeoficationDetailsFragment if FAB is clicked
     */
    fun onFabClicked() {
        _navigateToGeoficationDetails.value = -1L
    }

    /**
     * Call this immediately after navigating to GeoficationDeatails Fragment
     * It will clear the navigation request, so if the user rotates their phone it won't navigate
     * twice.
     */
    fun onGeoficationNavigated() {
        _navigateToGeoficationDetails.value = null
    }
}