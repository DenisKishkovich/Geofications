package com.deniskishkovich.geofications.ui.main

import android.app.NotificationChannel
import android.app.NotificationManager
import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.RecyclerView
import com.deniskishkovich.geofications.GeoficationClickListener
import com.deniskishkovich.geofications.MainRecyclerAdapter
import com.deniskishkovich.geofications.R
import com.deniskishkovich.geofications.data.GeoficationDatabase
import com.deniskishkovich.geofications.databinding.FragmentMainBinding

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


        // Handle clicks on menu items
        binding.mainFragmentToolbar.setOnMenuItemClickListener {
            when (it.itemId) {
                R.id.clear_all_menu_item -> {
                    mainViewModel.onClear()
                    Toast.makeText(context, "Database cleared", Toast.LENGTH_SHORT).show()
                    true
                }
                else -> false
            }}

        // Creating an adapter with click listener. Once is clicked, id is handled to onGeoficationClicked method of viewModel
        val myAdapter = MainRecyclerAdapter(GeoficationClickListener { geoficationID ->
            mainViewModel.onGeoficationClicked(geoficationID)
        }, mainViewModel)
        binding.notifList.adapter = myAdapter

        // Refresh recycler view as database changes
        mainViewModel.geoficationList.observe(viewLifecycleOwner) {
            it?.let {
                myAdapter.submitGeoficationList(it)
            }
        }

        // Scroll to top of geofication list if new item added or item changed
        myAdapter.registerAdapterDataObserver(object : RecyclerView.AdapterDataObserver() {
            override fun onItemRangeInserted(positionStart: Int, itemCount: Int) {
                super.onItemRangeInserted(positionStart, itemCount)
                binding.notifList.scrollToPosition(positionStart)
            }

            override fun onItemRangeChanged(positionStart: Int, itemCount: Int) {
                super.onItemRangeChanged(positionStart, itemCount)
                binding.notifList.scrollToPosition(positionStart)
            }
        })

        // Add an Observer on the state variable for Navigating.
        mainViewModel.navigateToGeoficationDetails.observe(viewLifecycleOwner) {
            it?.let {
                val argAppBarTitle =
                    if (it == -1L) getString(R.string.add_notification) else getString(R.string.edit_notification)

                this.findNavController().navigate(
                    MainFragmentDirections.actionMainFragmentToGeoficationDetailsFragment(
                        it,
                        argAppBarTitle
                    )
                )
                mainViewModel.onGeoficationNavigated()
            }
        }

        createChannel(
            getString(R.string.on_time_notification_channel_id),
            getString(R.string.on_time_notification_channel_name)
        )

        return binding.root
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