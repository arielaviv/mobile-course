package com.field.survey.ui.feed

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.asLiveData
import androidx.lifecycle.viewModelScope
import androidx.paging.PagingData
import androidx.paging.cachedIn
import com.field.survey.data.repository.DistributionPointRepository
import com.field.survey.domain.model.DistributionPoint
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class FeedViewModel
    @Inject
    constructor(
        private val repository: DistributionPointRepository,
    ) : ViewModel() {

        val pagedPosts: Flow<PagingData<DistributionPoint>> =
            repository.pagedPosts().cachedIn(viewModelScope)

        val recentActivity: LiveData<List<DistributionPoint>> =
            repository.observeRecent(5).asLiveData()

        private val _isRefreshing = MutableLiveData(false)
        val isRefreshing: LiveData<Boolean> = _isRefreshing

        init {
            refresh()
        }

        fun refresh() {
            viewModelScope.launch {
                _isRefreshing.value = true
                repository.syncFromFirestore()
                _isRefreshing.value = false
            }
        }
    }
