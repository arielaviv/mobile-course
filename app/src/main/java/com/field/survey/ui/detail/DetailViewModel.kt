package com.field.survey.ui.detail

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.asLiveData
import androidx.lifecycle.viewModelScope
import com.field.survey.data.repository.AuthRepository
import com.field.survey.data.repository.CommentRepository
import com.field.survey.data.repository.DistributionPointRepository
import com.field.survey.data.repository.GeocodingRepository
import com.field.survey.domain.model.Comment
import com.field.survey.domain.model.DistributionPoint
import com.field.survey.domain.model.LocationAddress
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import java.util.UUID
import javax.inject.Inject

@HiltViewModel
class DetailViewModel
    @Inject
    constructor(
        savedStateHandle: SavedStateHandle,
        private val pointRepository: DistributionPointRepository,
        private val commentRepository: CommentRepository,
        private val authRepository: AuthRepository,
        private val geocodingRepository: GeocodingRepository,
    ) : ViewModel() {

        private val pointId: String = savedStateHandle["pointId"] ?: ""

        private val _point = MutableLiveData<DistributionPoint?>()
        val point: LiveData<DistributionPoint?> = _point

        private val _address = MutableLiveData<LocationAddress?>()
        val address: LiveData<LocationAddress?> = _address

        val comments: LiveData<List<Comment>> =
            commentRepository.observeComments(pointId).asLiveData()

        private val _isLoading = MutableLiveData(true)
        val isLoading: LiveData<Boolean> = _isLoading

        init {
            loadPoint()
            syncComments()
        }

        private fun loadPoint() {
            viewModelScope.launch {
                _isLoading.value = true
                val dp = pointRepository.getById(pointId)
                _point.value = dp
                _isLoading.value = false
                if (dp != null && !dp.type.isLine) {
                    fetchAddress(dp.latitude, dp.longitude)
                }
            }
        }

        private fun fetchAddress(
            lat: Double,
            lon: Double,
        ) {
            viewModelScope.launch {
                val result = geocodingRepository.reverseGeocode(lat, lon)
                _address.value = result.getOrNull()
            }
        }

        private fun syncComments() {
            viewModelScope.launch {
                commentRepository.syncComments(pointId)
            }
        }

        fun addComment(text: String) {
            if (text.isBlank()) return
            viewModelScope.launch {
                val comment =
                    Comment(
                        id = UUID.randomUUID().toString(),
                        postId = pointId,
                        userId = authRepository.getUserId(),
                        userName = authRepository.getUserName(),
                        text = text.trim(),
                        createdAt = System.currentTimeMillis(),
                    )
                commentRepository.addComment(comment)
            }
        }
    }
