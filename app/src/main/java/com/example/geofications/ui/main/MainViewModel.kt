package com.example.geofications.ui.main

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.example.geofications.data.Geofication
import com.example.geofications.data.GeoficationDao
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class MainViewModel(val database: GeoficationDao, application: Application) :
    AndroidViewModel(application) {

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

    private suspend fun insert(geofication: Geofication) {
        withContext(Dispatchers.IO) {
            database.insertGeofication(geofication)
        }
    }

    private suspend fun clearAll() {
        withContext(Dispatchers.IO) {
            database.deleteAllGeofications()
        }
    }

    /**
     * Insert new geofication
     */
    fun insertNewGeodication() {
            viewModelScope.launch {
            val newGeofication = Geofication(title = "tesst", description = "testDescr")

            insert(newGeofication)
        }
    }

    /**
     * Clear geofication database
     */
    fun onClear() {
        viewModelScope.launch {
            clearAll()
        }
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
        insertNewGeodication()
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