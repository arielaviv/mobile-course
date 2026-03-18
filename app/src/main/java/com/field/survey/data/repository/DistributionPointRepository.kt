package com.field.survey.data.repository

import android.util.Base64
import com.field.survey.data.local.dao.DistributionPointDao
import com.field.survey.data.local.entity.DistributionPointEntity
import com.field.survey.domain.model.DistributionPoint
import com.field.survey.domain.model.DpType
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DistributionPointRepository @Inject constructor(
    private val dao: DistributionPointDao,
) {
    private val firestore = FirebaseFirestore.getInstance()
    private val collection = firestore.collection("distribution_points")

    fun observeAll(): Flow<List<DistributionPoint>> =
        dao.observeAll().map { entities -> entities.map { it.toDomain() } }

    fun observeFromFirestore(): Flow<List<DistributionPoint>> = callbackFlow {
        val listener = collection
            .orderBy("createdAt", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }
                val dps = snapshot?.documents?.mapNotNull { doc ->
                    try {
                        DistributionPoint(
                            id = doc.id,
                            label = doc.getString("label") ?: "",
                            type = DpType.valueOf(doc.getString("type") ?: "DP"),
                            latitude = doc.getDouble("latitude") ?: 0.0,
                            longitude = doc.getDouble("longitude") ?: 0.0,
                            photoPath = null,
                            imageBase64 = doc.getString("imageBase64"),
                            notes = doc.getString("notes") ?: "",
                            createdAt = doc.getLong("createdAt") ?: 0L,
                            createdBy = doc.getString("createdBy") ?: "",
                            createdByName = doc.getString("createdByName") ?: "",
                        )
                    } catch (e: Exception) {
                        null
                    }
                } ?: emptyList()
                trySend(dps)
            }
        awaitClose { listener.remove() }
    }

    suspend fun syncFromFirestore() {
        try {
            val snapshot = collection
                .orderBy("createdAt", Query.Direction.DESCENDING)
                .get()
                .await()
            val dps = snapshot.documents.mapNotNull { doc ->
                try {
                    DistributionPointEntity(
                        id = doc.id,
                        label = doc.getString("label") ?: "",
                        type = doc.getString("type") ?: "DP",
                        latitude = doc.getDouble("latitude") ?: 0.0,
                        longitude = doc.getDouble("longitude") ?: 0.0,
                        photoPath = null,
                        imageBase64 = doc.getString("imageBase64"),
                        notes = doc.getString("notes") ?: "",
                        createdAt = doc.getLong("createdAt") ?: 0L,
                        createdBy = doc.getString("createdBy") ?: "",
                        createdByName = doc.getString("createdByName") ?: "",
                    )
                } catch (e: Exception) {
                    null
                }
            }
            dps.forEach { dao.insert(it) }
        } catch (_: Exception) {
            // Offline — rely on Room cache
        }
    }

    suspend fun save(dp: DistributionPoint) {
        val imageBase64 = dp.photoPath?.let { path ->
            try {
                val bytes = File(path).readBytes()
                Base64.encodeToString(bytes, Base64.NO_WRAP)
            } catch (_: Exception) {
                null
            }
        }

        val dpWithImage = dp.copy(imageBase64 = imageBase64)

        dao.insert(DistributionPointEntity.fromDomain(dpWithImage))

        val firestoreData = mapOf(
            "label" to dpWithImage.label,
            "type" to dpWithImage.type.name,
            "latitude" to dpWithImage.latitude,
            "longitude" to dpWithImage.longitude,
            "imageBase64" to dpWithImage.imageBase64,
            "notes" to dpWithImage.notes,
            "createdAt" to dpWithImage.createdAt,
            "createdBy" to dpWithImage.createdBy,
            "createdByName" to dpWithImage.createdByName,
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
        val imageBase64 = dp.photoPath?.let { path ->
            try {
                val bytes = File(path).readBytes()
                Base64.encodeToString(bytes, Base64.NO_WRAP)
            } catch (_: Exception) {
                dp.imageBase64
            }
        } ?: dp.imageBase64

        val updated = dp.copy(imageBase64 = imageBase64)
        dao.insert(DistributionPointEntity.fromDomain(updated))

        val firestoreData = mapOf(
            "label" to updated.label,
            "type" to updated.type.name,
            "latitude" to updated.latitude,
            "longitude" to updated.longitude,
            "imageBase64" to updated.imageBase64,
            "notes" to updated.notes,
            "createdAt" to updated.createdAt,
            "createdBy" to updated.createdBy,
            "createdByName" to updated.createdByName,
        )
        try {
            collection.document(updated.id).set(firestoreData).await()
        } catch (_: Exception) {
            // Offline
        }
    }
}
