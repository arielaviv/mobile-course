package com.field.survey.ui.settings

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.field.survey.data.repository.AuthRepository
import com.field.survey.data.repository.MapSettingsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

data class SettingsState(
    val lightPreset: String,
    val roadLabels: Boolean,
    val poiLabels: Boolean,
    val placeLabels: Boolean,
    val objects3d: Boolean,
)

@HiltViewModel
class SettingsViewModel
    @Inject
    constructor(
        private val authRepository: AuthRepository,
        private val mapSettings: MapSettingsRepository,
    ) : ViewModel() {

        private val initial =
            SettingsState(
                lightPreset = mapSettings.getLightPreset(),
                roadLabels = mapSettings.getBool(
                    MapSettingsRepository.KEY_ROAD_LABELS,
                    MapSettingsRepository.DEFAULT_ROAD_LABELS,
                ),
                poiLabels = mapSettings.getBool(
                    MapSettingsRepository.KEY_POI_LABELS,
                    MapSettingsRepository.DEFAULT_POI_LABELS,
                ),
                placeLabels = mapSettings.getBool(
                    MapSettingsRepository.KEY_PLACE_LABELS,
                    MapSettingsRepository.DEFAULT_PLACE_LABELS,
                ),
                objects3d = mapSettings.getBool(
                    MapSettingsRepository.KEY_3D_OBJECTS,
                    MapSettingsRepository.DEFAULT_3D_OBJECTS,
                ),
            )

        private val _state = MutableLiveData(initial)
        val state: LiveData<SettingsState> = _state

        fun setLightPreset(preset: String) {
            mapSettings.setLightPreset(preset)
            update { it.copy(lightPreset = preset) }
        }

        fun setRoadLabels(v: Boolean) = persistBool(MapSettingsRepository.KEY_ROAD_LABELS, v) {
            it.copy(roadLabels = v)
        }

        fun setPoiLabels(v: Boolean) = persistBool(MapSettingsRepository.KEY_POI_LABELS, v) {
            it.copy(poiLabels = v)
        }

        fun setPlaceLabels(v: Boolean) = persistBool(MapSettingsRepository.KEY_PLACE_LABELS, v) {
            it.copy(placeLabels = v)
        }

        fun setObjects3d(v: Boolean) = persistBool(MapSettingsRepository.KEY_3D_OBJECTS, v) {
            it.copy(objects3d = v)
        }

        private fun persistBool(
            key: String,
            value: Boolean,
            transform: (SettingsState) -> SettingsState,
        ) {
            mapSettings.setBool(key, value)
            update(transform)
        }

        private fun update(transform: (SettingsState) -> SettingsState) {
            val current = _state.value ?: return
            val next = transform(current)
            if (next != current) _state.value = next
        }

        fun logout() {
            authRepository.logout()
        }
    }
