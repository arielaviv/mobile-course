package com.field.survey.data.remote.dto

import kotlinx.serialization.Serializable

@Serializable
data class WorkOrderTasksDto(
    val workOrderId: String,
    val tasks: List<TaskDto>,
)
