package com.lux.field.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.lux.field.data.local.entity.ChatMessageEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ChatMessageDao {

    @Query("SELECT * FROM chat_messages WHERE taskId = :taskId ORDER BY createdAt ASC")
    fun observeByTask(taskId: String): Flow<List<ChatMessageEntity>>

    @Query("SELECT * FROM chat_messages WHERE taskId = :taskId ORDER BY createdAt ASC")
    suspend fun getByTask(taskId: String): List<ChatMessageEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(message: ChatMessageEntity)

    @Query("DELETE FROM chat_messages WHERE taskId = :taskId")
    suspend fun deleteByTask(taskId: String)
}
