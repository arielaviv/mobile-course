package com.lux.field.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.lux.field.data.local.entity.TaskEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface TaskDao {

    @Query("SELECT * FROM tasks WHERE workOrderId = :workOrderId ORDER BY sequence ASC")
    fun observeByWorkOrder(workOrderId: String): Flow<List<TaskEntity>>

    @Query("SELECT * FROM tasks WHERE id = :id")
    suspend fun getById(id: String): TaskEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(tasks: List<TaskEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(task: TaskEntity)

    @Query("UPDATE tasks SET status = :status WHERE id = :id")
    suspend fun updateStatus(id: String, status: String)

    @Query("UPDATE tasks SET stepsJson = :stepsJson WHERE id = :id")
    suspend fun updateSteps(id: String, stepsJson: String)

    @Query("DELETE FROM tasks WHERE workOrderId = :workOrderId")
    suspend fun deleteByWorkOrder(workOrderId: String)
}
