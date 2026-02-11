package com.lux.field.ui.settings

import android.content.Context
import androidx.lifecycle.ViewModel
import com.lux.field.data.repository.AuthRepository
import com.lux.field.data.repository.MapStyle
import com.lux.field.data.repository.PreferencesRepository
import com.lux.field.service.LocationTrackingService
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.StateFlow
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val preferencesRepository: PreferencesRepository,
    private val authRepository: AuthRepository,
    @ApplicationContext private val context: Context,
) : ViewModel() {

    val mapStyle: StateFlow<MapStyle> = preferencesRepository.mapStyle

    fun setMapStyle(style: MapStyle) {
        preferencesRepository.setMapStyle(style)
    }

    fun logout() {
        LocationTrackingService.stop(context)
        authRepository.logout()
    }
}
