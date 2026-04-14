package com.field.survey.data.repository

import com.field.survey.data.local.dao.DistributionPointDao
import com.field.survey.data.local.entity.DistributionPointEntity
import com.field.survey.domain.model.DistributionPoint
import com.field.survey.domain.model.toDpType
import com.field.survey.ui.util.ImageCompression
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
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
        private val collection = firestore.collection("distribution_points")

        fun observeAll(): Flow<List<DistributionPoint>> = dao.observeAll().map { entities -> entities.map { it.toDomain() } }

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
                dps.forEach { dao.insert(it) }
            } catch (_: Exception) {
                // Offline — rely on Room cache
            }
        }

        suspend fun save(dp: DistributionPoint) {
            val imageBase64 = dp.photoPath?.let { ImageCompression.fileToCompressedBase64(it) }

            val dpWithImage = dp.copy(imageBase64 = imageBase64)

            dao.insert(DistributionPointEntity.fromDomain(dpWithImage))

            val firestoreData =
                mapOf(
                    "label" to dpWithImage.label,
                    "type" to dpWithImage.type.name,
                    "latitude" to dpWithImage.latitude,
                    "longitude" to dpWithImage.longitude,
                    "pathCoordinates" to dpWithImage.pathCoordinates,
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
            val imageBase64 =
                dp.photoPath?.let { ImageCompression.fileToCompressedBase64(it) } ?: dp.imageBase64

            val updated =
                dp.copy(
                    imageBase64 = imageBase64,
                    updatedAt = System.currentTimeMillis(),
                    updatedBy = authRepository.getUserName().ifBlank { authRepository.getUserId() },
                )
            dao.insert(DistributionPointEntity.fromDomain(updated))

            val firestoreData =
                mapOf(
                    "label" to updated.label,
                    "type" to updated.type.name,
                    "latitude" to updated.latitude,
                    "longitude" to updated.longitude,
                    "pathCoordinates" to updated.pathCoordinates,
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
    }
