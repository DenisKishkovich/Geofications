package com.deniskishkovich.geofications.ui.maps

import android.app.Application
import android.location.Address
import android.location.Geocoder
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.google.android.gms.maps.model.LatLng
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.Locale

class MapsViewModel(app: Application): AndroidViewModel(app) {

    private val geocoder = Geocoder(getApplication(), Locale.getDefault())

    /**
     * Inner variable for selected location coordinates (where user places marker)
     */
    private val _selectedLocationLatLng = MutableLiveData<LatLng?>()

    /**
     * Outer variable for selected location coordinates (where user places marker)
     */
    val selectedLocationLatLng: LiveData<LatLng?>
        get() = _selectedLocationLatLng

    /**
     * Inner variable for selected location address string (where user places marker)
     */
    private val _selectedLocationAddress = MutableLiveData<String?>()

    /**
     * Outer variable for selected location address string (where user places marker)
     */
    val selectedLocationAddress: LiveData<String?>
        get() = _selectedLocationAddress

    /**
     * Method for setting selected location LatLng
     */
    fun setSelectedLocationLatLng(latLng: LatLng?) {
        _selectedLocationLatLng.value = latLng
    }

    /**
     * Method for setting selected location address string (api 33)
     */
    @RequiresApi(Build.VERSION_CODES.TIRAMISU)
    private fun setSelectedLocationAddress(address: String?) {
        viewModelScope.launch {
            _selectedLocationAddress.postValue(address)
        }
    }

    /**
     * Gets address from geocoder
     */
    fun getAddress() {
        if (_selectedLocationLatLng.value == null) {
            return
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            geocoder.getFromLocation(
                _selectedLocationLatLng.value!!.latitude, _selectedLocationLatLng.value!!.longitude, 1)
            {
                setSelectedLocationAddress(generateAddressString(it.firstOrNull(), _selectedLocationLatLng.value!!))
            }
        } else {
            viewModelScope.launch {
                val address = withContext(Dispatchers.IO) {
                    try {
                        @Suppress("DEPRECATION")
                        geocoder.getFromLocation(
                            _selectedLocationLatLng.value!!.latitude, _selectedLocationLatLng.value!!.longitude, 1
                        )?.firstOrNull()
                    } catch(e: Exception) {
                        null
                    }
                }
                _selectedLocationAddress.value = generateAddressString(address,
                    _selectedLocationLatLng.value!!
                )
            }
        }
    }

    /**
     * Generates address string
     */
    private fun generateAddressString(address: Address?, latLng: LatLng): String {
        return if (address != null) {
            address.getAddressLine(0)
        } else {
            "${latLng.latitude}; ${latLng.longitude}"
        }
    }

}