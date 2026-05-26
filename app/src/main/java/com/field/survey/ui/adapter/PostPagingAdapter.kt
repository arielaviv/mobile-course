package com.field.survey.ui.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.paging.PagingDataAdapter
import androidx.recyclerview.widget.RecyclerView
import com.field.survey.R
import com.field.survey.databinding.ItemPostBinding
import com.field.survey.domain.model.DistributionPoint
import com.field.survey.ui.util.loadDpImage

class PostPagingAdapter(
    private val onClick: (DistributionPoint) -> Unit,
) : PagingDataAdapter<DistributionPoint, PostPagingAdapter.PostViewHolder>(DistributionPointDiffCallback) {

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
        getItem(position)?.let { holder.bind(it) }
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
        }
    }
}
