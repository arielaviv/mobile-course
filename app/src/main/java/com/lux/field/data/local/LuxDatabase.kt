package com.lux.field.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import com.lux.field.data.local.dao.TaskDao
import com.lux.field.data.local.dao.WorkOrderDao
import com.lux.field.data.local.entity.SyncQueueEntity
import com.lux.field.data.local.entity.TaskEntity
import com.lux.field.data.local.entity.WorkOrderEntity

@Database(
    entities = [
        WorkOrderEntity::class,
        TaskEntity::class,
        SyncQueueEntity::class,
    ],
    version = 1,
    exportSchema = true,
)
abstract class LuxDatabase : RoomDatabase() {
    abstract fun workOrderDao(): WorkOrderDao
    abstract fun taskDao(): TaskDao
}
