package com.field.survey.ui.map

import com.mapbox.geojson.Point
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

sealed class MapEvent {
    data class FlyTo(val point: Point, val zoom: Double = 17.0) : MapEvent()

    data class ShowPoint(val pointId: String) : MapEvent()
}

@Singleton
class MapEventsBus
    @Inject
    constructor() {
        private val _events =
            MutableSharedFlow<MapEvent>(
                extraBufferCapacity = 4,
                onBufferOverflow = BufferOverflow.DROP_OLDEST,
            )
        val events: SharedFlow<MapEvent> = _events.asSharedFlow()

        private val _mapCenter = MutableStateFlow<Point?>(null)
        val mapCenter: StateFlow<Point?> = _mapCenter.asStateFlow()

        suspend fun emit(event: MapEvent) = _events.emit(event)

        fun tryEmit(event: MapEvent) {
            _events.tryEmit(event)
        }

        fun updateMapCenter(point: Point) {
            _mapCenter.value = point
        }
    }
