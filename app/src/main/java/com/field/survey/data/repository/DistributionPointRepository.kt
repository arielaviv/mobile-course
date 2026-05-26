package com.field.survey.data.repository

import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingData
import androidx.paging.map
import com.field.survey.data.local.dao.DistributionPointDao
import com.field.survey.data.local.entity.DistributionPointEntity
import com.field.survey.domain.model.DistributionPoint
import com.field.survey.domain.model.toDpType
import android.net.Uri
import com.field.survey.ui.util.ImageCompression
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.Query
import com.google.firebase.storage.FirebaseStorage
import java.io.File
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DistributionPointRepository
    @Inject
    constructor(
        private val dao: DistributionPointDao,
        private val authRepository: AuthRepository,
    ) {
        private val firestore = FirebaseFirestore.getInstance()
        private val storage = FirebaseStorage.getInstance()
        private val collection = firestore.collection("distribution_points")
        private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
        private var listenerRegistration: ListenerRegistration? = null

        fun startRealtimeSync() {
            if (listenerRegistration != null) return
            listenerRegistration = collection
                .orderBy("createdAt", Query.Direction.DESCENDING)
                .addSnapshotListener { snapshot, error ->
                    if (error != null || snapshot == null) return@addSnapshotListener
                    val dps = snapshot.documents.mapNotNull { doc ->
                        try {
                            DistributionPointEntity(
                                id = doc.id,
                                label = doc.getString("label") ?: "",
                                type = (doc.getString("type") ?: "POLES").toDpType().name,
                                latitude = doc.getDouble("latitude") ?: 0.0,
                                longitude = doc.getDouble("longitude") ?: 0.0,
                                pathCoordinates = doc.getString("pathCoordinates"),
                                photoPath = null,
                                imageBase64 = doc.getString("imageBase64"),
                                photoUrl = doc.getString("photoUrl"),
                                notes = doc.getString("notes") ?: "",
                                createdAt = doc.getLong("createdAt") ?: 0L,
                                createdBy = doc.getString("createdBy") ?: "",
                                createdByName = doc.getString("createdByName") ?: "",
                                updatedAt = doc.getLong("updatedAt"),
                                updatedBy = doc.getString("updatedBy"),
                            )
                        } catch (_: Exception) {
                            null
                        }
                    }
                    scope.launch {
                        dao.deleteAll()
                        dao.insertAll(dps)
                    }
                }
        }

        fun stopRealtimeSync() {
            listenerRegistration?.remove()
            listenerRegistration = null
        }

        fun observeAll(): Flow<List<DistributionPoint>> = dao.observeAll().map { entities -> entities.map { it.toDomain() } }

        fun pagedPosts(): Flow<PagingData<DistributionPoint>> =
            Pager(
                config = PagingConfig(pageSize = 10, enablePlaceholders = false, initialLoadSize = 15),
                pagingSourceFactory = { dao.pagedAll() },
            ).flow.map { pagingData -> pagingData.map { it.toDomain() } }

        fun observeByUser(userId: String): Flow<List<DistributionPoint>> = dao.observeByUser(userId).map { entities -> entities.map { it.toDomain() } }

        fun observeRecent(limit: Int): Flow<List<DistributionPoint>> = dao.observeRecent(limit).map { entities -> entities.map { it.toDomain() } }

        suspend fun getById(id: String): DistributionPoint? = dao.getById(id)?.toDomain()

        suspend fun syncFromFirestore() {
            try {
                val snapshot =
                    collection
                        .orderBy("createdAt", Query.Direction.DESCENDING)
                        .get()
                        .await()
                val dps =
                    snapshot.documents.mapNotNull { doc ->
                        try {
                            DistributionPointEntity(
                                id = doc.id,
                                label = doc.getString("label") ?: "",
                                type = (doc.getString("type") ?: "POLES").toDpType().name,
                                latitude = doc.getDouble("latitude") ?: 0.0,
                                longitude = doc.getDouble("longitude") ?: 0.0,
                                pathCoordinates = doc.getString("pathCoordinates"),
                                photoPath = null,
                                imageBase64 = doc.getString("imageBase64"),
                                photoUrl = doc.getString("photoUrl"),
                                notes = doc.getString("notes") ?: "",
                                createdAt = doc.getLong("createdAt") ?: 0L,
                                createdBy = doc.getString("createdBy") ?: "",
                                createdByName = doc.getString("createdByName") ?: "",
                                updatedAt = doc.getLong("updatedAt"),
                                updatedBy = doc.getString("updatedBy"),
                            )
                        } catch (e: Exception) {
                            null
                        }
                    }
                dao.deleteAll()
                dao.insertAll(dps)
            } catch (_: Exception) {
                // Offline — rely on Room cache
            }
        }

        suspend fun save(dp: DistributionPoint) {
            val photoUrl = dp.photoPath?.let { uploadImage(dp.createdBy, dp.id, it) }
            val imageBase64 = if (photoUrl == null) dp.photoPath?.let { ImageCompression.fileToCompressedBase64(it) } else null

            val dpWithImage = dp.copy(photoUrl = photoUrl, imageBase64 = imageBase64)

            dao.insert(DistributionPointEntity.fromDomain(dpWithImage))

            val firestoreData =
                mapOf(
                    "label" to dpWithImage.label,
                    "type" to dpWithImage.type.name,
                    "latitude" to dpWithImage.latitude,
                    "longitude" to dpWithImage.longitude,
                    "pathCoordinates" to dpWithImage.pathCoordinates,
                    "photoUrl" to dpWithImage.photoUrl,
                    "imageBase64" to dpWithImage.imageBase64,
                    "notes" to dpWithImage.notes,
                    "createdAt" to dpWithImage.createdAt,
                    "createdBy" to dpWithImage.createdBy,
                    "createdByName" to dpWithImage.createdByName,
                    "updatedAt" to dpWithImage.updatedAt,
                    "updatedBy" to dpWithImage.updatedBy,
                )
            try {
                collection.document(dpWithImage.id).set(firestoreData).await()
            } catch (_: Exception) {
                // Offline — saved locally, will sync later
            }
        }

        suspend fun delete(dpId: String) {
            dao.deleteById(dpId)
            try {
                collection.document(dpId).delete().await()
            } catch (_: Exception) {
                // Offline
            }
        }

        suspend fun update(dp: DistributionPoint) {
            val userId = authRepository.getUserId()
            val photoUrl = dp.photoPath?.let { uploadImage(userId, dp.id, it) } ?: dp.photoUrl
            val imageBase64 = when {
                photoUrl != null -> null
                dp.photoPath != null -> ImageCompression.fileToCompressedBase64(dp.photoPath)
                else -> dp.imageBase64
            }

            val updated =
                dp.copy(
                    photoUrl = photoUrl,
                    imageBase64 = imageBase64,
                    updatedAt = System.currentTimeMillis(),
                    updatedBy = authRepository.getUserName().ifBlank { userId },
                )
            dao.insert(DistributionPointEntity.fromDomain(updated))

            val firestoreData =
                mapOf(
                    "label" to updated.label,
                    "type" to updated.type.name,
                    "latitude" to updated.latitude,
                    "longitude" to updated.longitude,
                    "pathCoordinates" to updated.pathCoordinates,
                    "photoUrl" to updated.photoUrl,
                    "imageBase64" to updated.imageBase64,
                    "notes" to updated.notes,
                    "createdAt" to updated.createdAt,
                    "createdBy" to updated.createdBy,
                    "createdByName" to updated.createdByName,
                    "updatedAt" to updated.updatedAt,
                    "updatedBy" to updated.updatedBy,
                )
            try {
                collection.document(updated.id).set(firestoreData).await()
            } catch (_: Exception) {
                // Offline
            }
        }

        private suspend fun uploadImage(userId: String, dpId: String, photoPath: String): String? {
            return try {
                val file = File(photoPath)
                if (!file.exists()) return null
                val ref = storage.reference.child("dps/$userId/$dpId.jpg")
                ref.putFile(Uri.fromFile(file)).await()
                ref.downloadUrl.await().toString()
            } catch (_: Exception) {
                null
            }
        }
    }
