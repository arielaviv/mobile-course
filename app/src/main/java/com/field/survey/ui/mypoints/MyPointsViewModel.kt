package com.field.survey.ui.mypoints

import androidx.lifecycle.LiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.asLiveData
import androidx.lifecycle.viewModelScope
import com.field.survey.data.repository.AuthRepository
import com.field.survey.data.repository.DistributionPointRepository
import com.field.survey.domain.model.DistributionPoint
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class MyPointsViewModel @Inject constructor(
    private val repository: DistributionPointRepository,
    private val authRepository: AuthRepository,
) : ViewModel() {

    val posts: LiveData<List<DistributionPoint>> =
        repository.observeByUser(authRepository.getUserId()).asLiveData()

    fun deletePoint(dpId: String) {
        viewModelScope.launch {
            repository.delete(dpId)
        }
    }
}
