package com.field.survey.ui.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.field.survey.R
import com.field.survey.databinding.ItemPostBinding
import com.field.survey.domain.model.DistributionPoint
import com.field.survey.ui.util.loadDpImage

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

            binding.ivPhoto.loadDpImage(point)

            binding.root.setOnClickListener { onClick(point) }

            binding.btnEdit.isVisible = onEdit != null
            binding.btnDelete.isVisible = onDelete != null

            binding.btnEdit.setOnClickListener { onEdit?.invoke(point) }
            binding.btnDelete.setOnClickListener { onDelete?.invoke(point) }
        }
    }

}
