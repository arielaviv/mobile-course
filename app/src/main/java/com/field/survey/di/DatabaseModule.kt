package com.field.survey.di

import android.content.Context
import androidx.room.Room
import com.field.survey.data.local.FieldSurveyDatabase
import com.field.survey.data.local.MIGRATION_1_2
import com.field.survey.data.local.MIGRATION_2_3
import com.field.survey.data.local.MIGRATION_3_4
import com.field.survey.data.local.dao.ChatMessageDao
import com.field.survey.data.local.dao.DistributionPointDao
import com.field.survey.data.local.dao.TaskDao
import com.field.survey.data.local.dao.TaskPhotoDao
import com.field.survey.data.local.dao.WorkOrderDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): FieldSurveyDatabase {
        return Room.databaseBuilder(
            context,
            FieldSurveyDatabase::class.java,
            "field_survey.db",
        )
            .addMigrations(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4)
            .build()
    }

    @Provides
    fun provideWorkOrderDao(database: FieldSurveyDatabase): WorkOrderDao = database.workOrderDao()

    @Provides
    fun provideTaskDao(database: FieldSurveyDatabase): TaskDao = database.taskDao()

    @Provides
    fun provideTaskPhotoDao(database: FieldSurveyDatabase): TaskPhotoDao = database.taskPhotoDao()

    @Provides
    fun provideChatMessageDao(database: FieldSurveyDatabase): ChatMessageDao = database.chatMessageDao()

    @Provides
    fun provideDistributionPointDao(database: FieldSurveyDatabase): DistributionPointDao = database.distributionPointDao()
}
