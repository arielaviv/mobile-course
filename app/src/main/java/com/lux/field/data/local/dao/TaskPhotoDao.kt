package com.lux.field.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.lux.field.data.local.entity.TaskPhotoEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface TaskPhotoDao {

    @Query("SELECT * FROM task_photos WHERE taskId = :taskId ORDER BY capturedAt DESC")
    fun observeByTask(taskId: String): Flow<List<TaskPhotoEntity>>

    @Query("SELECT * FROM task_photos WHERE id = :id")
    suspend fun getById(id: String): TaskPhotoEntity?

    @Query("SELECT COUNT(*) FROM task_photos WHERE taskId = :taskId")
    suspend fun countByTask(taskId: String): Int

    @Query("SELECT COUNT(*) FROM task_photos WHERE taskId = :taskId AND cameraFacing = :facing")
    suspend fun countByTaskAndFacing(taskId: String, facing: String): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(photo: TaskPhotoEntity)

    @Query("UPDATE task_photos SET analysisStatus = :status, analysisResult = :result WHERE id = :id")
    suspend fun updateAnalysis(id: String, status: String, result: String?)

    @Query("DELETE FROM task_photos WHERE id = :id")
    suspend fun delete(id: String)
}
