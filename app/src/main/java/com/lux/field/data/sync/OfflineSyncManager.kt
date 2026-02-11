package com.lux.field.data.sync

import android.util.Log
import com.lux.field.BuildConfig
import com.lux.field.data.connectivity.ConnectivityObserver
import com.lux.field.data.local.dao.SyncQueueDao
import com.lux.field.data.local.entity.SyncQueueEntity
import com.lux.field.data.remote.LuxApi
import com.lux.field.data.remote.dto.TaskUpdateRequest
import com.lux.field.domain.model.TaskStatus
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class OfflineSyncManager @Inject constructor(
    private val syncQueueDao: SyncQueueDao,
    private val api: LuxApi,
    private val connectivityObserver: ConnectivityObserver,
    private val json: Json,
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    init {
        observeConnectivity()
        startPeriodicSync()
    }

    suspend fun enqueue(type: String, payloadJson: String) {
        syncQueueDao.insert(
            SyncQueueEntity(
                type = type,
                payloadJson = payloadJson,
            )
        )
        // Try immediate sync if online
        if (connectivityObserver.isOnline.value) {
            processQueue()
        }
    }

    private fun observeConnectivity() {
        scope.launch {
            connectivityObserver.isOnline.collect { online ->
                if (online) processQueue()
            }
        }
    }

    private fun startPeriodicSync() {
        scope.launch {
            while (true) {
                delay(SYNC_INTERVAL_MS)
                if (connectivityObserver.isOnline.value) {
                    processQueue()
                }
            }
        }
    }

    private suspend fun processQueue() {
        if (BuildConfig.USE_MOCK_API) return

        val pending = syncQueueDao.getPending()
        if (pending.isEmpty()) return

        Log.d(TAG, "Processing ${pending.size} queued operations")

        for (entry in pending) {
            val success = processEntry(entry)
            if (success) {
                syncQueueDao.delete(entry.id)
            } else {
                syncQueueDao.incrementRetry(entry.id)
                // If we failed, back off before trying the next one
                val backoffMs = minOf(1000L * (1 shl entry.retryCount), MAX_BACKOFF_MS)
                delay(backoffMs)
            }
        }

        // Clean up exhausted entries periodically
        syncQueueDao.deleteExhausted()
    }

    private suspend fun processEntry(entry: SyncQueueEntity): Boolean {
        return try {
            when (entry.type) {
                TYPE_TASK_UPDATE -> processTaskUpdate(entry.payloadJson)
                TYPE_PHOTO_UPLOAD -> processPhotoUpload(entry.payloadJson)
                else -> {
                    Log.w(TAG, "Unknown sync operation: ${entry.type}")
                    true // Delete unknown entries
                }
            }
        } catch (e: Exception) {
            Log.w(TAG, "Sync failed for ${entry.type} (retry ${entry.retryCount})", e)
            false
        }
    }

    private suspend fun processTaskUpdate(payloadJson: String): Boolean {
        val payload = json.decodeFromString<TaskUpdatePayload>(payloadJson)
        api.updateTaskStatus(
            TaskUpdateRequest(
                workOrderId = payload.workOrderId,
                taskId = payload.taskId,
                status = TaskStatus.valueOf(payload.status.uppercase()),
                notes = payload.notes,
                timestamp = payload.timestamp,
            )
        )
        return true
    }

    private suspend fun processPhotoUpload(payloadJson: String): Boolean {
        val payload = json.decodeFromString<PhotoUploadPayload>(payloadJson)
        val file = File(payload.filePath)
        if (!file.exists()) {
            Log.w(TAG, "Photo file missing, skipping: ${payload.filePath}")
            return true // Delete entry since file is gone
        }

        val requestFile = file.asRequestBody("image/jpeg".toMediaTypeOrNull())
        val photoPart = MultipartBody.Part.createFormData("photo", file.name, requestFile)

        fun stringPart(value: String) = value.toRequestBody("text/plain".toMediaTypeOrNull())

        api.uploadPhoto(
            photo = photoPart,
            photoId = stringPart(payload.photoId),
            taskId = stringPart(payload.taskId),
            workOrderId = stringPart(payload.workOrderId),
            cameraFacing = stringPart(payload.cameraFacing),
            capturedAt = stringPart(payload.capturedAt.toString()),
            latitude = payload.latitude?.let { stringPart(it.toString()) },
            longitude = payload.longitude?.let { stringPart(it.toString()) },
        )
        return true
    }

    companion object {
        private const val TAG = "OfflineSyncManager"
        private const val SYNC_INTERVAL_MS = 90_000L
        private const val MAX_BACKOFF_MS = 60_000L
        const val TYPE_TASK_UPDATE = "task_update"
        const val TYPE_PHOTO_UPLOAD = "photo_upload"
    }
}
