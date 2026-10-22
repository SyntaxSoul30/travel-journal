package com.example.csci_asp.scrapbook

import android.net.Uri
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.ImageView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.csci_asp.databinding.ItemPhotoPreviewBinding

class PhotoPreviewAdapter :
    ListAdapter<Uri, PhotoPreviewAdapter.PhotoViewHolder>(DiffCallback) {

    private var rotationPattern: List<Float> = emptyList()

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PhotoViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        val binding = ItemPhotoPreviewBinding.inflate(inflater, parent, false)
        return PhotoViewHolder(binding)
    }

    override fun onBindViewHolder(holder: PhotoViewHolder, position: Int) {
        holder.bind(getItem(position), rotationFor(position))
    }

    fun setRotationPattern(pattern: List<Float>) {
        rotationPattern = pattern
        notifyDataSetChanged()
    }

    private fun rotationFor(position: Int): Float =
        if (rotationPattern.isEmpty()) 0f else rotationPattern[position % rotationPattern.size]

    class PhotoViewHolder(
        private val binding: ItemPhotoPreviewBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(uri: Uri, rotation: Float) {
            binding.imagePreview.scaleType = ImageView.ScaleType.FIT_CENTER
            Glide.with(binding.imagePreview)
                .load(uri)
                .fitCenter()
                .into(binding.imagePreview)
            binding.frameContainer.rotation = rotation
        }
    }

    private object DiffCallback : DiffUtil.ItemCallback<Uri>() {
        override fun areItemsTheSame(oldItem: Uri, newItem: Uri): Boolean = oldItem == newItem
        override fun areContentsTheSame(oldItem: Uri, newItem: Uri): Boolean = oldItem == newItem
    }
}

