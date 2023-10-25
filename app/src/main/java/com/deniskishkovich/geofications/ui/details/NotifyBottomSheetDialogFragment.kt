package com.deniskishkovich.geofications.ui.details

import android.app.Dialog
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import com.deniskishkovich.geofications.R
import com.deniskishkovich.geofications.databinding.NotifyBottomSheetContentBinding
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import javax.security.auth.callback.Callback

class NotifyBottomSheetDialogFragment: BottomSheetDialogFragment() {

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

        val parentGeoficationDetailsFragment = parentFragment as GeoficationDetailsFragment

        binding.timeSelectionTextView.setOnClickListener {
            parentGeoficationDetailsFragment.showTimeSelectionDialog()
            dismiss()
        }

        binding.geoSelectionTextView.setOnClickListener {
            parentGeoficationDetailsFragment.showMapsDialog()
            dismiss()
        }
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val dialog =  super.onCreateDialog(savedInstanceState)

        // Shows dialog fully expanded (for horizontal mode)
        val modalBottomSheetBehavior = (dialog as BottomSheetDialog).behavior

        dialog.setOnShowListener {
            modalBottomSheetBehavior.state = BottomSheetBehavior.STATE_EXPANDED
        }

            return dialog
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}