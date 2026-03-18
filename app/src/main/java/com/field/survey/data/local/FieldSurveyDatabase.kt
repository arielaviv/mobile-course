package com.field.survey.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import com.field.survey.data.local.dao.ChatMessageDao
import com.field.survey.data.local.dao.DistributionPointDao
import com.field.survey.data.local.dao.TaskDao
import com.field.survey.data.local.dao.TaskPhotoDao
import com.field.survey.data.local.dao.WorkOrderDao
import com.field.survey.data.local.entity.ChatMessageEntity
import com.field.survey.data.local.entity.DistributionPointEntity
import com.field.survey.data.local.entity.SyncQueueEntity
import com.field.survey.data.local.entity.TaskEntity
import com.field.survey.data.local.entity.TaskPhotoEntity
import com.field.survey.data.local.entity.WorkOrderEntity

@Database(
    entities = [
        WorkOrderEntity::class,
        TaskEntity::class,
        SyncQueueEntity::class,
        TaskPhotoEntity::class,
        ChatMessageEntity::class,
        DistributionPointEntity::class,
    ],
    version = 4,
    exportSchema = true,
)
abstract class FieldSurveyDatabase : RoomDatabase() {
    abstract fun workOrderDao(): WorkOrderDao
    abstract fun taskDao(): TaskDao
    abstract fun taskPhotoDao(): TaskPhotoDao
    abstract fun chatMessageDao(): ChatMessageDao
    abstract fun distributionPointDao(): DistributionPointDao
}
