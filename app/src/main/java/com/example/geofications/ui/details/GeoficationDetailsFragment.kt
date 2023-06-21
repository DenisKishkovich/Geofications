package com.example.geofications.ui.details

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.get
import com.example.geofications.R
import com.example.geofications.data.GeoficationDao
import com.example.geofications.data.GeoficationDatabase
import com.example.geofications.databinding.FragmentGeoficationDetailsBinding

class GeoficationDetailsFragment() : Fragment() {

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val binding: FragmentGeoficationDetailsBinding = DataBindingUtil.inflate(
            inflater, R.layout.fragment_geofication_details, container, false)

        val application = requireNotNull(this.activity).application

        val dataSource = GeoficationDatabase.getInstance(application).geoficationDAO

        // Arguments from navigation
        val args = GeoficationDetailsFragmentArgs.fromBundle(requireArguments())

        // Geofication ID from args
        val argGeoficationID = args.geoficationID
        Toast.makeText(context, argGeoficationID.toString(), Toast.LENGTH_LONG).show()

        val viewModelFactory = GeoficationDetailsViewModelFactory(dataSource, argGeoficationID)
        val geoficationDetailsViewModel = ViewModelProvider(this, viewModelFactory).get(GeoficationDetailsViewModel::class.java)

        return binding.root
    }
}