package com.example.geofications.ui.details

import android.app.AlarmManager
import android.app.Application
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.core.app.AlarmManagerCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.example.geofications.AlarmReceiver
import com.example.geofications.R
import com.example.geofications.cancelNotifications
import com.example.geofications.data.Geofication
import com.example.geofications.data.GeoficationDao
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.Calendar

class GeoficationDetailsViewModel(
    private val database: GeoficationDao,
    private val geoficationID: Long,
    private val app: Application
) : AndroidViewModel(app) {

    // REQUEST CODE for multiple pending intents from multiple geofications
    private val requestCodeNotifyOnTime = geoficationID.toInt()

    private val notifyPendingIntentAlarm: PendingIntent
    private val alarmManager = app.getSystemService(Context.ALARM_SERVICE) as AlarmManager

    var isNewGeofication: Boolean = false

    var currentCreatedTimestamp: Long = 0L

    // Two-way databinding, exposing MutableLiveData
    val title = MutableLiveData<String?>()

    // Two-way databinding, exposing MutableLiveData
    val description = MutableLiveData<String>()

    val isCompleted = MutableLiveData<Boolean>()

    private val _editedTimestamp = MutableLiveData<Long>()
    val editedTimestamp: LiveData<Long>
        get() = _editedTimestamp

    /**
     * Variable that tells to navigate to a MainFragment.
     * This is private because we don't want to expose setting this value to the Fragment.
     */
    private val _navigateToMain = MutableLiveData<Boolean>()

    /**
     * When true immediately navigate back to the MainFragment
     */
    val navigateToMain: LiveData<Boolean>
        get() = _navigateToMain

    /**
     * "Inner" variable which triggers the snackbar
     */
    private val _snackbarText = MutableLiveData<Int>()

    /**
     * When changes immediately triggers the snackbar
     */
    val snackbarText: LiveData<Int>
        get() = _snackbarText

    /**
     * "Inner" variable which triggers the toast
     */
    private val _toastText = MutableLiveData<Int>()

    /**
     * When changes immediately triggers the toast
     */
    val toastText: LiveData<Int>
        get() = _toastText

    private val _dateTimeAlarmOn = MutableLiveData<Boolean>()
    val isDateTimeAlarmOn: LiveData<Boolean>
        get() = _dateTimeAlarmOn

    private val notifyAlarmIntent = Intent(app, AlarmReceiver::class.java)




    //time selection dialog
    val hour = MutableLiveData<Int?>()
    val minute = MutableLiveData<Int?>()

    //calendar selection
    val dateInMillis = MutableLiveData<Long?>()

    init {

        if (geoficationID == -1L) {
            isNewGeofication = true
        }

        if (!isNewGeofication) {
            loadGeoficationParams(geoficationID)
        } else {
            title.value = ""
            description.value = ""
            isCompleted.value = false
        }

        _dateTimeAlarmOn.value = PendingIntent.getBroadcast(
            getApplication(),
            requestCodeNotifyOnTime,
            notifyAlarmIntent,
            PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
        ) != null

        notifyPendingIntentAlarm = PendingIntent.getBroadcast(
            getApplication(),
            requestCodeNotifyOnTime,
            notifyAlarmIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    /**
     * Method to get geof. from database by id
     */
    private suspend fun getGeofication(geoficationId: Long): Geofication? =
        withContext(Dispatchers.IO) {
            return@withContext database.getGeoficationById(geoficationId)
        }

    /**
     * Method to get geof's params by id
     */
    private fun loadGeoficationParams(geoficationID: Long) {
        viewModelScope.launch {
            val geofication = getGeofication(geoficationID)

            if (geofication != null) {
                title.value = geofication.title
                description.value = geofication.description
                isCompleted.value = geofication.isCompleted
                currentCreatedTimestamp = geofication.createdTimestamp
                _editedTimestamp.value = geofication.editedTimestamp
            } else {
                throw Exception("Geofication not found")
            }
        }
    }

    /**
     * Save geofication. It starts when save button is clicked
     */
    fun saveGeofication() {
        val currentTitle = title.value
        val currentDescription = description.value
        val currentIsCompleted = isCompleted.value

        // Null check
        if (currentTitle == null || currentDescription == null || currentIsCompleted == null) {
            _toastText.value = R.string.empty_notif_deleted
            if (!isNewGeofication) {
                deleteGeofication()
            } else {
                _navigateToMain.value = true
            }
            return
        }
        if (Geofication(title = currentTitle, description = currentDescription).isEmpty) {
            _toastText.value = R.string.empty_notif_deleted
            if (!isNewGeofication) {
                deleteGeofication()
            } else {
                _navigateToMain.value = true
            }
            return
        }
        if (currentTitle.isEmpty()) {
            _snackbarText.value = R.string.notif_empty
            return
        }

        if (isNewGeofication) {
            createNewGeofication(
                Geofication(
                    title = currentTitle,
                    description = currentDescription
                )
            )
        } else {
            val currentId = geoficationID
            updateCurrentGeofication(
                Geofication(
                    currentId,
                    currentTitle,
                    currentDescription,
                    currentIsCompleted,
                    currentCreatedTimestamp,
                    System.currentTimeMillis()
                )
            )
        }
        _navigateToMain.value = true
    }

    /**
     * Insert new geofication into database
     */
    private suspend fun insertGeofication(geofication: Geofication) {
        withContext(Dispatchers.IO) {
            database.insertGeofication(geofication)
        }
    }

    /**
     * Launch creating new grofication
     */
    private fun createNewGeofication(geofication: Geofication) {
        viewModelScope.launch {
            insertGeofication(geofication)
        }
    }

    /**
     * Update geofication in database
     */
    private suspend fun updateGeofication(geofication: Geofication) {
        withContext(Dispatchers.IO) {
            database.updateGeofication(geofication)
        }
    }

    /**
     * Launch updating current geofication
     */
    private fun updateCurrentGeofication(geofication: Geofication) {
        viewModelScope.launch {
            updateGeofication(geofication)
        }
    }

    /**
     * Call this immediately after navigating to MainFragment
     * It will clear the navigation request, so if the user rotates their phone it won't navigate
     * twice.
     */
    fun doneNavigating() {
        _navigateToMain.value = false
    }

    /**
     * Delete geofication from database
     */
    private suspend fun deleteGeoficationFromDb(geoficationId: Long) {
        withContext(Dispatchers.IO) {
            database.deleteGeoficationById(geoficationId)
        }
    }

    /**
     * Launch deleting current geofication
     */
    fun deleteGeofication() {
        if (!isNewGeofication) {
            viewModelScope.launch {
                deleteGeoficationFromDb(geoficationID)
            }
            _navigateToMain.value = true
        } else
            throw RuntimeException("deleteGeofication() was called for a new geofication")
    }

    /**
     * Creates a new alarm and notification
     */
    fun startNotificationCountdown() {
        _dateTimeAlarmOn.value = false
        _dateTimeAlarmOn.value?.let {
            if (!it) {
                _dateTimeAlarmOn.value = true

                val calendar = Calendar.getInstance().apply {
                    timeInMillis = dateInMillis.value!!
                    set(Calendar.HOUR_OF_DAY, hour.value!!)
                    set(Calendar.MINUTE, minute.value!!)
                    set(Calendar.SECOND, 0)
                }

                //TODO delete this
//                val dateFormatter = DateFormat.getDateTimeInstance()
//                Log.i("CALENDAR", dateFormatter.format(calendar.time))

                val notificationManager = ContextCompat.getSystemService(
                    app,
                    NotificationManager::class.java
                ) as NotificationManager

                notificationManager.cancelNotifications()
                alarmManager.cancel(notifyPendingIntentAlarm)

                AlarmManagerCompat.setExactAndAllowWhileIdle(
                    alarmManager,
                    AlarmManager.RTC,
                    calendar.timeInMillis,
                    notifyPendingIntentAlarm
                )
            }
        }
        _dateTimeAlarmOn.value = false  // TODO check this line
    }
}