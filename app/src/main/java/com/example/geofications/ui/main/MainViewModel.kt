package com.example.geofications.ui.main

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.geofications.data.Geofication
import com.example.geofications.data.GeoficationDao
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class MainViewModel(val database: GeoficationDao, application: Application) :
    AndroidViewModel(application) {

    val geoficationList = database.getAllGeofications()

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

    fun insertTest() {
        viewModelScope.launch {
            val newGeofication = Geofication(title = "tesst", description = "testDescr")

            insert(newGeofication)
        }
    }

    fun onClear() {
        viewModelScope.launch {
            clearAll()
        }
    }
}