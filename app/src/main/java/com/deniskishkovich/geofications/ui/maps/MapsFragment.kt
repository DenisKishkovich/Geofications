package com.deniskishkovich.geofications.ui.maps

import android.Manifest
import android.annotation.SuppressLint
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Location
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import android.widget.LinearLayout
import androidx.activity.addCallback
import androidx.core.app.ActivityCompat
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.LinearLayoutManager
import com.deniskishkovich.geofications.BuildConfig
import com.deniskishkovich.geofications.R
import com.deniskishkovich.geofications.databinding.FragmentMapsBinding
import com.deniskishkovich.geofications.ui.details.GeoficationDetailsViewModel
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MarkerOptions
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.divider.MaterialDividerItemDecoration
import com.google.android.material.search.SearchView
import com.google.android.material.snackbar.Snackbar
import com.google.android.material.transition.MaterialSharedAxis

class MapsFragment : DialogFragment(), OnMapReadyCallback {

    private var _binding: FragmentMapsBinding? = null

    // This property is only valid between onCreateView and
    // onDestroyView.
    private val binding get() = _binding!!

    private val mapsViewModel: MapsViewModel by viewModels()

    private val sharedViewModel: GeoficationDetailsViewModel by viewModels(ownerProducer = { requireParentFragment() })

    private lateinit var bottomSheetBehavior: BottomSheetBehavior<LinearLayout>

    private lateinit var map: GoogleMap

    private lateinit var fusedLocationProviderClient: FusedLocationProviderClient

    private var lastKnownLocation: Location? = null

    // A default location and default zoom to use when location permission is
    // not granted.
    private val defaultLocation = LatLng(59.939874, 30.314526)

    private var foregroundAndBackgroundLocationPermissionGranted = false
    private var foregroundLocationPermissionGranted = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Set animations
        enterTransition = MaterialSharedAxis(MaterialSharedAxis.Z, /* forward= */ true).apply {
            duration = 750
            secondaryAnimatorProvider = null
        }

        returnTransition = MaterialSharedAxis(MaterialSharedAxis.Z, /* forward= */ false)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentMapsBinding.inflate(inflater, container, false)

        binding.viewModel = mapsViewModel

        // set Toolbar
        val toolbar = binding.mapsToolbar
        toolbar.setNavigationOnClickListener { requireActivity().onBackPressedDispatcher.onBackPressed() }
        toolbar.setOnMenuItemClickListener {
            when (it.itemId) {
                R.id.save_menu_item -> {
                    getLocationPermission()

                    if (mapsViewModel.selectedLocationLatLng.value == null) {
                        Snackbar.make(binding.root,
                            getString(R.string.select_location_prompt), Snackbar.LENGTH_SHORT).show()
                        return@setOnMenuItemClickListener false
                    }

                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                        if (foregroundLocationPermissionGranted && foregroundAndBackgroundLocationPermissionGranted) {
                            saveInGeofication()
                            requireActivity().onBackPressedDispatcher.onBackPressed()
                            true
                        } else {
                            false
                        }
                    } else {
                        if (foregroundLocationPermissionGranted) {
                            saveInGeofication()
                            requireActivity().onBackPressedDispatcher.onBackPressed()
                            true
                        } else {
                            makeSnackbarForPermissions()
                            false
                        }
                    }

                }

                else -> false
            }
        }
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Client to find device's location
        fusedLocationProviderClient =
            LocationServices.getFusedLocationProviderClient(requireActivity())

        val mapFragment = childFragmentManager.findFragmentById(R.id.map) as SupportMapFragment?
        mapFragment?.getMapAsync(this)

        // Hide bottom sheet when view created
        bottomSheetBehavior = BottomSheetBehavior.from(binding.addressBottomSheet)
        if (mapsViewModel.selectedLocationAddressString.value == null) {
            bottomSheetBehavior.state = BottomSheetBehavior.STATE_HIDDEN
        }
    }

    /**
     * Manipulates the map once available.
     * This callback is triggered when the map is ready to be used.
     * This is where we can add markers or lines, add listeners or move the camera.
     * In this case, we just add a marker near Sydney, Australia.
     * If Google Play services is not installed on the device, the user will be prompted to
     * install it inside the SupportMapFragment. This method will only be triggered once the
     * user has installed Google Play services and returned to the app.
     */
    override fun onMapReady(googleMap: GoogleMap) {
        map = googleMap

        // Set padding for google map's default buttons
        map.setPadding(0, 300, 0, 0)

        // Prompt the user for permission.
        getLocationPermission()

        // If location is in database, select it
        sharedViewModel.latLngWhereNotify.value?.let {
            mapsViewModel.setSelectedLocationLatLng(it)
        }

        // Get the current location of the device and set the position of the map.
        getDeviceLocation()

        // Set map click listeners
        setMapClick()

        // Init search view
        initSearch()

        // Create marker when selectedLocationLatLng in view model updates
        mapsViewModel.selectedLocationLatLng.observe(viewLifecycleOwner) {
            it?.let {
                map.clear()
                map.addMarker(MarkerOptions()
                    .position(it))

                bottomSheetBehavior.state = BottomSheetBehavior.STATE_HIDDEN

                mapsViewModel.getAddress()
            }
        }

        // Show bottom sheet with address
        mapsViewModel.selectedLocationAddressString.observe(viewLifecycleOwner) {
            binding.addressTextBottomSheet.text = it
            bottomSheetBehavior.state = BottomSheetBehavior.STATE_EXPANDED
        }


    }

    /**
     * Get the best and most recent location of the device, which may be null in rare
     * cases when a location is not available.
     */
    @SuppressLint("MissingPermission")
    private fun getDeviceLocation() {
        try {
            if (foregroundLocationPermissionGranted) {
                map.isMyLocationEnabled = true

                if (mapsViewModel.selectedLocationLatLng.value == null) {
                    val locationResult = fusedLocationProviderClient.lastLocation
                    locationResult.addOnCompleteListener(requireActivity()) { task ->
                        if (task.isSuccessful) {
                            // Set the map's camera position to the current location of the device.
                            lastKnownLocation = task.result

                            if (lastKnownLocation != null) {
                                map.moveCamera(
                                    CameraUpdateFactory.newLatLngZoom(
                                        LatLng(
                                            lastKnownLocation!!.latitude,
                                            lastKnownLocation!!.longitude
                                        ),
                                        DEFAULT_ZOOM.toFloat()
                                    )
                                )
                            }
                        } else {
                            map.moveCamera(
                                CameraUpdateFactory.newLatLngZoom(
                                    defaultLocation,
                                    DEFAULT_ZOOM.toFloat()
                                )
                            )
                        }
                    }
                } else {
                    map.moveCamera(
                        CameraUpdateFactory.newLatLngZoom(
                            mapsViewModel.selectedLocationLatLng.value!!,
                            DEFAULT_ZOOM.toFloat()
                        )
                    )
                }
            }
        } catch (e: SecurityException) {
            Log.e("Exception: %s", e.message, e)
        }
    }

    private fun setMapClick() {
        map.setOnMapClickListener { latLng ->
            mapsViewModel.setSelectedLocationLatLng(latLng)
        }
        map.setOnMapLongClickListener { latLng ->
            mapsViewModel.setSelectedLocationLatLng(latLng)
        }
    }


    /**
     * Prompt user for location permissions
     */
    private fun getLocationPermission() {
        val permissionArray = arrayOf(
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION
        )

        if (ActivityCompat.checkSelfPermission(
                requireContext(),
                Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(
                requireContext(),
                Manifest.permission.ACCESS_COARSE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            foregroundLocationPermissionGranted = true
            checkBackgroundPermissionAndShowDialog()

        } else {
            makeSnackbarForPermissions()

            @Suppress("DEPRECATION")
            requestPermissions(
                permissionArray,
                REQUEST_FOREGROUND_PERMISSIONS_REQUEST_CODE
            )
        }
    }

    @Suppress("DEPRECATION")
    @Deprecated("Deprecated in Java")
    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        when (requestCode) {
            // If request is cancelled, the result arrays are empty.
            REQUEST_FOREGROUND_PERMISSIONS_REQUEST_CODE -> {
                if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    foregroundLocationPermissionGranted = true
                    getDeviceLocation()
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                        if (ActivityCompat.checkSelfPermission(
                                requireContext(),
                                Manifest.permission.ACCESS_BACKGROUND_LOCATION
                            ) != PackageManager.PERMISSION_GRANTED
                        ) {
                            showRequestBackgroundPermissionDialog()
                        }
                    }

                }
            }
            else ->  super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        }
    }

    /**
     * Pass data for geofence creation to Geofication Details View Model
     */
    private fun saveInGeofication() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            if (!foregroundLocationPermissionGranted || !foregroundAndBackgroundLocationPermissionGranted) {
                return
            }
        } else {
            if (foregroundLocationPermissionGranted) {
                return
            }
        }

        // Return if location is not selected or null
        if (mapsViewModel.selectedLocationLatLng.value == null) {
            return
        }

        sharedViewModel.updateLocationNotification(mapsViewModel.selectedLocationLatLng.value!!, mapsViewModel.selectedLocationAddressString.value ?: "")
    }

    /**
     * Init search view
     */
    private fun initSearch() {
        // Init adapter
        val mapsSearchAdapter = MapsSearchRecyclerAdapter(mapsViewModel, MapSearchClickListener { address ->
            binding.mapsSearchView.hide()
            mapsViewModel.setSelectedLocationLatLng(LatLng(address.latitude, address.longitude))
            map.animateCamera(
                CameraUpdateFactory.newLatLngZoom(
                    LatLng(
                        address.latitude,
                        address.longitude
                    ),
                    DEFAULT_ZOOM.toFloat()
                )
            )
        })
        binding.mapsSearchRecyclerView.adapter = mapsSearchAdapter

        // Add dividers to recycler view
        val divider = MaterialDividerItemDecoration(requireContext(), LinearLayoutManager.VERTICAL)
        divider.isLastItemDecorated = false
        binding.mapsSearchRecyclerView.addItemDecoration(divider)

        // Refresh recycler view as data changes
        mapsViewModel.searchedAddresses.observe(viewLifecycleOwner) {
            mapsSearchAdapter.submitAddressesList(it)
        }


        // Search when search ime button pressed in search view
        binding.mapsSearchView.editText.setOnEditorActionListener { textView, actionId, keyEvent ->
            if (actionId == EditorInfo.IME_ACTION_SEARCH || actionId == EditorInfo.IME_ACTION_DONE) {
                // Execute search
                mapsViewModel.searchAddresses(binding.mapsSearchView.editText.text.toString())
            }

            false
        }

        // Clear addresses list when search view is hiding
        binding.mapsSearchView.addTransitionListener { searchView, previousState, newState ->
            if (newState == SearchView.TransitionState.HIDING) {
                mapsViewModel.clearAddressesList()
            }
        }

        // Hide search view when back is pressed
        requireActivity().onBackPressedDispatcher.addCallback {
            if (binding.mapsSearchView.isShowing) {
                this.isEnabled = true
                binding.mapsSearchView.hide()
            } else {
                this.isEnabled = false
                requireActivity().onBackPressedDispatcher.onBackPressed()
            }
        }
    }

    /**
     * Dialog which explains that background permission is needed
     */
    private fun showRequestBackgroundPermissionDialog() {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle(getString(R.string.dialog_Title_background_location))
            .setMessage(getString(R.string.dialog_background_location_message))
            .setIcon(android.R.drawable.ic_dialog_map)
            .setNegativeButton(getString(R.string.dialog_button_skip)) { dialog, _ ->
                dialog.cancel()
            }
            .setPositiveButton(getString(R.string.dialog_button_open_setings)) { dialog, _ ->
                startActivity(Intent().apply {
                    action = Settings.ACTION_APPLICATION_DETAILS_SETTINGS
                    data = Uri.fromParts("package", BuildConfig.APPLICATION_ID, null)
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                })
                dialog.cancel()
            }
            .show()
    }

    private fun makeSnackbarForPermissions() {
        view?.let {
            Snackbar.make(it,
                getString(R.string.snackbar_location_permission), Snackbar.LENGTH_SHORT)
                .setAction(getString(R.string.settings)) {
                    startActivity(Intent().apply {
                        action = Settings.ACTION_APPLICATION_DETAILS_SETTINGS
                        data = Uri.fromParts("package", BuildConfig.APPLICATION_ID, null)
                        flags = Intent.FLAG_ACTIVITY_NEW_TASK
                    })
                }.show()
        }
    }

    private fun checkBackgroundPermissionAndShowDialog() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            if (ActivityCompat.checkSelfPermission(
                    requireContext(),
                    Manifest.permission.ACCESS_BACKGROUND_LOCATION
                ) == PackageManager.PERMISSION_GRANTED
            ) {
                foregroundAndBackgroundLocationPermissionGranted = true
            } else {
                showRequestBackgroundPermissionDialog()
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    companion object {
        private const val DEFAULT_ZOOM = 15
        private const val REQUEST_FOREGROUND_PERMISSIONS_REQUEST_CODE = 1
    }
}