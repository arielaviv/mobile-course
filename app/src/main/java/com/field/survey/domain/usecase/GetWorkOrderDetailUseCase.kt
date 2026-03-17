package com.field.survey.domain.usecase

import com.field.survey.data.repository.WorkOrderRepository
import com.field.survey.domain.model.Task
import com.field.survey.domain.model.WorkOrder
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
