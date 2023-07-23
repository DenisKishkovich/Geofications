package com.example.geofications.ui.details

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.geofications.R
import com.example.geofications.data.Geofication
import com.example.geofications.data.GeoficationDao
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class GeoficationDetailsViewModel(
    private val database: GeoficationDao,
    private val geoficationID: Long
) : ViewModel() {

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
            _snackbarText.value = R.string.empty_notif_deleted
            if (!isNewGeofication) {
                deleteGeofication()
            } else {
                _navigateToMain.value = true
            }
            return
        }
        if (Geofication(title = currentTitle, description = currentDescription).isEmpty) {
            _snackbarText.value = R.string.empty_notif_deleted
            if (!isNewGeofication) {
                deleteGeofication()
            } else {
                _navigateToMain.value = true
            }
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
            updateCurrentGeofication(Geofication(currentId, currentTitle, currentDescription, currentIsCompleted))
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
}