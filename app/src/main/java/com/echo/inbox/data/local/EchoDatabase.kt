package com.echo.inbox.data.local

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(
    entities = [
        AppSourceEntity::class,
        ConversationEntity::class,
        NotificationSnapshotEntity::class,
        MessageEventEntity::class,
        MessageSearchFtsEntity::class,
        CaptureHealthEntity::class,
    ],
    version = 2,
    exportSchema = false,
)
abstract class EchoDatabase : RoomDatabase() {
    abstract fun appSourceDao(): AppSourceDao
    abstract fun conversationDao(): ConversationDao
    abstract fun snapshotDao(): SnapshotDao
    abstract fun messageDao(): MessageDao
    abstract fun searchDao(): SearchDao
    abstract fun captureHealthDao(): CaptureHealthDao
    abstract fun maintenanceDao(): MaintenanceDao
}
