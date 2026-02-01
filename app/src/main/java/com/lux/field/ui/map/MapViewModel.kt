package com.lux.field.ui.map

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.lux.field.data.repository.AuthRepository
import com.lux.field.domain.model.WorkOrder
import com.lux.field.domain.usecase.GetAssignedWorkOrdersUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class MapUiState(
    val workOrders: List<WorkOrder> = emptyList(),
    val selectedWorkOrder: WorkOrder? = null,
    val isLoading: Boolean = false,
    val error: String? = null,
    val userName: String = "",
)

@HiltViewModel
class MapViewModel @Inject constructor(
    private val getAssignedWorkOrdersUseCase: GetAssignedWorkOrdersUseCase,
    private val authRepository: AuthRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(MapUiState())
    val uiState: StateFlow<MapUiState> = _uiState.asStateFlow()

    init {
        _uiState.update { it.copy(userName = authRepository.getUserName()) }
        observeWorkOrders()
        refresh()
    }

    private fun observeWorkOrders() {
        viewModelScope.launch {
            getAssignedWorkOrdersUseCase.observe().collect { workOrders ->
                _uiState.update { it.copy(workOrders = workOrders) }
            }
        }
    }

    fun refresh() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            val result = getAssignedWorkOrdersUseCase.refresh()
            result.fold(
                onSuccess = {
                    _uiState.update { it.copy(isLoading = false) }
                },
                onFailure = { e ->
                    _uiState.update { it.copy(isLoading = false, error = e.message) }
                },
            )
        }
    }

    fun selectWorkOrder(workOrder: WorkOrder?) {
        _uiState.update { it.copy(selectedWorkOrder = workOrder) }
    }
}
