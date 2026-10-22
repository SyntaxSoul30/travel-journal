package com.example.csci_asp.photos

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.csci_asp.databinding.ItemDevicePhotoBinding

class DevicePhotoAdapter(
    private val onPhotoClicked: (DevicePhoto) -> Unit
) : ListAdapter<DevicePhoto, DevicePhotoAdapter.DevicePhotoViewHolder>(DiffCallback) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): DevicePhotoViewHolder {
        val binding = ItemDevicePhotoBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return DevicePhotoViewHolder(binding)
    }

    override fun onBindViewHolder(holder: DevicePhotoViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class DevicePhotoViewHolder(
        private val binding: ItemDevicePhotoBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(photo: DevicePhoto) {
            Glide.with(binding.imageDevicePhoto)
                .load(photo.uri)
                .centerCrop()
                .into(binding.imageDevicePhoto)
            binding.root.setOnClickListener { onPhotoClicked(photo) }
        }
    }

    private object DiffCallback : DiffUtil.ItemCallback<DevicePhoto>() {
        override fun areItemsTheSame(oldItem: DevicePhoto, newItem: DevicePhoto): Boolean =
            oldItem.uri == newItem.uri

        override fun areContentsTheSame(oldItem: DevicePhoto, newItem: DevicePhoto): Boolean =
            oldItem == newItem
    }
}

