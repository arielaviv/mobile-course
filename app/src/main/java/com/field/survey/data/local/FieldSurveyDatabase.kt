package com.field.survey.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import com.field.survey.data.local.dao.CommentDao
import com.field.survey.data.local.dao.DistributionPointDao
import com.field.survey.data.local.entity.CommentEntity
import com.field.survey.data.local.entity.DistributionPointEntity

@Database(
    entities = [
        DistributionPointEntity::class,
        CommentEntity::class,
    ],
    version = 9,
    exportSchema = true,
)
abstract class FieldSurveyDatabase : RoomDatabase() {
    abstract fun distributionPointDao(): DistributionPointDao

    abstract fun commentDao(): CommentDao
}
