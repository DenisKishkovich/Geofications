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
import com.example.geofications.cancelNotification
import com.example.geofications.data.Geofication
import com.example.geofications.data.GeoficationDao
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.Calendar

class GeoficationDetailsViewModel(
    private val database: GeoficationDao,
    private var geoficationID: Long,
    private val app: Application
) : AndroidViewModel(app) {

    private val INTENT_ACTION_DATE_TIME = "datetime"

    private val alarmManager = app.getSystemService(Context.ALARM_SERVICE) as AlarmManager

    var isNewGeofication: Boolean = false

    private var currentCreatedTimestamp: Long = 0L

    private val notifyAlarmIntent = Intent(app, AlarmReceiver::class.java)

    // Two-way data binding, exposing MutableLiveData
    val title = MutableLiveData<String?>()

    // Two-way data binding, exposing MutableLiveData
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


    //Data for time selection dialog
    val hourForAlarm = MutableLiveData<Int?>()
    val minuteForAlarm = MutableLiveData<Int?>()

    //Data fot calendar selection
    val dateInMillisForAlarm = MutableLiveData<Long?>()

    private val _dateTimeInMillisForAlarm = MutableLiveData<Long?>()
    val dateTimeInMillisForAlarm: LiveData<Long?>
        get() = _dateTimeInMillisForAlarm

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
                _dateTimeInMillisForAlarm.value = geofication.timestampToNotify
                _dateTimeAlarmOn.value = geofication.isTimeNotificationSet
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
        val currentTimestampToNotify = _dateTimeInMillisForAlarm.value
        val currentDateTimeAlarmOn = _dateTimeAlarmOn.value ?: false

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
                    description = currentDescription,
                    timestampToNotify = currentTimestampToNotify,
                    isTimeNotificationSet = currentDateTimeAlarmOn
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
                    System.currentTimeMillis(), // Edited timestamp
                    currentTimestampToNotify,
                    currentDateTimeAlarmOn
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
            geoficationID = database.insertGeofication(geofication)
            if (_dateTimeAlarmOn.value == true) {
                startNotificationCountdown()
            }
        }
    }

    /**
     * Launch creating new grofication
     */
    private fun createNewGeofication(geofication: Geofication) {
        if (_dateTimeAlarmOn.value == true) {
            updateDateTimeAlarm()
        }
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
            if (_dateTimeAlarmOn.value == true) {
                cancelDateTimeNotificationAndAlarm(createPendingIntentForDateTimeAlarm())
            }
            viewModelScope.launch {
                deleteGeoficationFromDb(geoficationID)
            }
            _navigateToMain.value = true
        } else
            throw RuntimeException("deleteGeofication() was called for a new geofication")
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
     * Sets _dateTimeInMillisForAlarm live data
     */
    private fun setDateTimeInMillisForAlarm() {
        if (dateInMillisForAlarm.value != null || hourForAlarm.value != null || minuteForAlarm.value != null) {
            val calendar = Calendar.getInstance().apply {
                timeInMillis = dateInMillisForAlarm.value!!
                set(Calendar.HOUR_OF_DAY, hourForAlarm.value!!)
                set(Calendar.MINUTE, minuteForAlarm.value!!)
                set(Calendar.SECOND, 0)
            }
            _dateTimeInMillisForAlarm.value = calendar.timeInMillis
        }
    }

    /**
     * Creates a new alarm and notification
     */
    private fun startNotificationCountdown() {
        if (_dateTimeAlarmOn.value == true) {

            val notifyPendingIntentAlarm = createPendingIntentForDateTimeAlarm()

            // delete current notification if exist
            cancelDateTimeNotificationAndAlarm(notifyPendingIntentAlarm)

            _dateTimeInMillisForAlarm.value?.let {
                AlarmManagerCompat.setExactAndAllowWhileIdle(
                    alarmManager,
                    AlarmManager.RTC_WAKEUP,
                    it,
                    notifyPendingIntentAlarm
                )
            }
        }
    }

    private fun createPendingIntentForDateTimeAlarm(): PendingIntent {
        notifyAlarmIntent.apply {
            action = INTENT_ACTION_DATE_TIME
            putExtra("id", geoficationID)
            putExtra("title", title.value)
            putExtra("description", description.value)
        }
        return PendingIntent.getBroadcast(
            getApplication(),
            geoficationID.toInt(), // REQUEST CODE for multiple pending intents from multiple geofications
            notifyAlarmIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    private fun cancelDateTimeNotificationAndAlarm(pendingIntent: PendingIntent) {
        val notificationManager = ContextCompat.getSystemService(
            app,
            NotificationManager::class.java
        ) as NotificationManager
        notificationManager.cancelNotification(geoficationID.toInt())
        alarmManager.cancel(pendingIntent)
    }

    /**
     * Update completed date and time notification status in database
     */
    private suspend fun updateDateTimeAlarmInDb(
        id: Long,
        isTimeNotificationSet: Boolean,
        alarmTimeInMillis: Long?
    ) {
        withContext(Dispatchers.IO) {
            database.updateDateTimeNotificationStatus(id, isTimeNotificationSet, alarmTimeInMillis)
        }
    }

    /**
     * Update date and time notification status
     */
    fun updateDateTimeAlarm() {
        _dateTimeAlarmOn.value = true
        setDateTimeInMillisForAlarm()
        if (_dateTimeInMillisForAlarm.value!! <= System.currentTimeMillis()) {
            _snackbarText.value = R.string.time_less_than_current
        }

        if (!isNewGeofication) {
            viewModelScope.launch {
                updateDateTimeAlarmInDb(
                    geoficationID,
                    _dateTimeAlarmOn.value ?: false,
                    _dateTimeInMillisForAlarm.value
                )
            }
            startNotificationCountdown()
        }
    }

    /**
     * Cancel date/time alarm and notification
     */
    fun cancelDateTimeAlarm() {
        _dateTimeAlarmOn.value = false
        _dateTimeInMillisForAlarm.value = null

        if (!isNewGeofication) {
            cancelDateTimeNotificationAndAlarm(createPendingIntentForDateTimeAlarm())

            viewModelScope.launch {
                updateDateTimeAlarmInDb(
                    geoficationID,
                    _dateTimeAlarmOn.value ?: false,
                    _dateTimeInMillisForAlarm.value
                )
            }
        }
    }
}