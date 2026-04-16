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

        fun getLanguage(): String = prefs.getString(KEY_LANGUAGE, DEFAULT_LANGUAGE) ?: DEFAULT_LANGUAGE

        fun setLanguage(language: String) {
            prefs.edit().putString(KEY_LANGUAGE, language).apply()
        }

        fun getDistanceUnits(): String = prefs.getString(KEY_UNITS, DEFAULT_UNITS) ?: DEFAULT_UNITS

        fun setDistanceUnits(units: String) {
            prefs.edit().putString(KEY_UNITS, units).apply()
        }

        fun isImperial(): Boolean = getDistanceUnits() == UNITS_IMPERIAL

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

            const val LANGUAGE_EN = "en"
            const val LANGUAGE_HE = "he"

            const val UNITS_METRIC = "metric"
            const val UNITS_IMPERIAL = "imperial"

            const val KEY_ROAD_LABELS = "show_road_labels"
            const val KEY_POI_LABELS = "show_poi_labels"
            const val KEY_PLACE_LABELS = "show_place_labels"
            const val KEY_3D_OBJECTS = "show_3d_objects"

            const val KEY_SATELLITE = "map_satellite"
            const val KEY_PITCH_3D = "map_pitch_3d"
            const val KEY_TERRAIN = "map_terrain"

            const val DEFAULT_ROAD_LABELS = true
            const val DEFAULT_POI_LABELS = false
            const val DEFAULT_PLACE_LABELS = true
            const val DEFAULT_3D_OBJECTS = true

            const val DEFAULT_SATELLITE = false
            const val DEFAULT_PITCH_3D = true
            const val DEFAULT_TERRAIN = false

            private const val PREFS_NAME = "map_settings"
            private const val KEY_LIGHT_PRESET = "light_preset"
            private const val KEY_LANGUAGE = "map_language"
            private const val KEY_UNITS = "distance_units"
            private const val DEFAULT_PRESET = PRESET_DAY
            private const val DEFAULT_LANGUAGE = LANGUAGE_EN
            private const val DEFAULT_UNITS = UNITS_METRIC
        }
    }
