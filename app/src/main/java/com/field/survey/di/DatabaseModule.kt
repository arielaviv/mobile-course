package com.field.survey.di

import android.content.Context
import androidx.room.Room
import com.field.survey.data.local.FieldSurveyDatabase
import com.field.survey.data.local.MIGRATION_1_2
import com.field.survey.data.local.MIGRATION_2_3
import com.field.survey.data.local.MIGRATION_3_4
import com.field.survey.data.local.MIGRATION_4_5
import com.field.survey.data.local.MIGRATION_5_6
import com.field.survey.data.local.MIGRATION_6_7
import com.field.survey.data.local.MIGRATION_7_8
import com.field.survey.data.local.MIGRATION_8_9
import com.field.survey.data.local.dao.CommentDao
import com.field.survey.data.local.dao.DistributionPointDao
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
    fun provideDatabase(
        @ApplicationContext context: Context,
    ): FieldSurveyDatabase {
        return Room.databaseBuilder(
            context,
            FieldSurveyDatabase::class.java,
            "field_survey.db",
        )
            .addMigrations(
                MIGRATION_1_2,
                MIGRATION_2_3,
                MIGRATION_3_4,
                MIGRATION_4_5,
                MIGRATION_5_6,
                MIGRATION_6_7,
                MIGRATION_7_8,
                MIGRATION_8_9,
            )
            .build()
    }

    @Provides
    fun provideDistributionPointDao(database: FieldSurveyDatabase): DistributionPointDao = database.distributionPointDao()

    @Provides
    fun provideCommentDao(database: FieldSurveyDatabase): CommentDao = database.commentDao()
}
