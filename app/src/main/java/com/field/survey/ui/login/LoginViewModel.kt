package com.field.survey.ui.login

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.field.survey.R
import com.field.survey.data.repository.AuthRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class LoginViewModel
    @Inject
    constructor(
        private val authRepository: AuthRepository,
    ) : ViewModel() {

        private val _isLoading = MutableLiveData(false)
        val isLoading: LiveData<Boolean> = _isLoading

        private val _error = MutableLiveData<Int?>(null)
        val error: LiveData<Int?> = _error

        private val _loginSuccess = MutableLiveData(false)
        val loginSuccess: LiveData<Boolean> = _loginSuccess

        init {
            if (authRepository.isLoggedIn()) {
                _loginSuccess.value = true
            }
        }

        fun login(
            email: String,
            password: String,
        ) {
            if (email.isBlank() || password.isBlank()) {
                _error.value = R.string.login_credentials_required
                return
            }
            viewModelScope.launch {
                _isLoading.value = true
                _error.value = null
                val result = authRepository.login(email.trim(), password)
                result.fold(
                    onSuccess = {
                        _isLoading.value = false
                        _loginSuccess.value = true
                    },
                    onFailure = {
                        _isLoading.value = false
                        _error.value = R.string.error_generic
                    },
                )
            }
        }
    }
