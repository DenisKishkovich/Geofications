package com.deniskishkovich.geofications.ui.main

import android.graphics.Paint
import android.view.View
import android.widget.TextView
import androidx.databinding.BindingAdapter

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