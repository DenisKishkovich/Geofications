package com.example.geofications.ui.details

import android.app.NotificationChannel
import android.app.NotificationManager
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.InputMethodManager
import android.widget.Toast
import androidx.core.view.MenuHost
import androidx.core.view.MenuProvider
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.Fragment
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.findNavController
import com.example.geofications.R
import com.example.geofications.data.GeoficationDatabase
import com.example.geofications.databinding.FragmentGeoficationDetailsBinding
import com.google.android.material.snackbar.Snackbar

class GeoficationDetailsFragment() : Fragment() {

    private lateinit var geoficationDetailsViewModel: GeoficationDetailsViewModel

    private lateinit var binding: FragmentGeoficationDetailsBinding

    private var argGeoficationID = -1L

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = DataBindingUtil.inflate(
            inflater, R.layout.fragment_geofication_details, container, false
        )

        val application = requireNotNull(this.activity).application

        val dataSource = GeoficationDatabase.getInstance(application).geoficationDAO

        // Arguments from navigation
        val args = GeoficationDetailsFragmentArgs.fromBundle(requireArguments())

        // Geofication ID from args
        argGeoficationID = args.geoficationID

        val viewModelFactory =
            GeoficationDetailsViewModelFactory(dataSource, argGeoficationID, application)
        geoficationDetailsViewModel =
            ViewModelProvider(this, viewModelFactory).get(GeoficationDetailsViewModel::class.java)

        binding.viewModel = geoficationDetailsViewModel
        binding.lifecycleOwner = this

        createMenu()

        //Hide checkbox if new geofication
        if (argGeoficationID == -1L) {
            binding.checkBoxInDetails.visibility = View.GONE
        }

        // Add an Observer on the state variable for Navigating.
        geoficationDetailsViewModel.navigateToMain.observe(viewLifecycleOwner, Observer {
            if (it == true) {
                this.findNavController().navigateUp()
                geoficationDetailsViewModel.doneNavigating()
            }
        })

        // Add an Observer on the state variable for snackbars.
        geoficationDetailsViewModel.snackbarText.observe(viewLifecycleOwner, Observer {
            view?.let { gottenView ->
                Snackbar.make(gottenView, getText(it), Snackbar.LENGTH_SHORT).show()
            }
        })

        // Add an Observer on the state variable for toasts.
        geoficationDetailsViewModel.toastText.observe(viewLifecycleOwner, Observer {
            Toast.makeText(context, it, Toast.LENGTH_SHORT).show()
        })

        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Show keyboard if new notification
        val titleEditText = binding.title
        if (argGeoficationID == -1L) {
            titleEditText.requestFocus()

            val imm = requireActivity().getSystemService(InputMethodManager::class.java)
            imm.showSoftInput(titleEditText, InputMethodManager.SHOW_IMPLICIT)
        }
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
        menuHost.addMenuProvider(object : MenuProvider {
            override fun onCreateMenu(menu: Menu, menuInflater: MenuInflater) {
                menuInflater.inflate(R.menu.details_fragment_menu, menu)

                // Don't show delete button if new geofication
                if (argGeoficationID == -1L) {
                    menu.findItem(R.id.delete_menu_item).isVisible = false
                }
            }

            override fun onMenuItemSelected(menuItem: MenuItem): Boolean {
                return when (menuItem.itemId) {
                    R.id.delete_menu_item -> {
                        if (argGeoficationID == -1L) {
                            return false
                        }

                        geoficationDetailsViewModel.deleteGeofication()
                        Toast.makeText(context, R.string.notification_deleted, Toast.LENGTH_SHORT)
                            .show()
                        true
                    }

                    R.id.create_notification_menu_item -> {
                        geoficationDetailsViewModel.startNotificationCountdown()
                        true
                    }

                    else -> false
                }
            }
        }, viewLifecycleOwner, Lifecycle.State.RESUMED)
    }

    private fun createChannel(channelId: String, channelName: String) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val notificationChannel = NotificationChannel(
                channelId,
                channelName,
                NotificationManager.IMPORTANCE_HIGH
            )
            notificationChannel.enableVibration(true)
            notificationChannel.description =
                "Notification on selected time"  //TODO correct description

            val notificationManager =
                requireActivity().getSystemService(NotificationManager::class.java)
            notificationManager.createNotificationChannel(notificationChannel)
        }
    }
}