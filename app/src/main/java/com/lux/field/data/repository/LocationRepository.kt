package com.lux.field.data.repository

import android.util.Log
import com.lux.field.BuildConfig
import com.lux.field.data.connectivity.ConnectivityObserver
import com.lux.field.data.local.dao.LocationPointDao
import com.lux.field.data.local.entity.LocationPointEntity
import com.lux.field.data.remote.LuxApi
import com.lux.field.data.remote.dto.LocationBatchRequest
import com.lux.field.data.remote.dto.LocationPointDto
import com.lux.field.domain.model.LocationPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class LocationRepository @Inject constructor(
    private val locationPointDao: LocationPointDao,
    private val api: LuxApi,
    private val tokenProvider: TokenProvider,
    private val connectivityObserver: ConnectivityObserver,
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    val latestLocation: StateFlow<LocationPoint?> = locationPointDao.observeLatest()
        .map { entity -> entity?.toDomain() }
        .stateIn(scope, SharingStarted.Eagerly, null)

    init {
        startPeriodicFlush()
        startPeriodicPrune()
        flushOnReconnect()
    }

    suspend fun saveLocation(
        latitude: Double,
        longitude: Double,
        accuracy: Float,
        speed: Float,
        bearing: Float,
        altitude: Double,
        timestamp: Long,
    ) {
        locationPointDao.insert(
            LocationPointEntity(
                latitude = latitude,
                longitude = longitude,
                accuracy = accuracy,
                speed = speed,
                bearing = bearing,
                altitude = altitude,
                timestamp = timestamp,
            )
        )
        val unsyncedCount = locationPointDao.getUnsyncedCount()
        if (unsyncedCount >= FLUSH_THRESHOLD) {
            flushToServer()
        }
    }

    private fun startPeriodicFlush() {
        scope.launch {
            while (true) {
                delay(FLUSH_INTERVAL_MS)
                if (connectivityObserver.isOnline.value) {
                    flushToServer()
                }
            }
        }
    }

    private fun flushOnReconnect() {
        scope.launch {
            connectivityObserver.isOnline.collect { online ->
                if (online) flushToServer()
            }
        }
    }

    private fun startPeriodicPrune() {
        scope.launch {
            while (true) {
                delay(PRUNE_INTERVAL_MS)
                val cutoff = System.currentTimeMillis() - RETENTION_MS
                locationPointDao.deleteOlderThan(cutoff)
                locationPointDao.deleteSynced()
            }
        }
    }

    private suspend fun flushToServer() {
        if (BuildConfig.USE_MOCK_API) return
        try {
            val unsynced = locationPointDao.getUnsynced(BATCH_SIZE)
            if (unsynced.isEmpty()) return

            val crewId = tokenProvider.getCrewId()
            val userId = tokenProvider.getUserId()
            if (crewId.isBlank() || userId.isBlank()) return

            val request = LocationBatchRequest(
                crewId = crewId,
                userId = userId,
                points = unsynced.map { it.toDto() },
            )
            val response = api.uploadLocationBatch(request)
            if (response.accepted > 0) {
                locationPointDao.markSynced(unsynced.map { it.id })
            }
        } catch (e: Exception) {
            Log.w(TAG, "Location batch upload failed, will retry", e)
        }
    }

    companion object {
        private const val TAG = "LocationRepository"
        private const val FLUSH_THRESHOLD = 20
        private const val FLUSH_INTERVAL_MS = 60_000L
        private const val PRUNE_INTERVAL_MS = 3_600_000L // 1 hour
        private const val RETENTION_MS = 86_400_000L // 24 hours
        private const val BATCH_SIZE = 50
    }
}

private fun LocationPointEntity.toDomain() = LocationPoint(
    latitude = latitude,
    longitude = longitude,
    accuracy = accuracy,
    speed = speed,
    bearing = bearing,
    altitude = altitude,
    timestamp = timestamp,
)

private fun LocationPointEntity.toDto() = LocationPointDto(
    lat = latitude,
    lng = longitude,
    accuracy = accuracy,
    speed = speed,
    bearing = bearing,
    altitude = altitude,
    ts = timestamp,
)
