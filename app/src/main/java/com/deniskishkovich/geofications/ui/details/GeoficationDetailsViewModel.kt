package com.deniskishkovich.geofications.ui.details

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
import android.widget.Toast
import androidx.core.app.ActivityCompat
import androidx.core.app.AlarmManagerCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.deniskishkovich.geofications.AlarmReceiver
import com.deniskishkovich.geofications.GeofenceBroadcastReceiver
import com.deniskishkovich.geofications.R
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
import java.util.Calendar
import java.util.concurrent.TimeUnit

class GeoficationDetailsViewModel(
    private val database: GeoficationDao,
    private var geoficationID: Long,
    private val app: Application
) : AndroidViewModel(app) {

    private val INTENT_ACTION_DATE_TIME = "com.deniskishkovich.action.datetime"

    private val INTENT_ACTION_LOCATION = "com.deniskishkovich.action.geofence"

    private val alarmManager = app.getSystemService(Context.ALARM_SERVICE) as AlarmManager

    private val geofencingClient = LocationServices.getGeofencingClient(app)

    var isNewGeofication: Boolean = false

    private var currentCreatedTimestamp: Long = 0L

    private val notifyAlarmIntent = Intent(app, AlarmReceiver::class.java)

    private val notifyLocationIntent = Intent(app, GeofenceBroadcastReceiver::class.java)

    // Two-way data binding, exposing MutableLiveData
    val title = MutableLiveData<String>()

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
     * "Inner" old title of geofication (when loaded)
     */
    private val _oldTitle = MutableLiveData<String>()

    /**
     *" Outer" old title of geofication (when loaded)
     */
    val oldTitle: LiveData<String>
        get() = _oldTitle

    /**
     * "Inner" old description of geofication (when loaded)
     */
    private val _oldDescription = MutableLiveData<String>()

    /**
     *" Outer" old description of geofication (when loaded)
     */
    val oldDescription: LiveData<String>
        get() = _oldDescription

    /**
     * "Inner" old isCompleted of geofication (when loaded)
     */
    private val _oldIsCompleted = MutableLiveData<Boolean>()

    /**
     *" Outer" old isCompleted of geofication (when loaded)
     */
    val oldIsCompleted: LiveData<Boolean>
        get() = _oldIsCompleted

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

    //Data for time selection dialog
    val hourForAlarm = MutableLiveData<Int?>()
    val minuteForAlarm = MutableLiveData<Int?>()

    //Data fot calendar selection
    val dateInMillisForAlarm = MutableLiveData<Long?>()

    private val _dateTimeInMillisForAlarm = MutableLiveData<Long?>()
    val dateTimeInMillisForAlarm: LiveData<Long?>
        get() = _dateTimeInMillisForAlarm



    // LatLng where to notify
    private val _latLngWhereNotify = MutableLiveData<LatLng?>()
    val latLngWhereNotify: LiveData<LatLng?>
        get() = _latLngWhereNotify

    // Location name
    private val _locationString = MutableLiveData<String?>()
    val locationString: LiveData<String?>
        get() = _locationString

    private val _isLocationNotificationOn = MutableLiveData<Boolean>()
    val isLocationNotificationOn: LiveData<Boolean>
        get() = _isLocationNotificationOn

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
     * Get geof. from database by id
     */
    private suspend fun getGeofication(geoficationId: Long): Geofication? =
        withContext(Dispatchers.IO) {
            return@withContext database.getGeoficationById(geoficationId)
        }

    /**
     * Get geof's params by id
     */
    private fun loadGeoficationParams(geoficationID: Long) {
        viewModelScope.launch {
            val geofication = getGeofication(geoficationID)

            if (geofication != null) {
                _oldTitle.value = geofication.title
                _oldDescription.value = geofication.description
                _oldIsCompleted.value = geofication.isCompleted

                title.value = geofication.title
                description.value = geofication.description
                isCompleted.value = geofication.isCompleted
                currentCreatedTimestamp = geofication.createdTimestamp
                _editedTimestamp.value = geofication.editedTimestamp
                _dateTimeInMillisForAlarm.value = geofication.timestampToNotify
                _dateTimeAlarmOn.value = geofication.isTimeNotificationSet

                if (geofication.latitude != null && geofication.longitude != null) {
                    _latLngWhereNotify.value = LatLng(geofication.latitude!!, geofication.longitude!!)
                } else _latLngWhereNotify.value = null
                _locationString.value = geofication.locationString
                _isLocationNotificationOn.value = geofication.isLocationNotificationSet

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
        val currentLocationLatitude = _latLngWhereNotify.value?.latitude
        val currentLocationLongitude = _latLngWhereNotify.value?.longitude
        val currentLocationString = _locationString.value
        val currentIsLocationNotificationOn = _isLocationNotificationOn.value ?: false

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
                    isTimeNotificationSet = currentDateTimeAlarmOn,
                    latitude = currentLocationLatitude,
                    longitude = currentLocationLongitude,
                    locationString = currentLocationString,
                    isLocationNotificationSet = currentIsLocationNotificationOn
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
                    currentDateTimeAlarmOn,
                    currentLocationLatitude,
                    currentLocationLongitude,
                    currentLocationString,
                    currentIsLocationNotificationOn
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
            if (_isLocationNotificationOn.value == true) {
                createGeofence()
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
            if (_isLocationNotificationOn.value == true) {
                cancelLocationNotificationAndGeofence()
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
                    _dateTimeAlarmOn.value ?: true,
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

        hourForAlarm.value = null
        minuteForAlarm.value = null
        dateInMillisForAlarm.value = null

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

    /**
     * Cancels location notification and geofence
     */
    fun cancelLocationNotificationAndGeofence() {
        _isLocationNotificationOn.value = false
        _latLngWhereNotify.value = null
        _locationString.value = null

        if (!isNewGeofication) {
            val notificationManager = ContextCompat.getSystemService(
                app,
                NotificationManager::class.java
            ) as NotificationManager
            notificationManager.cancelNotification(geoficationID.toInt())

            removeGeofence()

            viewModelScope.launch {
                updateLocationNotificationInDb(geoficationID, _latLngWhereNotify.value,
                    _locationString.value, _isLocationNotificationOn.value ?: false)
            }
        }
    }

    /**
     * Updates location notification
     */
    fun updateLocationNotification(latLng: LatLng, address: String) {
        _isLocationNotificationOn.value = true
        _latLngWhereNotify.value = latLng
        _locationString.value = address

        if (!isNewGeofication) {
            viewModelScope.launch {
                updateLocationNotificationInDb(geoficationID, latLng, address, _isLocationNotificationOn.value ?: true)
            }

            createGeofence()
        }
    }

    /**
     * Updates location notification in database
     */
    private suspend fun updateLocationNotificationInDb(id: Long, latLng: LatLng?, address: String?, isSet: Boolean) {
        withContext(Dispatchers.IO) {
            database.updateLocationNotificationStatus(id, isSet, latLng?.latitude, latLng?.longitude, address)
        }
    }

    /**
     * Creates pending intend for geofencing
     */
    private fun createPendingIntentForGeofence(): PendingIntent {
        notifyLocationIntent.apply {
            action = INTENT_ACTION_LOCATION
            putExtra("id", geoficationID)
            putExtra("title", title.value)
            putExtra("description", description.value)
        }
        return PendingIntent.getBroadcast(
            getApplication(),
            geoficationID.toInt(),
            notifyLocationIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_MUTABLE
        )
    }

    /**
     * Creates geofence for geofication
     */
    private fun createGeofence() {
        // Check permissions
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            if (ActivityCompat.checkSelfPermission(
                    app,
                    Manifest.permission.ACCESS_FINE_LOCATION
                ) != PackageManager.PERMISSION_GRANTED ||
                ActivityCompat.checkSelfPermission(
                    app,
                    Manifest.permission.ACCESS_BACKGROUND_LOCATION
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                return
            }
        } else {
            if (ActivityCompat.checkSelfPermission(
                    app,
                    Manifest.permission.ACCESS_FINE_LOCATION
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                return
            }
        }

        // Return if location is not selected or null
        if (!_isLocationNotificationOn.value!! || _latLngWhereNotify.value == null || _locationString.value == null) {
            return
        }

        val geofence = Geofence.Builder()
            // Set the request ID of the geofence. This is a string to identify this
            // geofence.
            .setRequestId(geoficationID.toString())
            // Set the circular region of this geofence.
            .setCircularRegion(
                _latLngWhereNotify.value!!.latitude,
                _latLngWhereNotify.value!!.longitude,
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
        removeGeofence()

        // Create new geofence
        geofencingClient.addGeofences(geofencingRequest, createPendingIntentForGeofence()).run {
            addOnFailureListener {
                cancelLocationNotificationAndGeofence()
                Toast.makeText(app,
                    app.getString(R.string.failed_to_set_a_reminder_for_location), Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun removeGeofence() {
        geofencingClient.removeGeofences(createPendingIntentForGeofence()).run {
            addOnSuccessListener {
                Log.i("GeoficationDetailsViewModel", "Geofence is removed")
            }
            addOnFailureListener {
                Log.e("GeoficationDetailsViewModel", "Failed to remove the geofence")
            }
        }
    }
}