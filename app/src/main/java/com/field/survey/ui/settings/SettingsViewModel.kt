package com.field.survey.ui.settings

import androidx.lifecycle.ViewModel
import com.field.survey.data.repository.AuthRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val authRepository: AuthRepository,
) : ViewModel() {

    fun logout() {
        authRepository.logout()
    }
}
