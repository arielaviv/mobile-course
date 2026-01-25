package com.lux.field.domain.usecase

import com.lux.field.data.repository.WorkOrderRepository
import com.lux.field.domain.model.Task
import com.lux.field.domain.model.WorkOrder
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class GetWorkOrderDetailUseCase @Inject constructor(
    private val workOrderRepository: WorkOrderRepository,
) {
    suspend fun getDetail(id: String): Result<WorkOrder> =
        workOrderRepository.getWorkOrderDetail(id)

    fun observeTasks(workOrderId: String): Flow<List<Task>> =
        workOrderRepository.observeTasks(workOrderId)
}
