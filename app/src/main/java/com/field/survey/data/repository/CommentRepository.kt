package com.field.survey.data.repository

import com.field.survey.data.local.dao.CommentDao
import com.field.survey.data.local.entity.CommentEntity
import com.field.survey.domain.model.Comment
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class CommentRepository @Inject constructor(
    private val dao: CommentDao,
) {
    private val firestore = FirebaseFirestore.getInstance()

    private fun collection(postId: String) =
        firestore.collection("distribution_points").document(postId).collection("comments")

    fun observeComments(postId: String): Flow<List<Comment>> =
        dao.observeByPostId(postId).map { entities -> entities.map { it.toDomain() } }

    suspend fun syncComments(postId: String) {
        try {
            val snapshot = collection(postId)
                .orderBy("createdAt", Query.Direction.ASCENDING)
                .get()
                .await()
            val comments = snapshot.documents.mapNotNull { doc ->
                try {
                    CommentEntity(
                        id = doc.id,
                        postId = postId,
                        userId = doc.getString("userId") ?: "",
                        userName = doc.getString("userName") ?: "",
                        text = doc.getString("text") ?: "",
                        createdAt = doc.getLong("createdAt") ?: 0L,
                    )
                } catch (_: Exception) {
                    null
                }
            }
            dao.deleteByPostId(postId)
            dao.insertAll(comments)
        } catch (_: Exception) {
            // offline, use cache
        }
    }

    suspend fun addComment(comment: Comment) {
        dao.insert(CommentEntity.fromDomain(comment))
        try {
            val data = mapOf(
                "userId" to comment.userId,
                "userName" to comment.userName,
                "text" to comment.text,
                "createdAt" to comment.createdAt,
            )
            collection(comment.postId).document(comment.id).set(data).await()
        } catch (_: Exception) {
            // saved locally
        }
    }

    suspend fun deleteComment(commentId: String, postId: String) {
        dao.deleteById(commentId)
        try {
            collection(postId).document(commentId).delete().await()
        } catch (_: Exception) {
            // deleted locally
        }
    }
}
