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

    private var loadedGeofication = MutableLiveData<Geofication?>()

    val title = MutableLiveData<String?>()
    val description = MutableLiveData<String>()

    init {
        Log.i("ID CHECK", geoficationID.toString())
        if (geoficationID == -1L) {
            isNewGeofication = true
        }
        Log.i("ISNEW CHECK", isNewGeofication.toString())
        if (!isNewGeofication) {
            loadGeoficationById(geoficationID)

            if (loadedGeofication.value == null) {
                Log.i("LOAD CHECK", "loaded null")
            }

            Log.i("LOAD CHECK", "loaded")
            title.value = loadedGeofication.value?.title
            Log.i("TITLE CHECK", title.value ?: "null")
            description.value = loadedGeofication.value?.description
        }
    }

    private suspend fun getGeofication(geoficationId: Long): Geofication? {
        return database.getGeoficationById(geoficationId)
    }

    fun loadGeoficationById(geoficationID: Long) {
        viewModelScope.launch {
            loadedGeofication.value = getGeofication(geoficationID)
        }
    }
}