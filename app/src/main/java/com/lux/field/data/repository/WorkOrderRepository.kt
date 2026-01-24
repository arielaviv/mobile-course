package com.lux.field.data.repository

import com.lux.field.BuildConfig
import com.lux.field.data.local.dao.TaskDao
import com.lux.field.data.local.dao.WorkOrderDao
import com.lux.field.data.mock.MockDataProvider
import com.lux.field.data.remote.LuxApi
import com.lux.field.domain.model.Task
import com.lux.field.domain.model.WorkOrder
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class WorkOrderRepository @Inject constructor(
    private val api: LuxApi,
    private val workOrderDao: WorkOrderDao,
    private val taskDao: TaskDao,
    private val mockDataProvider: MockDataProvider,
) {
    fun observeWorkOrders(): Flow<List<WorkOrder>> {
        return workOrderDao.observeAll().map { entities ->
            entities.map { it.toDomain() }
        }
    }

    suspend fun refreshWorkOrders(): Result<List<WorkOrder>> {
        return try {
            val workOrders = if (BuildConfig.USE_MOCK_API) {
                mockDataProvider.getWorkOrders()
            } else {
                val dtos = api.getWorkOrders()
                dtos.map { it.toDomain() }
            }
            workOrderDao.insertAll(workOrders.map { it.toEntity() })
            for (wo in workOrders) {
                taskDao.insertAll(wo.tasks.map { it.toEntity() })
            }
            Result.success(workOrders)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getWorkOrderDetail(id: String): Result<WorkOrder> {
        return try {
            if (BuildConfig.USE_MOCK_API) {
                val wo = mockDataProvider.getWorkOrderDetail(id)
                    ?: return Result.failure(NoSuchElementException("Work order not found: $id"))
                workOrderDao.insert(wo.toEntity())
                taskDao.insertAll(wo.tasks.map { it.toEntity() })
                Result.success(wo)
            } else {
                val dto = api.getWorkOrderDetail(id)
                val wo = dto.toDomain()
                workOrderDao.insert(wo.toEntity())
                taskDao.insertAll(wo.tasks.map { it.toEntity() })
                Result.success(wo)
            }
        } catch (e: Exception) {
            val cached = workOrderDao.getById(id)
            if (cached != null) {
                Result.success(cached.toDomain())
            } else {
                Result.failure(e)
            }
        }
    }

    fun observeTasks(workOrderId: String): Flow<List<Task>> {
        return taskDao.observeByWorkOrder(workOrderId).map { entities ->
            entities.map { it.toDomain() }
        }
    }
}
