package com.lux.field.di

import android.content.Context
import androidx.room.Room
import com.lux.field.data.local.LuxDatabase
import com.lux.field.data.local.MIGRATION_1_2
import com.lux.field.data.local.dao.ChatMessageDao
import com.lux.field.data.local.dao.TaskDao
import com.lux.field.data.local.dao.TaskPhotoDao
import com.lux.field.data.local.dao.WorkOrderDao
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
    fun provideDatabase(@ApplicationContext context: Context): LuxDatabase {
        return Room.databaseBuilder(
            context,
            LuxDatabase::class.java,
            "lux_field.db",
        )
            .addMigrations(MIGRATION_1_2)
            .build()
    }

    @Provides
    fun provideWorkOrderDao(database: LuxDatabase): WorkOrderDao = database.workOrderDao()

    @Provides
    fun provideTaskDao(database: LuxDatabase): TaskDao = database.taskDao()

    @Provides
    fun provideTaskPhotoDao(database: LuxDatabase): TaskPhotoDao = database.taskPhotoDao()

    @Provides
    fun provideChatMessageDao(database: LuxDatabase): ChatMessageDao = database.chatMessageDao()
}
