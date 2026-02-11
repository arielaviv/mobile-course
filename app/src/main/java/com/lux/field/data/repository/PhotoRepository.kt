package com.lux.field.data.repository

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import com.lux.field.BuildConfig
import com.lux.field.data.local.dao.TaskPhotoDao
import com.lux.field.data.sync.OfflineSyncManager
import com.lux.field.data.sync.PhotoUploadPayload
import com.lux.field.domain.model.CameraFacing
import com.lux.field.domain.model.PhotoAnalysisStatus
import com.lux.field.domain.model.TaskPhoto
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.File
import java.io.FileOutputStream
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PhotoRepository @Inject constructor(
    @ApplicationContext private val context: Context,
    private val taskPhotoDao: TaskPhotoDao,
    private val offlineSyncManager: OfflineSyncManager,
    private val json: Json,
) {

    private val photosDir: File
        get() = File(context.filesDir, "photos").also { it.mkdirs() }

    private val thumbnailsDir: File
        get() = File(context.filesDir, "photos/thumbnails").also { it.mkdirs() }

    fun observePhotos(taskId: String): Flow<List<TaskPhoto>> =
        taskPhotoDao.observeByTask(taskId).map { entities ->
            entities.map { it.toDomain() }
        }

    suspend fun getPhoto(photoId: String): TaskPhoto? =
        taskPhotoDao.getById(photoId)?.toDomain()

    suspend fun savePhoto(
        sourceFile: File,
        taskId: String,
        stepId: String?,
        workOrderId: String,
        cameraFacing: CameraFacing,
        latitude: Double?,
        longitude: Double?,
    ): TaskPhoto = withContext(Dispatchers.IO) {
        val photoId = UUID.randomUUID().toString()
        val destFile = File(photosDir, "$photoId.jpg")
        sourceFile.copyTo(destFile, overwrite = true)

        val thumbnailFile = File(thumbnailsDir, "${photoId}_thumb.jpg")
        generateThumbnail(destFile, thumbnailFile)

        val photo = TaskPhoto(
            id = photoId,
            taskId = taskId,
            stepId = stepId,
            workOrderId = workOrderId,
            filePath = destFile.absolutePath,
            thumbnailPath = thumbnailFile.absolutePath,
            cameraFacing = cameraFacing,
            capturedAt = System.currentTimeMillis(),
            latitude = latitude,
            longitude = longitude,
            analysisStatus = PhotoAnalysisStatus.NONE,
            analysisResult = null,
        )

        taskPhotoDao.insert(photo.toEntity())

        // Queue photo upload to server
        if (!BuildConfig.USE_MOCK_API) {
            val payload = PhotoUploadPayload(
                photoId = photoId,
                taskId = taskId,
                workOrderId = workOrderId,
                filePath = destFile.absolutePath,
                cameraFacing = cameraFacing.name.lowercase(),
                capturedAt = photo.capturedAt,
                latitude = latitude,
                longitude = longitude,
            )
            offlineSyncManager.enqueue(
                type = OfflineSyncManager.TYPE_PHOTO_UPLOAD,
                payloadJson = json.encodeToString(payload),
            )
        }

        photo
    }

    suspend fun updateAnalysis(
        photoId: String,
        status: PhotoAnalysisStatus,
        result: String?,
    ) {
        taskPhotoDao.updateAnalysis(photoId, status.name.lowercase(), result)
    }

    suspend fun deletePhoto(photoId: String) {
        val entity = taskPhotoDao.getById(photoId) ?: return
        File(entity.filePath).delete()
        entity.thumbnailPath?.let { File(it).delete() }
        taskPhotoDao.delete(photoId)
    }

    private fun generateThumbnail(source: File, dest: File) {
        val options = BitmapFactory.Options().apply {
            inJustDecodeBounds = true
        }
        BitmapFactory.decodeFile(source.absolutePath, options)

        val targetSize = 200
        val scaleFactor = maxOf(
            options.outWidth / targetSize,
            options.outHeight / targetSize,
            1,
        )

        val decodeOptions = BitmapFactory.Options().apply {
            inSampleSize = scaleFactor
        }
        val bitmap = BitmapFactory.decodeFile(source.absolutePath, decodeOptions) ?: return

        val scaled = Bitmap.createScaledBitmap(bitmap, targetSize, targetSize, true)
        FileOutputStream(dest).use { out ->
            scaled.compress(Bitmap.CompressFormat.JPEG, 80, out)
        }
        if (scaled !== bitmap) scaled.recycle()
        bitmap.recycle()
    }
}
