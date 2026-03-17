package com.field.survey.domain.model

data class CrewMember(
    val id: String,
    val name: String,
    val phone: String,
    val role: String,
    val crewId: String,
)
