package com.example.geofications.ui.details

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.example.geofications.data.GeoficationDao

class GeoficationDetailsViewModelFactory(
    private val dataSource: GeoficationDao,
    private val geoficationID: Long
) : ViewModelProvider.Factory {
    @Suppress("unchecked_cast")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(GeoficationDetailsViewModel::class.java)) {
            return GeoficationDetailsViewModel(dataSource, geoficationID) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}