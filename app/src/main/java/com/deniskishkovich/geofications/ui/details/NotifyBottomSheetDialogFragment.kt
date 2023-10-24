package com.deniskishkovich.geofications.ui.details

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import com.deniskishkovich.geofications.R
import com.deniskishkovich.geofications.databinding.NotifyBottomSheetContentBinding
import com.google.android.material.bottomsheet.BottomSheetDialogFragment

class NotifyBottomSheetDialogFragment(private val clickListenerTimeView: BottomSheetItemClickListener, private val clickListenerGeoView: BottomSheetItemClickListener): BottomSheetDialogFragment() {

    private var _binding: NotifyBottomSheetContentBinding? = null

    // This property is only valid between onCreateView and
    // onDestroyView.
    private val binding get() = _binding!!

    // Inflate dialog with layout
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        _binding = NotifyBottomSheetContentBinding.inflate(inflater, container, false)
        return binding.root

    }

    // Set click listeners for the views
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val timeView = view.findViewById<TextView>(R.id.time_selection_textView)
        timeView.setOnClickListener {
            clickListenerTimeView.onClick()
            dismiss()
        }

        val geoView = view.findViewById<TextView>(R.id.geo_selection_textView)
        geoView.setOnClickListener {
            clickListenerGeoView.onClick()
            dismiss()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}

class BottomSheetItemClickListener(val clickListener: () -> Unit) {
    fun onClick() = clickListener()
}