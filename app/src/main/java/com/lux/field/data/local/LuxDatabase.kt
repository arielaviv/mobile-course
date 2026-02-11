package com.lux.field.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import com.lux.field.data.local.dao.ChatMessageDao
import com.lux.field.data.local.dao.LocationPointDao
import com.lux.field.data.local.dao.SyncQueueDao
import com.lux.field.data.local.dao.TaskDao
import com.lux.field.data.local.dao.TaskPhotoDao
import com.lux.field.data.local.dao.WorkOrderDao
import com.lux.field.data.local.entity.ChatMessageEntity
import com.lux.field.data.local.entity.LocationPointEntity
import com.lux.field.data.local.entity.SyncQueueEntity
import com.lux.field.data.local.entity.TaskEntity
import com.lux.field.data.local.entity.TaskPhotoEntity
import com.lux.field.data.local.entity.WorkOrderEntity

@Database(
    entities = [
        WorkOrderEntity::class,
        TaskEntity::class,
        SyncQueueEntity::class,
        TaskPhotoEntity::class,
        ChatMessageEntity::class,
        LocationPointEntity::class,
    ],
    version = 3,
    exportSchema = true,
)
abstract class LuxDatabase : RoomDatabase() {
    abstract fun workOrderDao(): WorkOrderDao
    abstract fun taskDao(): TaskDao
    abstract fun taskPhotoDao(): TaskPhotoDao
    abstract fun chatMessageDao(): ChatMessageDao
    abstract fun locationPointDao(): LocationPointDao
    abstract fun syncQueueDao(): SyncQueueDao
}
