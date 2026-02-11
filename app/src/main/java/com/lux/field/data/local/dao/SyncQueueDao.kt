package com.lux.field.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.lux.field.data.local.entity.SyncQueueEntity

@Dao
interface SyncQueueDao {

    @Insert
    suspend fun insert(entry: SyncQueueEntity): Long

    @Query("SELECT * FROM sync_queue WHERE retryCount < :maxRetries ORDER BY createdAt ASC LIMIT :limit")
    suspend fun getPending(maxRetries: Int = 10, limit: Int = 20): List<SyncQueueEntity>

    @Query("UPDATE sync_queue SET retryCount = retryCount + 1 WHERE id = :id")
    suspend fun incrementRetry(id: Long)

    @Query("DELETE FROM sync_queue WHERE id = :id")
    suspend fun delete(id: Long)

    @Query("DELETE FROM sync_queue WHERE retryCount >= :maxRetries")
    suspend fun deleteExhausted(maxRetries: Int = 10)
}
