package com.field.survey.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.field.survey.domain.model.DistributionPoint
import com.field.survey.domain.model.toDpType

@Entity(tableName = "distribution_points")
data class DistributionPointEntity(
    @PrimaryKey val id: String,
    val label: String,
    val type: String,
    val latitude: Double,
    val longitude: Double,
    val photoPath: String?,
    val imageBase64: String?,
    val notes: String,
    val createdAt: Long,
    val createdBy: String,
    val createdByName: String,
) {
    fun toDomain(): DistributionPoint = DistributionPoint(
        id = id,
        label = label,
        type = type.toDpType(),
        latitude = latitude,
        longitude = longitude,
        photoPath = photoPath,
        imageBase64 = imageBase64,
        notes = notes,
        createdAt = createdAt,
        createdBy = createdBy,
        createdByName = createdByName,
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
                imageBase64 = dp.imageBase64,
                notes = dp.notes,
                createdAt = dp.createdAt,
                createdBy = dp.createdBy,
                createdByName = dp.createdByName,
            )
    }
}
