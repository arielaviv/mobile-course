package com.lux.field.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.lux.field.domain.model.DistributionPoint
import com.lux.field.domain.model.DpType

@Entity(tableName = "distribution_points")
data class DistributionPointEntity(
    @PrimaryKey val id: String,
    val label: String,
    val type: String,
    val latitude: Double,
    val longitude: Double,
    val photoPath: String?,
    val notes: String,
    val createdAt: Long,
    val createdBy: String,
) {
    fun toDomain(): DistributionPoint = DistributionPoint(
        id = id,
        label = label,
        type = DpType.valueOf(type),
        latitude = latitude,
        longitude = longitude,
        photoPath = photoPath,
        notes = notes,
        createdAt = createdAt,
        createdBy = createdBy,
    )

    companion object {
        fun fromDomain(dp: DistributionPoint): DistributionPointEntity =
            DistributionPointEntity(
                id = dp.id,
                label = dp.label,
                type = dp.type.name,
                latitude = dp.latitude,
                longitude = dp.longitude,
                photoPath = dp.photoPath,
                notes = dp.notes,
                createdAt = dp.createdAt,
                createdBy = dp.createdBy,
            )
    }
}
