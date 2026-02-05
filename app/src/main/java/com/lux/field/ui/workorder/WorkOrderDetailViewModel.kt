package com.lux.field.ui.workorder

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.lux.field.domain.model.Task
import com.lux.field.domain.model.WorkOrder
import com.lux.field.domain.usecase.GetWorkOrderDetailUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class WorkOrderDetailUiState(
    val workOrder: WorkOrder? = null,
    val tasks: List<Task> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null,
)

@HiltViewModel
class WorkOrderDetailViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val getWorkOrderDetailUseCase: GetWorkOrderDetailUseCase,
) : ViewModel() {

    private val workOrderId: String = savedStateHandle["workOrderId"] ?: ""

    private val _uiState = MutableStateFlow(WorkOrderDetailUiState())
    val uiState: StateFlow<WorkOrderDetailUiState> = _uiState.asStateFlow()

    init {
        loadDetail()
        observeTasks()
    }

    private fun loadDetail() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            val result = getWorkOrderDetailUseCase.getDetail(workOrderId)
            result.fold(
                onSuccess = { wo ->
                    _uiState.update {
                        it.copy(
                            workOrder = wo,
                            tasks = wo.tasks,
                            isLoading = false,
                        )
                    }
                },
                onFailure = { e ->
                    _uiState.update { it.copy(isLoading = false, error = e.message) }
                },
            )
        }
    }

    private fun observeTasks() {
        viewModelScope.launch {
            getWorkOrderDetailUseCase.observeTasks(workOrderId).collect { tasks ->
                _uiState.update { it.copy(tasks = tasks) }
            }
        }
    }

    fun refresh() {
        loadDetail()
    }
}
