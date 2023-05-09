package com.example.geofications.ui.main

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import com.example.geofications.data.GeoficationDao

class MainViewModel(val database: GeoficationDao, application: Application) :
    AndroidViewModel(application) {

}