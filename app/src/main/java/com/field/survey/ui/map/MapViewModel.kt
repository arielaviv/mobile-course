package com.field.survey.ui.map

import androidx.lifecycle.LiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.asLiveData
import androidx.lifecycle.viewModelScope
import com.field.survey.data.repository.DistributionPointRepository
import com.field.survey.data.repository.WeatherRepository
import com.field.survey.domain.model.DistributionPoint
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class MapViewModel @Inject constructor(
    private val repository: DistributionPointRepository,
    private val weatherRepository: WeatherRepository,
) : ViewModel() {

    val posts: LiveData<List<DistributionPoint>> =
        repository.observeAll().asLiveData()

    init {
        syncFromFirestore()
    }

    private fun syncFromFirestore() {
        viewModelScope.launch {
            repository.syncFromFirestore()
        }
    }
}
