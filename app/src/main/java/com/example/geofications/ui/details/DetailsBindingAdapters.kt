package com.example.geofications.ui.details

import android.text.format.DateUtils
import android.view.View
import android.widget.CheckBox
import android.widget.TextView
import androidx.databinding.BindingAdapter
import com.google.android.material.chip.Chip
import java.text.DateFormat
import java.util.Calendar

@BindingAdapter("app:set_timestamp_text")
fun setTimestamp(textView: TextView, editedTimeInMillis: Long?) {
    editedTimeInMillis?.let {
        val dateFormatter = DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.SHORT)
        textView.text = dateFormatter.format(editedTimeInMillis)
    }
}

@BindingAdapter("app:set_timestamp_text")
fun setTimestamp(chipView: Chip, editedTimeInMillis: Long?) {
    editedTimeInMillis?.let {
        val dateTimeCharSequence = DateUtils.getRelativeDateTimeString(
            chipView.context,
            editedTimeInMillis,
            DateUtils.MINUTE_IN_MILLIS,
            DateUtils.WEEK_IN_MILLIS,
            DateUtils.FORMAT_SHOW_TIME
        )
        chipView.text = dateTimeCharSequence
    }
}

@BindingAdapter("app:set_visibility")
fun setVisibility(checkBox: CheckBox, isNewGeofication: Boolean) {
    if (isNewGeofication) {
        checkBox.visibility = View.GONE
    } else {
        checkBox.visibility = View.VISIBLE
    }
}

@BindingAdapter("app:set_visibility")
fun setVisibility(textView: TextView, isNewGeofication: Boolean) {
    if (isNewGeofication) {
        textView.visibility = View.GONE
    } else {
        textView.visibility = View.VISIBLE
    }
}

@BindingAdapter("app:set_visibility")
fun setVisibility(chipView: Chip, dateTimeInMillisForAlarm: Long?) {
    if (dateTimeInMillisForAlarm == null) {
        chipView.visibility = View.GONE
    } else {
        chipView.visibility = View.VISIBLE
    }
}