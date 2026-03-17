package com.field.survey.data.repository

import com.field.survey.data.local.dao.DistributionPointDao
import com.field.survey.data.local.entity.DistributionPointEntity
import com.field.survey.domain.model.DistributionPoint
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DistributionPointRepository @Inject constructor(
    private val dao: DistributionPointDao,
) {
    fun observeAll(): Flow<List<DistributionPoint>> =
        dao.observeAll().map { entities -> entities.map { it.toDomain() } }

    suspend fun save(dp: DistributionPoint) {
        dao.insert(DistributionPointEntity.fromDomain(dp))
    }
}
