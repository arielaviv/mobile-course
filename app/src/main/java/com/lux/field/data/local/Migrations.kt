package com.lux.field.data.local

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

val MIGRATION_1_2 = object : Migration(1, 2) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS `task_photos` (
                `id` TEXT NOT NULL,
                `taskId` TEXT NOT NULL,
                `stepId` TEXT,
                `workOrderId` TEXT NOT NULL,
                `filePath` TEXT NOT NULL,
                `thumbnailPath` TEXT,
                `cameraFacing` TEXT NOT NULL,
                `capturedAt` INTEGER NOT NULL,
                `latitude` REAL,
                `longitude` REAL,
                `analysisStatus` TEXT NOT NULL,
                `analysisResult` TEXT,
                PRIMARY KEY(`id`)
            )
            """.trimIndent()
        )
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_task_photos_taskId` ON `task_photos` (`taskId`)")
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_task_photos_workOrderId` ON `task_photos` (`workOrderId`)")

        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS `chat_messages` (
                `id` TEXT NOT NULL,
                `taskId` TEXT NOT NULL,
                `role` TEXT NOT NULL,
                `content` TEXT NOT NULL,
                `photoId` TEXT,
                `createdAt` INTEGER NOT NULL,
                PRIMARY KEY(`id`)
            )
            """.trimIndent()
        )
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_chat_messages_taskId` ON `chat_messages` (`taskId`)")
    }
}

val MIGRATION_2_3 = object : Migration(2, 3) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS `distribution_points` (
                `id` TEXT NOT NULL,
                `label` TEXT NOT NULL,
                `type` TEXT NOT NULL,
                `latitude` REAL NOT NULL,
                `longitude` REAL NOT NULL,
                `photoPath` TEXT,
                `notes` TEXT NOT NULL,
                `createdAt` INTEGER NOT NULL,
                `createdBy` TEXT NOT NULL,
                PRIMARY KEY(`id`)
            )
            """.trimIndent()
        )
    }
}
