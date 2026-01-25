package com.lux.field.domain.usecase

import com.lux.field.data.repository.WorkOrderRepository
import com.lux.field.domain.model.WorkOrder
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class GetAssignedWorkOrdersUseCase @Inject constructor(
    private val workOrderRepository: WorkOrderRepository,
) {
    fun observe(): Flow<List<WorkOrder>> = workOrderRepository.observeWorkOrders()

    suspend fun refresh(): Result<List<WorkOrder>> = workOrderRepository.refreshWorkOrders()
}
