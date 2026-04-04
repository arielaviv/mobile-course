package com.field.survey.ui.register

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
class RegisterViewModel
    @Inject
    constructor(
        private val authRepository: AuthRepository,
    ) : ViewModel() {

        private val _isLoading = MutableLiveData(false)
        val isLoading: LiveData<Boolean> = _isLoading

        private val _error = MutableLiveData<Int?>(null)
        val error: LiveData<Int?> = _error

        private val _registerSuccess = MutableLiveData(false)
        val registerSuccess: LiveData<Boolean> = _registerSuccess

        private fun mapAuthError(e: Throwable): Int {
            val msg = e.message ?: ""
            return when {
                "email address is already in use" in msg.lowercase() -> R.string.error_email_in_use
                "email address is badly formatted" in msg.lowercase() -> R.string.error_invalid_email
                "password is invalid" in msg.lowercase() || "weak" in msg.lowercase() -> R.string.error_weak_password
                "network" in msg.lowercase() -> R.string.error_network
                else -> R.string.error_generic
            }
        }

        fun register(
            name: String,
            email: String,
            password: String,
        ) {
            if (name.isBlank() || email.isBlank()) {
                _error.value = R.string.register_all_fields_required
                return
            }
            if (password.length < 6) {
                _error.value = R.string.register_password_too_short
                return
            }
            viewModelScope.launch {
                _isLoading.value = true
                _error.value = null
                val result = authRepository.register(name.trim(), email.trim(), password)
                result.fold(
                    onSuccess = {
                        _isLoading.value = false
                        _registerSuccess.value = true
                    },
                    onFailure = {
                        _isLoading.value = false
                        _error.value = mapAuthError(it)
                    },
                )
            }
        }
    }
