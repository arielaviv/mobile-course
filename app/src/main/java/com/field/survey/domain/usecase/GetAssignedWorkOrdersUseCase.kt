package com.field.survey.domain.usecase

import com.field.survey.data.repository.WorkOrderRepository
import com.field.survey.domain.model.WorkOrder
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class GetAssignedWorkOrdersUseCase @Inject constructor(
    private val workOrderRepository: WorkOrderRepository,
) {
    fun observe(): Flow<List<WorkOrder>> = workOrderRepository.observeWorkOrders()

    suspend fun refresh(): Result<List<WorkOrder>> = workOrderRepository.refreshWorkOrders()
}
