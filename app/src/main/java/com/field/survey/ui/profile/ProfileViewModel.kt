package com.field.survey.ui.profile

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.field.survey.R
import com.field.survey.data.repository.AuthRepository
import com.field.survey.data.repository.UserProfile
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ProfileViewModel
    @Inject
    constructor(
        private val authRepository: AuthRepository,
    ) : ViewModel() {

        sealed class SaveResult {
            data class Success(val stringRes: Int) : SaveResult()
            data class Error(val stringRes: Int) : SaveResult()
        }

        private val _profile = MutableLiveData<UserProfile?>()
        val profile: LiveData<UserProfile?> = _profile

        private val _email = MutableLiveData<String>()
        val email: LiveData<String> = _email

        private val _isSaving = MutableLiveData(false)
        val isSaving: LiveData<Boolean> = _isSaving

        private val _saveResult = MutableLiveData<SaveResult?>()
        val saveResult: LiveData<SaveResult?> = _saveResult

        private var newPhotoPath: String? = null

        init {
            _email.value = authRepository.getUserEmail()
            loadProfile()
        }

        private fun loadProfile() {
            viewModelScope.launch {
                val result = authRepository.getUserProfile()
                result.onSuccess { p ->
                    _profile.value = p
                }
            }
        }

        fun setPhotoFromPath(path: String) {
            newPhotoPath = path
        }

        fun save(name: String) {
            if (name.isBlank()) {
                _saveResult.value = SaveResult.Error(R.string.profile_name_empty)
                return
            }
            viewModelScope.launch {
                _isSaving.value = true
                val result = authRepository.updateProfile(name.trim(), newPhotoPath)
                result.fold(
                    onSuccess = {
                        _isSaving.value = false
                        _saveResult.value = SaveResult.Success(R.string.profile_updated)
                        loadProfile()
                    },
                    onFailure = {
                        _isSaving.value = false
                        _saveResult.value = SaveResult.Error(R.string.error_generic)
                    },
                )
            }
        }
    }
