package com.field.survey.ui.feed

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.field.survey.databinding.ItemRecentActivityBinding
import com.field.survey.domain.model.DistributionPoint
import com.field.survey.ui.util.TimeAgo
import com.field.survey.ui.util.colorRes

class RecentActivityAdapter(
    private val onClick: (DistributionPoint) -> Unit,
) : ListAdapter<DistributionPoint, RecentActivityAdapter.ActivityViewHolder>(DiffCallback) {

    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int,
    ): ActivityViewHolder {
        val binding = ItemRecentActivityBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ActivityViewHolder(binding)
    }

    override fun onBindViewHolder(
        holder: ActivityViewHolder,
        position: Int,
    ) {
        holder.bind(getItem(position))
    }

    inner class ActivityViewHolder(
        private val binding: ItemRecentActivityBinding,
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(point: DistributionPoint) {
            val ctx = itemView.context
            binding.tvLabel.text = point.label.ifBlank { "Untitled" }
            binding.tvType.text = ctx.getString(point.type.labelRes)
            binding.typeDot.setBackgroundColor(ContextCompat.getColor(ctx, point.type.colorRes()))
            val ts = point.updatedAt ?: point.createdAt
            val who = point.updatedBy?.takeIf { it.isNotBlank() } ?: point.createdByName.ifBlank { "Unknown" }
            val verb = if (point.updatedAt != null) "edited" else "added"
            binding.tvMeta.text = "$verb ${TimeAgo.format(ts)} · $who"
            binding.root.setOnClickListener { onClick(point) }
        }
    }

    companion object DiffCallback : DiffUtil.ItemCallback<DistributionPoint>() {
        override fun areItemsTheSame(
            old: DistributionPoint,
            new: DistributionPoint,
        ) = old.id == new.id

        override fun areContentsTheSame(
            old: DistributionPoint,
            new: DistributionPoint,
        ) = old == new
    }
}
