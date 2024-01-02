package com.deniskishkovich.geofications.ui.details


import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.InputMethodManager
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.activity.addCallback
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.RequiresApi
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentTransaction
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.findNavController
import com.deniskishkovich.geofications.R
import com.deniskishkovich.geofications.data.GeoficationDatabase
import com.deniskishkovich.geofications.databinding.FragmentGeoficationDetailsBinding
import com.deniskishkovich.geofications.ui.maps.MapsFragment
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.snackbar.Snackbar

class GeoficationDetailsFragment() : Fragment() {

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

        setToolbar(args.appbarTitle)

        // Set chips behavior
        binding.datetimeChip.apply {
            setOnClickListener {
                showTimeSelectionDialog()
            }
            setOnCloseIconClickListener {
                geoficationDetailsViewModel.cancelDateTimeAlarm()
            }
        }
        binding.locationChip.apply {
            setOnClickListener {
                showMapsDialog()
            }
            setOnCloseIconClickListener {
                geoficationDetailsViewModel.cancelLocationNotificationAndGeofence()
            }
        }

        // Add an Observer on the state variable for Navigating.
        geoficationDetailsViewModel.navigateToMain.observe(viewLifecycleOwner) {
            if (it == true) {
                geoficationDetailsViewModel.doneNavigating()
                this.findNavController().navigateUp()
            }
        }

        // Add an Observer on the state variable for snackbars.
        geoficationDetailsViewModel.snackbarText.observe(viewLifecycleOwner) {
            view?.let { gottenView ->
                val snackbar = Snackbar.make(gottenView, getText(it), Snackbar.LENGTH_SHORT)
                snackbar.anchorView = binding.DetailsFab
                snackbar.show()
            }
        }

        // Add an Observer on the state variable for toasts.
        geoficationDetailsViewModel.toastText.observe(viewLifecycleOwner) {
            Toast.makeText(context, it, Toast.LENGTH_SHORT).show()
        }

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
     * Set toolbar appearance and behavior
     */
    private fun setToolbar(appBarTitle: String) {
        val toolbar = binding.detailsFragmentToolbar
        toolbar.title = appBarTitle
        toolbar.setNavigationOnClickListener {
            requireActivity().onBackPressedDispatcher.onBackPressed()
        }
        toolbar.setOnMenuItemClickListener {
            when (it.itemId) {
                R.id.delete_menu_item -> {
                    if (argGeoficationID == -1L) {
                        return@setOnMenuItemClickListener false
                    }

                    geoficationDetailsViewModel.deleteGeofication()
                    Toast.makeText(context, R.string.notification_deleted, Toast.LENGTH_SHORT)
                        .show()
                    true
                }

                R.id.create_notification_menu_item -> {
                    showNotifyBottomSheetDialog()
                    true
                }

                else -> false
            }
        }

        // Don't show delete button if new geofication
        if (argGeoficationID == -1L) {
            toolbar.menu.findItem(R.id.delete_menu_item).isVisible = false
        }
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
     fun showTimeSelectionDialog() {
        val timeSelectionDialogFragment = TimeSelectionDialogFragment()
        timeSelectionDialogFragment.show(childFragmentManager, "TimeSelectionDialog")

    }

    /**
     * Dialog if geofication is edited and not saved
     */
    private fun showExitWithoutSaveDialog(onBackPressedCallback: OnBackPressedCallback) {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle(getString(R.string.dialog_exit_without_save_request))
            .setNegativeButton(getString(R.string.dialog_cancel_button)) { dialog, _ ->
                dialog.cancel()
            }
            .setPositiveButton(getString(R.string.dialog_exit_button)) { dialog, _ ->
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
            .setNegativeButton(getString(R.string.dialog_button_skip)) { dialog, _ ->
                dialog.cancel()
            }
            .setPositiveButton(getString(R.string.dialog_button_i_understand)) { dialog, _ ->
                requestNotificationsPermissionLauncher.launch(android.Manifest.permission.POST_NOTIFICATIONS)
                dialog.cancel()
            }
            .show()
    }

    /**
     * Dialog for selecting the type of reminder (on time or on location)
     */
    private fun showNotifyBottomSheetDialog() {

        // Clear focus from edit text, for not making keyboard appear again
        binding.title.clearFocus()

        val notifyBottomSheetDialogFragment = NotifyBottomSheetDialogFragment()
        notifyBottomSheetDialogFragment.show(childFragmentManager, "NotifyBottomSheetDialog")
    }

    /**
     * Dialog for selecting a location for the reminder
     */
    fun showMapsDialog() {
        val mapsDialogFragment = MapsFragment()

        val transaction = childFragmentManager.beginTransaction()
        transaction.setTransition(FragmentTransaction.TRANSIT_FRAGMENT_OPEN)
        transaction
            .add(R.id.details_maps_dialog_container, mapsDialogFragment)
            .addToBackStack(null)
            .commit()
    }
}