package com.example.geofications.ui.details

import android.app.AlertDialog
import android.app.Dialog
import android.app.TimePickerDialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.CalendarView
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.viewModels
import com.example.geofications.R
import java.util.Calendar
import java.util.Locale

class TimeSelectionDialogFragment : DialogFragment() {

    private val dialogView: View by lazy {
        val inflater = requireActivity().layoutInflater
        inflater.inflate(R.layout.dialog_time_selection, null)
    }
    private val sharedViewModel: GeoficationDetailsViewModel by viewModels(ownerProducer = { requireParentFragment() })

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val dialog = activity?.let {
            val builder = AlertDialog.Builder(it)

            builder.setView(dialogView)
                .setPositiveButton(
                    getString(R.string.dialog_save_button)
                ) { dialog, id ->
                    sharedViewModel.updateDateTimeAlarm()
                    dialog.cancel()
                }
                .setNegativeButton(
                    getString(R.string.dialog_cancel_button)
                ) { dialog, id ->
                    dialog.cancel()
                }
            builder.create()
        } ?: throw IllegalStateException("Activity cannot be null")

        // Hide positive button if time is not set
        dialog.setOnShowListener {
            dialog.getButton(Dialog.BUTTON_POSITIVE).isEnabled = false
            sharedViewModel.hourForAlarm.observe(viewLifecycleOwner) {
                dialog.getButton(Dialog.BUTTON_POSITIVE).isEnabled = it != null
            }
        }

        return dialog
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return dialogView
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val selectTimeButton = getView()?.findViewById(R.id.dialog_time_button) as Button
        val calendarView = getView()?.findViewById(R.id.calendarView) as CalendarView

        initTimePicker(selectTimeButton)
        val calendar = Calendar.getInstance()
        val currentDate = calendar.timeInMillis
        calendarView.minDate = currentDate

        if (sharedViewModel.dateInMillisForAlarm.value == null) {
            sharedViewModel.dateInMillisForAlarm.value = calendar.timeInMillis
        }
        calendarView.date = sharedViewModel.dateInMillisForAlarm.value!!

        calendarView.setOnDateChangeListener { selectedCalendarView, year, month, day ->
            val calendarSelected = Calendar.getInstance()
            calendarSelected.set(
                year,
                month,
                day
            )
            selectedCalendarView.date = calendarSelected.timeInMillis
            sharedViewModel.dateInMillisForAlarm.value = selectedCalendarView.date
        }

    }

    /**
     * Initialize timePicker
     */
    private fun initTimePicker(selectButton: Button) {
        if (sharedViewModel.hourForAlarm.value != null && sharedViewModel.minuteForAlarm.value != null) {
            selectButton.text =
                String.format(
                    Locale.getDefault(),
                    "%02d:%02d",
                    sharedViewModel.hourForAlarm.value,
                    sharedViewModel.minuteForAlarm.value
                )
        }

        selectButton.setOnClickListener {
            val onTimeSetListener =
                TimePickerDialog.OnTimeSetListener { _, selectedHour, selectedMinute ->
                    sharedViewModel.hourForAlarm.value = selectedHour
                    sharedViewModel.minuteForAlarm.value = selectedMinute
                    selectButton.text =
                        String.format(
                            Locale.getDefault(),
                            "%02d:%02d",
                            sharedViewModel.hourForAlarm.value,
                            sharedViewModel.minuteForAlarm.value
                        )
                }
            val timePickerDialog = TimePickerDialog(
                context,
                onTimeSetListener,
                sharedViewModel.hourForAlarm.value ?: 0,
                sharedViewModel.minuteForAlarm.value ?: 0,
                true
            )
            timePickerDialog.show()
        }
    }
}