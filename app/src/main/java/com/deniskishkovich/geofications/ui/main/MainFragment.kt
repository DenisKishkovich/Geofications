package com.deniskishkovich.geofications.ui.main

import android.app.NotificationChannel
import android.app.NotificationManager
import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.RecyclerView
import com.deniskishkovich.geofications.GeoficationClickListener
import com.deniskishkovich.geofications.MainRecyclerAdapter
import com.deniskishkovich.geofications.R
import com.deniskishkovich.geofications.data.GeoficationDatabase
import com.deniskishkovich.geofications.databinding.FragmentMainBinding
import com.google.android.material.snackbar.Snackbar

class MainFragment : Fragment() {

    private lateinit var mainViewModel: MainViewModel

    private lateinit var binding: FragmentMainBinding

    private lateinit var myAdapter: MainRecyclerAdapter

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding =
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
                R.id.delete_completed_menu_item -> {
                    mainViewModel.deleteCompleted()
                    true
                }
                else -> false
            }}

        // Creating an adapter with click listener. Once is clicked, id is handled to onGeoficationClicked method of viewModel
        myAdapter = MainRecyclerAdapter(GeoficationClickListener { geoficationID ->
            mainViewModel.onGeoficationClicked(geoficationID)
        }, mainViewModel)
        binding.notifList.adapter = myAdapter
        setSwipeToDelete()

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

    private fun setSwipeToDelete() {
        ItemTouchHelper(object : ItemTouchHelper.SimpleCallback(0, ItemTouchHelper.LEFT) {
            override fun onMove(
                recyclerView: RecyclerView,
                viewHolder: RecyclerView.ViewHolder,
                target: RecyclerView.ViewHolder
            ): Boolean {
                return false
            }

            override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {
                if (mainViewModel.geoficationList.value == null) {
                    return
                }

                val geoficationId: Long = viewHolder.itemView.tag as Long

                // Get geofication to delete
                val deletedGeofication = mainViewModel.geoficationList.value!!.find { geofication -> geoficationId == geofication.id }
                    ?: return

                mainViewModel.swipeDeleteGeofication(deletedGeofication.id)

                Snackbar.make(binding.notifList, getString(R.string.deleted), Snackbar.LENGTH_LONG)
                    .setAction(
                        getString(R.string.undo)
                    ) {
                        mainViewModel.undoDeleteGeofication(deletedGeofication)
                    }
                    .show()
            }

        }).attachToRecyclerView(binding.notifList)
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
                getString(R.string.notif_chanel_description)

            val notificationManager =
                requireActivity().getSystemService(NotificationManager::class.java)
            notificationManager.createNotificationChannel(notificationChannel)
        }
    }
}