package com.example.geofications.ui.details

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.view.MenuHost
import androidx.core.view.MenuProvider
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.get
import androidx.navigation.fragment.findNavController
import com.example.geofications.R
import com.example.geofications.data.GeoficationDao
import com.example.geofications.data.GeoficationDatabase
import com.example.geofications.databinding.FragmentGeoficationDetailsBinding

class GeoficationDetailsFragment() : Fragment() {

    private lateinit var geoficationDetailsViewModel: GeoficationDetailsViewModel

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
        geoficationDetailsViewModel = ViewModelProvider(this, viewModelFactory).get(GeoficationDetailsViewModel::class.java)

        binding.viewModel = geoficationDetailsViewModel
        binding.lifecycleOwner = this

        // Create the menu
        if (argGeoficationID != -1L) {
            createMenu()
        }

        // Add an Observer on the state variable for Navigating.
        geoficationDetailsViewModel.navigateToMain.observe(viewLifecycleOwner, Observer {
            if (it == true) {
                this.findNavController().navigateUp()
                geoficationDetailsViewModel.doneNavigating()
            }
        })

        return binding.root
    }

    /**
     * Create the menu
     */
    private fun createMenu() {
        // Init the menu
        // The usage of an interface lets you inject your own implementation
        val menuHost: MenuHost = requireActivity()

        // Add menu items without using the Fragment Menu APIs
        // Note how we can tie the MenuProvider to the viewLifecycleOwner
        // and an optional Lifecycle.State (here, RESUMED) to indicate when
        // the menu should be visible
        menuHost.addMenuProvider(object: MenuProvider {
            override fun onCreateMenu(menu: Menu, menuInflater: MenuInflater) {
                menuInflater.inflate(R.menu.details_fragment_menu, menu)
            }

            override fun onMenuItemSelected(menuItem: MenuItem): Boolean {
                return when (menuItem.itemId) {
                    R.id.delete_menu_item -> {
                        geoficationDetailsViewModel.deleteGeofication()
                        Toast.makeText(context, "Deleted", Toast.LENGTH_SHORT).show()
                        true
                    }
                    else -> false
                }
            }
        }, viewLifecycleOwner, Lifecycle.State.RESUMED)
    }
}