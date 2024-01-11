package com.deniskishkovich.geofications.ui.main

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.deniskishkovich.geofications.data.Geofication
import com.deniskishkovich.geofications.data.GeoficationDao
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

    private suspend fun deleteCompletedFromDb() {
        withContext(Dispatchers.IO) {
            database.deleteCompletedGeofications()
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
     * Update completed
     */
    fun completeGeofication(geofication: Geofication, completed: Boolean) {
        viewModelScope.launch {
            updateIsCompletedInDb(geofication, completed)
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
     * Delete geofication on swiped
     */
    fun swipeDeleteGeofication(geoficationId: Long) {
        if (geoficationId < 1) {
            return
        }
        viewModelScope.launch {
            deleteGeoficationFromDb(geoficationId)
        }
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