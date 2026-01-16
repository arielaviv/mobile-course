package com.lux.field.domain.model

data class TaskStep(
    val id: String,
    val sequence: Int,
    val label: String,
    val description: String,
    val isCompleted: Boolean,
)
