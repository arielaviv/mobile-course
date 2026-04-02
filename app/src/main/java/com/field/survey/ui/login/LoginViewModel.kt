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

        private fun mapAuthError(e: Throwable): Int {
            val msg = e.message ?: ""
            return when {
                "password" in msg.lowercase() -> R.string.error_wrong_password
                "no user record" in msg.lowercase() || "user not found" in msg.lowercase() -> R.string.error_user_not_found
                "email address is badly formatted" in msg.lowercase() -> R.string.error_invalid_email
                "network" in msg.lowercase() -> R.string.error_network
                "credential is incorrect" in msg.lowercase() || "credential is malformed" in msg.lowercase() -> R.string.error_wrong_password
                else -> R.string.error_generic
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
                        _error.value = mapAuthError(it)
                    },
                )
            }
        }
    }
