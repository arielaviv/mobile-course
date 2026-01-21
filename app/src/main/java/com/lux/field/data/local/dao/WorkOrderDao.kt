package com.lux.field.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.lux.field.data.local.entity.WorkOrderEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface WorkOrderDao {

    @Query("SELECT * FROM work_orders ORDER BY priority DESC, updatedAt DESC")
    fun observeAll(): Flow<List<WorkOrderEntity>>

    @Query("SELECT * FROM work_orders WHERE id = :id")
    suspend fun getById(id: String): WorkOrderEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(workOrders: List<WorkOrderEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(workOrder: WorkOrderEntity)

    @Query("DELETE FROM work_orders")
    suspend fun deleteAll()

    @Query("DELETE FROM work_orders WHERE id = :id")
    suspend fun deleteById(id: String)
}
