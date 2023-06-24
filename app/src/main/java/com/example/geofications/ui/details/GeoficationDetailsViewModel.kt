package com.example.geofications.ui.details

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
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

    init {
        if (geoficationID == -1L) {
            isNewGeofication = true
        }

        if (!isNewGeofication) {
            loadGeoficationParams(geoficationID)
        } else {
            title.value = "New title"
            description.value = "New description"
        }
    }

    /**
     * Method to get geof. from database by id
     */
    private suspend fun getGeofication(geoficationId: Long): Geofication? = withContext(Dispatchers.IO) {
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
            } else {
                throw Exception("Geofication not found")
            }
        }
    }

    /**
     * Save geofication. It starts when save button is clicked
     */
    fun saveGeofication() {
        val currentTitle = title.value!!
        val currentDescription = description.value!!

        if (isNewGeofication) {
            createNewGeofication(Geofication(title = currentTitle, description = currentDescription))
        } else {
            val currentId = geoficationID
            updateCurrentGeofication(Geofication(currentId, currentTitle, currentDescription))
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
        viewModelScope.launch{
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
}