package com.field.survey.ui.dp

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.field.survey.data.repository.AuthRepository
import com.field.survey.data.repository.DistributionPointRepository
import com.field.survey.domain.model.DistributionPoint
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class MyDpsUiState(
    val points: List<DistributionPoint> = emptyList(),
    val isLoading: Boolean = true,
    val deleteTarget: DistributionPoint? = null,
)

@HiltViewModel
class MyDpsViewModel @Inject constructor(
    private val repository: DistributionPointRepository,
    private val authRepository: AuthRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(MyDpsUiState())
    val uiState: StateFlow<MyDpsUiState> = _uiState.asStateFlow()

    init {
        observeMyPoints()
    }

    private fun observeMyPoints() {
        viewModelScope.launch {
            val userId = authRepository.getUserId()
            repository.observeByUser(userId).collect { points ->
                _uiState.update { it.copy(points = points, isLoading = false) }
            }
        }
    }

    fun requestDelete(dp: DistributionPoint) {
        _uiState.update { it.copy(deleteTarget = dp) }
    }

    fun dismissDelete() {
        _uiState.update { it.copy(deleteTarget = null) }
    }

    fun confirmDelete() {
        val dp = _uiState.value.deleteTarget ?: return
        _uiState.update { it.copy(deleteTarget = null) }
        viewModelScope.launch {
            repository.delete(dp.id)
        }
    }
}
