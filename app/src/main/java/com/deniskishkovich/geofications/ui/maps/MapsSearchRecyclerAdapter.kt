package com.deniskishkovich.geofications.ui.maps

import android.location.Address
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.deniskishkovich.geofications.databinding.ListItemBinding
import com.deniskishkovich.geofications.databinding.MapsSearchListItemBinding
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class MapsSearchRecyclerAdapter(private val mapsViewModel: MapsViewModel, private val clickListener: MapSearchClickListener):
    ListAdapter<Address, MapsSearchRecyclerAdapter.ViewHolder>(AddressesDiffCallback()) {

    private val adapterScope = CoroutineScope(Dispatchers.Default)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        return ViewHolder.from(parent)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = getItem(position)
        holder.bind(item, mapsViewModel, clickListener)
    }

    // Needed to refresh list
    fun submitAddressesList(addresses: List<Address>) {
        adapterScope.launch {
            withContext(Dispatchers.Main) {
                submitList(addresses)
            }
        }
    }

    class ViewHolder private constructor(val binding: MapsSearchListItemBinding):
        RecyclerView.ViewHolder(binding.root) {
        fun bind(item: Address, mapsViewModel: MapsViewModel, clickListener: MapSearchClickListener) {
            binding.mapsViewModel = mapsViewModel
            binding.exactAddress = item
            binding.clickListener = clickListener
            binding.executePendingBindings()

        }

        companion object {
            fun from(parent: ViewGroup): ViewHolder {
                val layoutInflater = LayoutInflater.from(parent.context)
                val binding = MapsSearchListItemBinding.inflate(layoutInflater, parent, false)

                return ViewHolder(binding)
            }
        }
    }
}

class AddressesDiffCallback: DiffUtil.ItemCallback<Address>() {
    override fun areItemsTheSame(oldItem: Address, newItem: Address): Boolean {
        return oldItem.getAddressLine(0) == newItem.getAddressLine(0)
    }

    override fun areContentsTheSame(oldItem: Address, newItem: Address): Boolean {
        return oldItem.latitude == newItem.latitude && oldItem.longitude == newItem.longitude
    }
}

class MapSearchClickListener(val clicklistener: (address: Address) -> Unit) {

    fun onClick(address: Address) = clicklistener(address)
}