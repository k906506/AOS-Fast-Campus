package com.example.imageviewer

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.view.isGone
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions
import com.example.imageviewer.data.models.PhotoResponse
import com.example.imageviewer.databinding.ItemPhotoBinding

class PhotoAdapter : ListAdapter<PhotoResponse, PhotoAdapter.ViewHolder>(diffUtil) {
    inner class ViewHolder(private val binding: ItemPhotoBinding) :
        RecyclerView.ViewHolder(binding.root) {
        fun bindViews(photo: PhotoResponse) {
            val dimensionRatio = photo?.height!! / photo?.width?.toFloat()!!
            val targetWidth =
                binding.root.resources.displayMetrics.widthPixels - (binding.root.paddingStart + binding.root.paddingEnd)
            val targetHeight = (targetWidth * dimensionRatio).toInt()

            binding.contentsContainer.layoutParams = binding.contentsContainer.layoutParams.apply {
                height = targetHeight
            }

            Glide.with(binding.root)
                .load(photo.urls?.regular)
                .thumbnail(
                    Glide.with(binding.root)
                        .load(photo.urls?.thumb)
                        .transition(DrawableTransitionOptions.withCrossFade())
                )
                .override(targetWidth, targetHeight)
                .into(binding.photoImageView)

            Glide.with(binding.root)
                .load(photo.user?.profileImageUrls?.small)
                .placeholder(R.drawable.shape_profile_placeholder)
                .circleCrop()
                .into(binding.profileImageView)

            if (photo.user?.name.isNullOrBlank()) {
                binding.authorTextView.isGone = true
            } else {
                binding.authorTextView.isGone = false
                binding.authorTextView.text = photo.user?.name
            }

            if (photo.description.isNullOrBlank()) {
                binding.descriptionTextView.isGone = true
            } else {
                binding.descriptionTextView.isGone = false
                binding.descriptionTextView.text = photo.description
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        return ViewHolder(
            ItemPhotoBinding.inflate(
                LayoutInflater.from(parent.context),
                parent,
                false
            )
        )
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        return holder.bindViews(currentList[position])
    }


    companion object {
        private val diffUtil = object : DiffUtil.ItemCallback<PhotoResponse>() {
            override fun areItemsTheSame(oldItem: PhotoResponse, newItem: PhotoResponse): Boolean {
                return oldItem.id == newItem.id
            }

            override fun areContentsTheSame(
                oldItem: PhotoResponse,
                newItem: PhotoResponse
            ): Boolean {
                return oldItem == newItem
            }
        }
    }
}