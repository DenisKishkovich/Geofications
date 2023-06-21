package com.example.geofications.ui.details

import androidx.lifecycle.ViewModel
import com.example.geofications.data.GeoficationDao

class GeoficationDetailsViewModel(
    private val database: GeoficationDao,
    private val geoficationID: Long
) : ViewModel() {
}