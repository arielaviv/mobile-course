package com.field.survey.data.remote.dto

import kotlinx.serialization.Serializable

@Serializable
data class LoginRequest(
    val phone: String,
    val code: String,
)
