package com.example.geofications.ui.details

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.View.OnClickListener
import android.view.ViewGroup
import android.widget.TextView
import android.widget.Toast
import com.example.geofications.R
import com.google.android.material.bottomsheet.BottomSheetDialogFragment

class NotifyBottomSheetDialogFragment(private val clickListenerTimeView: BottomSheetItemClickListener, private val clickListenerGeoView: BottomSheetItemClickListener): BottomSheetDialogFragment() {

    // Inflate dialog with layout
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.notify_bottom_sheet_content, container, false)

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
}

class BottomSheetItemClickListener(val clickListener: () -> Unit) {
    fun onClick() = clickListener()
}