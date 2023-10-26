package com.deniskishkovich.geofications.ui.maps

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationManager
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.app.ActivityCompat
import androidx.core.content.getSystemService
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.viewModels
import com.deniskishkovich.geofications.R
import com.deniskishkovich.geofications.ui.details.GeoficationDetailsViewModel
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MarkerOptions
import com.google.android.material.appbar.MaterialToolbar

class MapsFragment : DialogFragment(), OnMapReadyCallback {

    private val sharedViewModel: GeoficationDetailsViewModel by viewModels(ownerProducer = { requireParentFragment() })

    private lateinit var map: GoogleMap

    private lateinit var fusedLocationProviderClient: FusedLocationProviderClient

    private var lastKnownLocation: Location? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val rootView =  inflater.inflate(R.layout.fragment_maps, container, false)

        // set Toolbar
        val toolbar = rootView.findViewById<MaterialToolbar>(R.id.maps_toolbar)
        toolbar.setNavigationOnClickListener { requireActivity().onBackPressedDispatcher.onBackPressed() }
        toolbar.setOnMenuItemClickListener {
            when (it.itemId) {
                R.id.save_menu_item -> {
                    Toast.makeText(context, "saved", Toast.LENGTH_SHORT).show()
                    requireActivity().onBackPressedDispatcher.onBackPressed()
                    true
                }
                else -> false
            }
        }

        return rootView
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        fusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(requireActivity())

        val mapFragment = childFragmentManager.findFragmentById(R.id.map) as SupportMapFragment?
        mapFragment?.getMapAsync(this)
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

        enableMyLocation()

        getDeviceLocation()
    }

    private fun enableMyLocation() {
        if (ActivityCompat.checkSelfPermission(
                requireContext(),
                Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(
                requireContext(),
                Manifest.permission.ACCESS_COARSE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            // TODO: Consider calling
            //    ActivityCompat#requestPermissions
            // here to request the missing permissions, and then overriding
            //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
            //                                          int[] grantResults)
            // to handle the case where the user grants the permission. See the documentation
            // for ActivityCompat#requestPermissions for more details.
            return
        }
        map.isMyLocationEnabled = true
    }

    @SuppressLint("MissingPermission")
    private fun getDeviceLocation() {
        val locationResult = fusedLocationProviderClient.lastLocation
        locationResult.addOnCompleteListener(requireActivity()) { task ->
            if (task.isSuccessful) {
                // Set the map's camera position to the current location of the device.
                lastKnownLocation = task.result

                if (lastKnownLocation != null) {
                    map.moveCamera(CameraUpdateFactory.newLatLngZoom(
                        LatLng(lastKnownLocation!!.latitude, lastKnownLocation!!.longitude),
                        DEFAULT_ZOOM.toFloat()))
                }
            }
        }
    }

    companion object {
        private const val DEFAULT_ZOOM = 15
    }
}