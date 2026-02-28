package com.lux.field.data.repository

import com.lux.field.data.local.dao.DistributionPointDao
import com.lux.field.data.local.entity.DistributionPointEntity
import com.lux.field.domain.model.DistributionPoint
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
