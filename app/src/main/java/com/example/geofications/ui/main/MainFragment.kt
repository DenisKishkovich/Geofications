package com.example.geofications.ui.main

import android.os.Bundle
import android.view.*
import android.widget.Toast
import androidx.core.view.MenuHost
import androidx.core.view.MenuProvider
import androidx.fragment.app.Fragment
import androidx.databinding.DataBindingUtil
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

    private lateinit var binding: FragmentMainBinding

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

        val mainViewModel = ViewModelProvider(this, viewModelFactory).get(MainViewModel::class.java)

        binding.mainViewModel = mainViewModel
        binding.lifecycleOwner = this

        // Creating an adapter with click listener. Once is clicked, id is handled to onGeoficationClicked method of viewModel
        val myAdapter = MainRecyclerAdapter(GeoficationClickListener { geoficationID ->
            mainViewModel.onGeoficationClicked(geoficationID)
        })
        binding.notifList.adapter = myAdapter

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

        // Refresh recycler view as database changes
        mainViewModel.geoficationList.observe(viewLifecycleOwner, Observer {
            it?.let {
                myAdapter.submitGeoficationList(it)
            }
        })

        // Add an Observer on the state variable for Navigating.
        mainViewModel.navigateToGeoficationDetails.observe(viewLifecycleOwner, Observer {
            it?.let {
                this.findNavController().navigate(MainFragmentDirections.actionMainFragmentToGeoficationDetailsFragment(it))
                mainViewModel.onGeoficationNavigated()
            }
        })

        return binding.root
    }
}