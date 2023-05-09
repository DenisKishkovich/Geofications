package com.example.geofications

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.geofications.data.Geofication
import com.example.geofications.databinding.ListItemBinding

class MainRecyclerAdapter(var data: List<Geofication>):
    ListAdapter<Geofication, MainRecyclerAdapter.ViewHolder>(GeoficationDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        return ViewHolder.from(parent)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = data[position]
        holder.bind(item)
    }

    override fun getItemCount(): Int {
        return data.size
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