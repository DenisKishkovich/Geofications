package com.example.geofications.ui.main

import android.app.NotificationChannel
import android.app.NotificationManager
import android.os.Build
import android.os.Bundle
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
import androidx.fragment.app.Fragment
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.findNavController
import com.example.geofications.GeoficationClickListener
import com.example.geofications.MainRecyclerAdapter
import com.example.geofications.R
import com.example.geofications.data.GeoficationDatabase
import com.example.geofications.databinding.FragmentMainBinding

class MainFragment : Fragment() {

    private lateinit var mainViewModel: MainViewModel

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val binding: FragmentMainBinding =
            DataBindingUtil.inflate(inflater, R.layout.fragment_main, container, false)

        val application = requireNotNull(this.activity).application

        val dataSource = GeoficationDatabase.getInstance(application).geoficationDAO

        val viewModelFactory = MainViewModelFactory(dataSource, application)

        mainViewModel = ViewModelProvider(this, viewModelFactory).get(MainViewModel::class.java)

        binding.mainViewModel = mainViewModel
        binding.lifecycleOwner = this

        // Create the menu
        createMenu()

        // Creating an adapter with click listener. Once is clicked, id is handled to onGeoficationClicked method of viewModel
        val myAdapter = MainRecyclerAdapter(GeoficationClickListener { geoficationID ->
            mainViewModel.onGeoficationClicked(geoficationID)
        }, mainViewModel)
        binding.notifList.adapter = myAdapter

        // Refresh recycler view as database changes
        mainViewModel.geoficationList.observe(viewLifecycleOwner, Observer {
            it?.let {
                myAdapter.submitGeoficationList(it)
            }
        })

        // Add an Observer on the state variable for Navigating.
        mainViewModel.navigateToGeoficationDetails.observe(viewLifecycleOwner, Observer {
            it?.let {
                val argAppBarTitle = if (it == -1L) "Add notification" else "Edit notification"

                this.findNavController().navigate(
                        MainFragmentDirections.actionMainFragmentToGeoficationDetailsFragment(it, argAppBarTitle)
                    )
                mainViewModel.onGeoficationNavigated()
            }
        })

        createChannel(
            getString(R.string.on_time_notification_channel_id),
            getString(R.string.on_time_notification_channel_name)
        )

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
        menuHost.addMenuProvider(object : MenuProvider {
            override fun onCreateMenu(menu: Menu, menuInflater: MenuInflater) {
                menuInflater.inflate(R.menu.main_fragment_menu, menu)
            }

            override fun onMenuItemSelected(menuItem: MenuItem): Boolean {
                return when (menuItem.itemId) {
                    R.id.clear_all_menu_item -> {
                        mainViewModel.onClear()

                        Toast.makeText(context, "Database cleared", Toast.LENGTH_SHORT).show()
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