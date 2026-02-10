package com.lux.field.ui.settings

import androidx.lifecycle.ViewModel
import com.lux.field.data.repository.AuthRepository
import com.lux.field.data.repository.MapStyle
import com.lux.field.data.repository.PreferencesRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.StateFlow
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val preferencesRepository: PreferencesRepository,
    private val authRepository: AuthRepository,
) : ViewModel() {

    val mapStyle: StateFlow<MapStyle> = preferencesRepository.mapStyle

    fun setMapStyle(style: MapStyle) {
        preferencesRepository.setMapStyle(style)
    }

    fun logout() {
        authRepository.logout()
    }
}
