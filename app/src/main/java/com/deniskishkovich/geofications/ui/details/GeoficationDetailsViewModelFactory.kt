package com.deniskishkovich.geofications.ui.details

import android.app.Application
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.deniskishkovich.geofications.data.GeoficationDao

class GeoficationDetailsViewModelFactory(
    private val dataSource: GeoficationDao,
    private val geoficationID: Long,
    private val app: Application
) : ViewModelProvider.Factory {
    @Suppress("unchecked_cast")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(GeoficationDetailsViewModel::class.java)) {
            return GeoficationDetailsViewModel(dataSource, geoficationID, app) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}