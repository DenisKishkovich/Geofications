package com.example.geofications.ui.details

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.get
import androidx.navigation.fragment.findNavController
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

        val viewModelFactory = GeoficationDetailsViewModelFactory(dataSource, argGeoficationID)
        val geoficationDetailsViewModel = ViewModelProvider(this, viewModelFactory).get(GeoficationDetailsViewModel::class.java)

        binding.viewModel = geoficationDetailsViewModel
        binding.lifecycleOwner = this

        // Add an Observer on the state variable for Navigating.
        geoficationDetailsViewModel.navigateToMain.observe(viewLifecycleOwner, Observer {
            if (it == true) {
                this.findNavController().navigateUp()
                geoficationDetailsViewModel.doneNavigating()
            }
        })

        return binding.root
    }
}