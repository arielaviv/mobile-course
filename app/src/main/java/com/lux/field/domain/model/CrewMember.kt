package com.lux.field.domain.model

data class CrewMember(
    val id: String,
    val name: String,
    val phone: String,
    val role: String,
    val crewId: String,
)
