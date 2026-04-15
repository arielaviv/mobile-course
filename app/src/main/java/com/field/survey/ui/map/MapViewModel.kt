package com.field.survey.ui.map

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.asLiveData
import androidx.lifecycle.viewModelScope
import com.field.survey.data.repository.DistributionPointRepository
import com.field.survey.domain.model.DistributionPoint
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class MapViewModel
    @Inject
    constructor(
        private val repository: DistributionPointRepository,
    ) : ViewModel() {

        val posts: LiveData<List<DistributionPoint>> =
            repository.observeAll().asLiveData()

        private val _isLoading = MutableLiveData(false)
        val isLoading: LiveData<Boolean> = _isLoading

        init {
            syncFromFirestore()
        }

        private fun syncFromFirestore() {
            viewModelScope.launch {
                _isLoading.value = true
                repository.syncFromFirestore()
                _isLoading.value = false
            }
        }
    }
