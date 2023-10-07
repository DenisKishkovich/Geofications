package com.example.geofications.ui.details

import android.widget.TextView
import androidx.databinding.BindingAdapter
import com.google.android.material.chip.Chip
import java.text.DateFormat
import java.util.Calendar

@BindingAdapter("app:set_timestamp_text")
fun setTimestamp(textView: TextView, editedTimeInMillis: Long?) {
    editedTimeInMillis?.let {
        val dateFormatter = DateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.SHORT)
        textView.text = dateFormatter.format(editedTimeInMillis)
    }
}

@BindingAdapter("app:set_timestamp_text")
fun setTimestamp(chipView: Chip, editedTimeInMillis: Long?) {
    editedTimeInMillis?.let {
        val dateFormatter = DateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.SHORT)
        chipView.text = dateFormatter.format(editedTimeInMillis)
    }
}