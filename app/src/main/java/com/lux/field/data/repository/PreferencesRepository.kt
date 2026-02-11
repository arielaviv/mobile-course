package com.lux.field.data.repository

import android.content.Context
import android.content.SharedPreferences
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

enum class MapStyle(val label: String, val styleUri: String?) {
    DEFAULT("Default", null),
    LUX_DARK("Lux Dark", "mapbox://styles/arielaviv/cmkbau2pn004a01r1ftd1bfi2"),
}

@Singleton
class PreferencesRepository @Inject constructor(
    @ApplicationContext context: Context,
) {
    private val prefs: SharedPreferences =
        context.getSharedPreferences("lux_prefs", Context.MODE_PRIVATE)

    private val _mapStyle = MutableStateFlow(loadMapStyle())
    val mapStyle: StateFlow<MapStyle> = _mapStyle.asStateFlow()

    private val _autoSpeak = MutableStateFlow(prefs.getBoolean(KEY_AUTO_SPEAK, false))
    val autoSpeak: StateFlow<Boolean> = _autoSpeak.asStateFlow()

    fun setMapStyle(style: MapStyle) {
        prefs.edit().putString(KEY_MAP_STYLE, style.name).apply()
        _mapStyle.value = style
    }

    fun setAutoSpeak(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_AUTO_SPEAK, enabled).apply()
        _autoSpeak.value = enabled
    }

    private fun loadMapStyle(): MapStyle {
        val name = prefs.getString(KEY_MAP_STYLE, MapStyle.DEFAULT.name)
        return MapStyle.entries.find { it.name == name } ?: MapStyle.DEFAULT
    }

    companion object {
        private const val KEY_MAP_STYLE = "map_style"
        private const val KEY_AUTO_SPEAK = "auto_speak"
    }
}
