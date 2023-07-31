package com.example.geofications.ui.details

import android.app.AlarmManager
import android.app.Application
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.CountDownTimer
import android.os.SystemClock
import androidx.core.app.AlarmManagerCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.example.geofications.AlarmReceiver
import com.example.geofications.R
import com.example.geofications.data.Geofication
import com.example.geofications.data.GeoficationDao
import com.example.geofications.sendNotification
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class GeoficationDetailsViewModel(
    private val database: GeoficationDao,
    private val geoficationID: Long,
    private val app: Application
) : AndroidViewModel(app) {

    private val REQUEST_CODE = 0
    private val TRIGGER_TIME = "TRIGGER_AT"

    private val notifyPendingIntentAlarm: PendingIntent
    private val alarmManager = app.getSystemService(Context.ALARM_SERVICE) as AlarmManager
    private val sharedPrefs =
        app.getSharedPreferences("com.example.geofications", Context.MODE_PRIVATE)
    private lateinit var timer: CountDownTimer

    private var isNewGeofication: Boolean = false

    // Two-way databinding, exposing MutableLiveData
    val title = MutableLiveData<String?>()

    // Two-way databinding, exposing MutableLiveData
    val description = MutableLiveData<String>()

    val isCompleted = MutableLiveData<Boolean>()

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

    private val _alarmOn = MutableLiveData<Boolean>()
    val isAlarmOn: LiveData<Boolean>
        get() = _alarmOn

    private val _elapsedTime = MutableLiveData<Long>()
    val elapsedTime: LiveData<Long>
        get() = _elapsedTime

    private val notifyAlarmIntent = Intent(app, AlarmReceiver::class.java)

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

        _alarmOn.value = PendingIntent.getBroadcast(
            getApplication(),
            REQUEST_CODE,
            notifyAlarmIntent,
            PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
        ) != null

        notifyPendingIntentAlarm = PendingIntent.getBroadcast(
            getApplication(),
            REQUEST_CODE,
            notifyAlarmIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        if (_alarmOn.value!!){
            createTimer()
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
                    currentIsCompleted
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
     * Creates a new alarm, notification and timer
     */
    fun startTimer() {
        _alarmOn.value?.let {
            if (!it) {
                _alarmOn.value = true

                val triggerTime = SystemClock.elapsedRealtime() + 10_000L

//                val notificationManager = ContextCompat.getSystemService(
//                    app,
//                    NotificationManager::class.java
//                ) as NotificationManager
//                notificationManager.sendNotification("Test notification body", app)

                AlarmManagerCompat.setExactAndAllowWhileIdle(
                    alarmManager,
                    AlarmManager.ELAPSED_REALTIME_WAKEUP,
                    triggerTime,
                    notifyPendingIntentAlarm
                )
                viewModelScope.launch { saveTime(triggerTime) }
            }
        }
        createTimer()
    }

    /**
     * Creates a new timer
     */
    private fun createTimer() {
        viewModelScope.launch {
            val triggerTime = loadTime()
            timer = object : CountDownTimer(triggerTime, 1_000L) {
                override fun onTick(millisUntilFinished: Long) {
                    _elapsedTime.value = triggerTime - SystemClock.elapsedRealtime()
                    if (_elapsedTime.value!! <= 0) {
                        resetTimer()
                    }
                }
                override fun onFinish() {
                    resetTimer()
                }
            }
            timer.start()
        }
    }

    private suspend fun saveTime(triggerTime: Long) {
        withContext(Dispatchers.IO) {
            sharedPrefs.edit().putLong(TRIGGER_TIME, triggerTime).apply()
        }
    }

    private suspend fun loadTime(): Long {
        return withContext(Dispatchers.IO) {
            sharedPrefs.getLong(TRIGGER_TIME, 0)
        }
    }

    /**
     * Resets the timer on screen and sets alarm value false
     */
    private fun resetTimer() {
        timer.cancel()
        _elapsedTime.value = 0
        _alarmOn.value = false
    }
}