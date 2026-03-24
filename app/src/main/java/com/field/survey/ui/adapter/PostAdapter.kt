package com.field.survey.ui.adapter

import android.graphics.BitmapFactory
import android.util.Base64
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.field.survey.databinding.ItemPostBinding
import com.field.survey.domain.model.DistributionPoint
import com.field.survey.domain.model.DpType

class PostAdapter(
    private val onClick: (DistributionPoint) -> Unit,
) : ListAdapter<DistributionPoint, PostAdapter.PostViewHolder>(DiffCallback) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PostViewHolder {
        val binding = ItemPostBinding.inflate(
            LayoutInflater.from(parent.context), parent, false,
        )
        return PostViewHolder(binding)
    }

    override fun onBindViewHolder(holder: PostViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class PostViewHolder(
        private val binding: ItemPostBinding,
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(point: DistributionPoint) {
            binding.tvLabel.text = point.label
            binding.tvType.text = itemView.context.getString(point.type.labelRes)
            binding.tvNotes.text = point.notes.ifBlank { "No description" }
            binding.tvAuthor.text = point.createdByName.ifBlank { "Unknown" }

            if (!point.imageBase64.isNullOrBlank()) {
                try {
                    val bytes = Base64.decode(point.imageBase64, Base64.DEFAULT)
                    val bmp = BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
                    binding.ivPhoto.setImageBitmap(bmp)
                } catch (_: Exception) {
                    binding.ivPhoto.setImageResource(android.R.drawable.ic_menu_gallery)
                }
            } else {
                binding.ivPhoto.setImageResource(android.R.drawable.ic_menu_gallery)
            }

            binding.root.setOnClickListener { onClick(point) }
        }
    }

    companion object DiffCallback : DiffUtil.ItemCallback<DistributionPoint>() {
        override fun areItemsTheSame(old: DistributionPoint, new: DistributionPoint) =
            old.id == new.id

        override fun areContentsTheSame(old: DistributionPoint, new: DistributionPoint) =
            old == new
    }
}
