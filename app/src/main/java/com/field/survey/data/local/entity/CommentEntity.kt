package com.field.survey.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.field.survey.domain.model.Comment

@Entity(tableName = "comments")
data class CommentEntity(
    @PrimaryKey val id: String,
    val postId: String,
    val userId: String,
    val userName: String,
    val text: String,
    val createdAt: Long,
) {
    fun toDomain(): Comment = Comment(
        id = id,
        postId = postId,
        userId = userId,
        userName = userName,
        text = text,
        createdAt = createdAt,
    )

    companion object {
        fun fromDomain(c: Comment): CommentEntity = CommentEntity(
            id = c.id,
            postId = c.postId,
            userId = c.userId,
            userName = c.userName,
            text = c.text,
            createdAt = c.createdAt,
        )
    }
}
