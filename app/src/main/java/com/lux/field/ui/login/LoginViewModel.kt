package com.lux.field.ui.login

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.lux.field.data.repository.AuthRepository
import com.lux.field.domain.usecase.LoginUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class LoginUiState(
    val phone: String = "",
    val code: String = "",
    val isLoading: Boolean = false,
    val error: String? = null,
    val isLoggedIn: Boolean = false,
)

@HiltViewModel
class LoginViewModel @Inject constructor(
    private val loginUseCase: LoginUseCase,
    private val authRepository: AuthRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(LoginUiState())
    val uiState: StateFlow<LoginUiState> = _uiState.asStateFlow()

    init {
        if (authRepository.isLoggedIn()) {
            _uiState.update { it.copy(isLoggedIn = true) }
        }
    }

    fun onPhoneChanged(phone: String) {
        _uiState.update { it.copy(phone = phone, error = null) }
    }

    fun onCodeChanged(code: String) {
        _uiState.update { it.copy(code = code, error = null) }
    }

    fun login() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            val result = loginUseCase(
                phone = _uiState.value.phone.trim(),
                code = _uiState.value.code.trim(),
            )
            result.fold(
                onSuccess = {
                    _uiState.update { it.copy(isLoading = false, isLoggedIn = true) }
                },
                onFailure = { e ->
                    _uiState.update { it.copy(isLoading = false, error = e.message ?: "Login failed") }
                },
            )
        }
    }
}
