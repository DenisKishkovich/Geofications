package com.example.geofications

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.geofications.data.Geofication
import com.example.geofications.databinding.ListItemBinding
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class MainRecyclerAdapter():
    ListAdapter<Geofication, MainRecyclerAdapter.ViewHolder>(GeoficationDiffCallback()) {

    private val adapterScope = CoroutineScope(Dispatchers.Default)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        return ViewHolder.from(parent)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = getItem(position)
        holder.bind(item)
    }

    /*override fun getItemCount(): Int {
        return data.size
    }*/

    // Needed to refresh list
    fun submitGeoficationList(geoficationsList: List<Geofication>?) {
        adapterScope.launch {
            withContext(Dispatchers.Main) {
                submitList(geoficationsList)
            }
        }
    }

    class ViewHolder private constructor(val binding: ListItemBinding): RecyclerView.ViewHolder(binding.root) {
        fun bind(item: Geofication) {
            binding.titleTextView.text = item.title
            binding.descriptionTextView.text = item.description
        }

        companion object {
            fun from(parent: ViewGroup): ViewHolder {
                val layoutInflater = LayoutInflater.from(parent.context)
                val binding = ListItemBinding.inflate(layoutInflater, parent, false)

                return ViewHolder(binding)
            }
        }
    }
}

class GeoficationDiffCallback: DiffUtil.ItemCallback<Geofication>() {
    override fun areItemsTheSame(oldItem: Geofication, newItem: Geofication): Boolean {
        return oldItem.id == newItem.id
    }

    override fun areContentsTheSame(oldItem: Geofication, newItem: Geofication): Boolean {
        return oldItem == newItem
    }

}