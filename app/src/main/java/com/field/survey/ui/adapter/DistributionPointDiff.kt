package com.field.survey.ui.adapter

import androidx.recyclerview.widget.DiffUtil
import com.field.survey.domain.model.DistributionPoint

object DistributionPointDiffCallback : DiffUtil.ItemCallback<DistributionPoint>() {
    override fun areItemsTheSame(
        old: DistributionPoint,
        new: DistributionPoint,
    ) = old.id == new.id

    override fun areContentsTheSame(
        old: DistributionPoint,
        new: DistributionPoint,
    ) = old == new
}
