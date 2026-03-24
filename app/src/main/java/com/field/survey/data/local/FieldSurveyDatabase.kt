package com.field.survey.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import com.field.survey.data.local.dao.DistributionPointDao
import com.field.survey.data.local.entity.DistributionPointEntity

@Database(
    entities = [
        DistributionPointEntity::class,
    ],
    version = 5,
    exportSchema = true,
)
abstract class FieldSurveyDatabase : RoomDatabase() {
    abstract fun distributionPointDao(): DistributionPointDao
}
