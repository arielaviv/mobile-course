package com.field.survey.ui.mypoints

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.asLiveData
import androidx.lifecycle.viewModelScope
import com.field.survey.data.repository.AuthRepository
import com.field.survey.data.repository.DistributionPointRepository
import com.field.survey.domain.model.DistributionPoint
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class MyPointsViewModel
    @Inject
    constructor(
        private val repository: DistributionPointRepository,
        private val authRepository: AuthRepository,
    ) : ViewModel() {

        private val _isLoading = MutableLiveData(true)
        val isLoading: LiveData<Boolean> = _isLoading

        val posts: LiveData<List<DistributionPoint>> =
            repository.observeByUser(authRepository.getUserId())
                .onEach { _isLoading.postValue(false) }
                .asLiveData()

        fun deletePoint(dpId: String) {
            viewModelScope.launch {
                repository.delete(dpId)
            }
        }
    }
