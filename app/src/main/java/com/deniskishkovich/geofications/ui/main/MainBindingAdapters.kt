package com.deniskishkovich.geofications.ui.main

import android.graphics.Paint
import android.text.format.DateUtils
import android.view.View
import android.widget.TextView
import androidx.databinding.BindingAdapter
import com.google.android.material.chip.Chip

@BindingAdapter("app:completedGeofication")
fun setStyle(textView: TextView, isCompleted: Boolean) {
    if (isCompleted) {
        textView.paintFlags = textView.paintFlags or Paint.STRIKE_THRU_TEXT_FLAG
    } else {
        textView.paintFlags = textView.paintFlags and Paint.STRIKE_THRU_TEXT_FLAG.inv()
    }
}

@BindingAdapter("app:is_description_empty")
fun setVisibility(textView: TextView, description: String){
    val isDescriptionEmpty = description.isEmpty()
    if (isDescriptionEmpty) {
        textView.visibility = View.GONE
    } else {
        textView.visibility = View.VISIBLE
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
fun setVisibility(chipView: Chip, dateTimeInMillisForAlarm: Long?) {
    if (dateTimeInMillisForAlarm == null) {
        chipView.visibility = View.GONE
    } else {
        chipView.visibility = View.VISIBLE
    }
}

@BindingAdapter("app:set_visibility")
fun setVisibility(chipView: Chip, isNotificationSet: Boolean) {
    if (!isNotificationSet) {
        chipView.visibility = View.GONE
    } else {
        chipView.visibility = View.VISIBLE
    }
}