package com.example.geofications.ui.main

import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import com.example.geofications.data.Geofication
import com.example.geofications.MainRecyclerAdapter
import com.example.geofications.R
import com.example.geofications.data.GeoficationDatabase
import com.example.geofications.databinding.FragmentMainBinding

class MainFragment : Fragment() {

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

        // For testing
        /*val a1 = Geofication(0L, "ASD", "asd")
        val a2 = Geofication(0L, "AS", "as")
        val a3 = Geofication(0L, "A", "a")
        val lisst = listOf<Geofication>(a1, a2, a3)*/
//
        val myAdapter = MainRecyclerAdapter()
        binding.notifList.adapter = myAdapter

        mainViewModel.geoficationList.observe(viewLifecycleOwner, Observer {
            it?.let {
                myAdapter.submitGeoficationList(it)
            }
        })

        // For testing
        //myAdapter.submitGeoficationList(lisst)

        return binding.root
    }

}