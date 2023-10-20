package com.example.geofications.ui.details


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
import androidx.activity.OnBackPressedCallback
import androidx.activity.addCallback
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.RequiresApi
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
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.snackbar.Snackbar

class GeoficationDetailsFragment() : Fragment() {

    private lateinit var timeSelectionDialogFragment: TimeSelectionDialogFragment

    private lateinit var geoficationDetailsViewModel: GeoficationDetailsViewModel

    private lateinit var binding: FragmentGeoficationDetailsBinding

    // Variable for launching request of notifications permission
    private lateinit var requestNotificationsPermissionLauncher: ActivityResultLauncher<String>

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

        // Set date/time chip behavior
        binding.datetimeChip.apply {
            setOnClickListener {
                showTimeSelectionDialog()
            }
            setOnCloseIconClickListener {
                geoficationDetailsViewModel.cancelDateTimeAlarm()
            }
        }

        // Add an Observer on the state variable for Navigating.
        geoficationDetailsViewModel.navigateToMain.observe(viewLifecycleOwner, Observer {
            if (it == true) {
                geoficationDetailsViewModel.doneNavigating()
                this.findNavController().navigateUp()
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

        requestNotificationsPermissionLauncher =
            registerForActivityResult(ActivityResultContracts.RequestPermission()) {
            }

        // Request notifications permission
        if (Build.VERSION.SDK_INT >= 33) {
            requestNotificationsPermissionRationale()
        }

        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Show keyboard if new notification
        if (argGeoficationID == -1L) {
            val titleEditText = binding.title
            titleEditText.requestFocus()

            val imm = requireActivity().getSystemService(InputMethodManager::class.java)
            imm.showSoftInput(titleEditText, InputMethodManager.SHOW_IMPLICIT)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        /**
         * Call dialog if geofication is edited and not saved
         */
        requireActivity().onBackPressedDispatcher.addCallback {
            if ((geoficationDetailsViewModel.title.value != geoficationDetailsViewModel.oldTitle.value || geoficationDetailsViewModel.description.value != geoficationDetailsViewModel.oldDescription.value || geoficationDetailsViewModel.isCompleted.value != geoficationDetailsViewModel.oldIsCompleted.value) && argGeoficationID != -1L) {
                this.isEnabled = true
                showExitWithoutSaveDialog(this)
            } else {
                this.isEnabled = false
                findNavController().navigateUp()
            }
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
                        showTimeSelectionDialog()
                        true
                    }

                    else -> false
                }
            }
        }, viewLifecycleOwner, Lifecycle.State.RESUMED)
    }

    @RequiresApi(Build.VERSION_CODES.TIRAMISU)
    private fun requestNotificationsPermissionRationale() {
        if (shouldShowRequestPermissionRationale(android.Manifest.permission.POST_NOTIFICATIONS)) {
            showNotificationPermissionRationaleDialog()
        } else {
            requestNotificationsPermissionLauncher.launch(android.Manifest.permission.POST_NOTIFICATIONS)
        }
    }


    /**
     * Show dialog of time & date selection
     */
    private fun showTimeSelectionDialog() {
        timeSelectionDialogFragment = TimeSelectionDialogFragment()
        timeSelectionDialogFragment.show(childFragmentManager, "game")

    }

    /**
     * Dialog if geofication is edited and not saved
     */
    private fun showExitWithoutSaveDialog(onBackPressedCallback: OnBackPressedCallback) {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle(getString(R.string.dialog_exit_without_save_request))
            .setNegativeButton(getString(R.string.dialog_cancel_button)) { dialog, which ->
                dialog.cancel()
            }
            .setPositiveButton(getString(R.string.dialog_exit_button)) { dialog, which ->
                if (onBackPressedCallback.isEnabled) {
                    onBackPressedCallback.isEnabled = false
                    findNavController().navigateUp()
                    dialog.cancel()
                }
                dialog.cancel()
            }
            .show()
    }

    /**
     * Dialog for requesting notification permission rationale
     */
    @RequiresApi(Build.VERSION_CODES.TIRAMISU)
    private fun showNotificationPermissionRationaleDialog() {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle(getString(R.string.dialog_title_get_notified))
            .setMessage(getString(R.string.dialog_message_notifications_permission))
            .setIcon(android.R.drawable.ic_popup_reminder)
            .setNegativeButton(getString(R.string.dialog_button_skip)) { dialog, which ->
                dialog.cancel()
            }
            .setPositiveButton(getString(R.string.dialog_button_i_understand)) { dialog, which ->
                requestNotificationsPermissionLauncher.launch(android.Manifest.permission.POST_NOTIFICATIONS)
                dialog.cancel()
            }
            .show()
    }
}