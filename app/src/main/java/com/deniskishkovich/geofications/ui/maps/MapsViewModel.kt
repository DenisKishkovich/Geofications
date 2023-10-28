package com.deniskishkovich.geofications.ui.maps

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.google.android.gms.maps.model.LatLng

class MapsViewModel: ViewModel() {
    private val _selectedLocationLatLng = MutableLiveData<LatLng?>()

    val selectedLocationLatLng: LiveData<LatLng?>
        get() = _selectedLocationLatLng

    fun setSelectedLocationLatLng(latLng: LatLng?) {
        _selectedLocationLatLng.value = latLng
    }
}