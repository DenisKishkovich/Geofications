package com.example.geofications.ui.details

import android.util.Log
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

    val title = MutableLiveData<String?>()
    val description = MutableLiveData<String>()

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
}