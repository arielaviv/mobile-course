package com.lux.field.domain.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
enum class WorkOrderType {
    @SerialName("feeder_install") FEEDER_INSTALL,
    @SerialName("distribution_install") DISTRIBUTION_INSTALL,
    @SerialName("drop_install") DROP_INSTALL,
    @SerialName("splice") SPLICE,
    @SerialName("duct_install") DUCT_INSTALL,
    @SerialName("pole_install") POLE_INSTALL,
    @SerialName("maintenance") MAINTENANCE,
    @SerialName("survey") SURVEY,
    @SerialName("emergency_repair") EMERGENCY_REPAIR,
}
