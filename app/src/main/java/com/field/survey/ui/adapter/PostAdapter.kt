package com.field.survey.ui.adapter

import android.graphics.BitmapFactory
import android.util.Base64
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.field.survey.R
import com.field.survey.databinding.ItemPostBinding
import com.field.survey.domain.model.DistributionPoint

class PostAdapter(
    private val onClick: (DistributionPoint) -> Unit,
    private val onEdit: ((DistributionPoint) -> Unit)? = null,
    private val onDelete: ((DistributionPoint) -> Unit)? = null,
) : ListAdapter<DistributionPoint, PostAdapter.PostViewHolder>(DistributionPointDiffCallback) {

    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int,
    ): PostViewHolder {
        val binding =
            ItemPostBinding.inflate(
                LayoutInflater.from(parent.context),
                parent,
                false,
            )
        return PostViewHolder(binding)
    }

    override fun onBindViewHolder(
        holder: PostViewHolder,
        position: Int,
    ) {
        holder.bind(getItem(position))
    }

    inner class PostViewHolder(
        private val binding: ItemPostBinding,
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(point: DistributionPoint) {
            val ctx = itemView.context
            binding.tvLabel.text = point.label
            binding.tvType.text = ctx.getString(point.type.labelRes)
            binding.tvNotes.text = point.notes.ifBlank { ctx.getString(R.string.point_no_description) }
            binding.tvAuthor.text = point.createdByName.ifBlank { ctx.getString(R.string.point_author_unknown) }

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

            binding.btnEdit.isVisible = onEdit != null
            binding.btnDelete.isVisible = onDelete != null

            binding.btnEdit.setOnClickListener { onEdit?.invoke(point) }
            binding.btnDelete.setOnClickListener { onDelete?.invoke(point) }
        }
    }

}
