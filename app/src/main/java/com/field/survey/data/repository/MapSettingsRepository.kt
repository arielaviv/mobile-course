package com.field.survey.data.repository

import android.content.Context
import android.content.SharedPreferences
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class MapSettingsRepository
    @Inject
    constructor(
        @ApplicationContext context: Context,
    ) {
        private val prefs: SharedPreferences =
            context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

        fun getLightPreset(): String = prefs.getString(KEY_LIGHT_PRESET, DEFAULT_PRESET) ?: DEFAULT_PRESET

        fun setLightPreset(preset: String) {
            prefs.edit().putString(KEY_LIGHT_PRESET, preset).apply()
        }

        fun getBool(
            key: String,
            default: Boolean,
        ): Boolean = prefs.getBoolean(key, default)

        fun setBool(
            key: String,
            value: Boolean,
        ) {
            prefs.edit().putBoolean(key, value).apply()
        }

        companion object {
            const val PRESET_DAWN = "dawn"
            const val PRESET_DAY = "day"
            const val PRESET_DUSK = "dusk"
            const val PRESET_NIGHT = "night"
            val ALL_PRESETS = listOf(PRESET_DAWN, PRESET_DAY, PRESET_DUSK, PRESET_NIGHT)

            const val KEY_ROAD_LABELS = "show_road_labels"
            const val KEY_POI_LABELS = "show_poi_labels"
            const val KEY_PLACE_LABELS = "show_place_labels"
            const val KEY_3D_OBJECTS = "show_3d_objects"

            const val DEFAULT_ROAD_LABELS = true
            const val DEFAULT_POI_LABELS = false
            const val DEFAULT_PLACE_LABELS = true
            const val DEFAULT_3D_OBJECTS = true

            private const val PREFS_NAME = "map_settings"
            private const val KEY_LIGHT_PRESET = "light_preset"
            private const val DEFAULT_PRESET = PRESET_DAY
        }
    }
