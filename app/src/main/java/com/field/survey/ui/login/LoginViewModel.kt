package com.field.survey.ui.login

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.field.survey.data.repository.AuthRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class LoginViewModel @Inject constructor(
    private val authRepository: AuthRepository,
) : ViewModel() {

    private val _isLoading = MutableLiveData(false)
    val isLoading: LiveData<Boolean> = _isLoading

    private val _error = MutableLiveData<String?>(null)
    val error: LiveData<String?> = _error

    private val _loginSuccess = MutableLiveData(false)
    val loginSuccess: LiveData<Boolean> = _loginSuccess

    init {
        if (authRepository.isLoggedIn()) {
            _loginSuccess.value = true
        }
    }

    fun login(email: String, password: String) {
        if (email.isBlank() || password.isBlank()) {
            _error.value = "Email and password are required"
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
                onFailure = { e ->
                    _isLoading.value = false
                    _error.value = e.message ?: "Login failed"
                },
            )
        }
    }
}
