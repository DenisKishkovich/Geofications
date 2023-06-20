package com.example.geofications.ui.details

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.databinding.DataBindingUtil
import com.example.geofications.R
import com.example.geofications.databinding.FragmentGeoficationDetailsBinding

class GeoficationDetailsFragment : Fragment() {
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val binding: FragmentGeoficationDetailsBinding = DataBindingUtil.inflate(
            inflater, R.layout.fragment_geofication_details, container, false)

        // arguments from navigation
        val args = GeoficationDetailsFragmentArgs.fromBundle(requireArguments())

        val argGeoficationID = args.geoficationID
        Toast.makeText(context, argGeoficationID.toString(), Toast.LENGTH_LONG).show()

        return binding.root
    }
}