package com.field.survey.ui.stats

import com.field.survey.domain.model.DistributionPoint
import com.field.survey.domain.model.DpType

data class StatsUiState(
    val total: Int,
    val byType: Map<DpType, Int>,
    val topContributors: List<ContributorRow>,
    val weeklyBuckets: List<Int>,
    val points: List<DistributionPoint>,
)

data class ContributorRow(
    val userId: String,
    val name: String,
    val count: Int,
    val initial: String,
)
