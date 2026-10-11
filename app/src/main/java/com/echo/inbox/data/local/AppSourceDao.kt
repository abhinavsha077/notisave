package com.echo.inbox.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow

@Dao
interface AppSourceDao {
    @Query("SELECT * FROM app_sources ORDER BY captureEnabled DESC, displayName COLLATE NOCASE")
    fun observeAll(): Flow<List<AppSourceEntity>>

    @Query("SELECT * FROM app_sources WHERE eventCount > 0 OR packageName IN (SELECT DISTINCT appPackage FROM conversations) ORDER BY eventCount DESC, lastSeenAt DESC, displayName COLLATE NOCASE")
    fun observeWithEvents(): Flow<List<AppSourceEntity>>

    @Query("SELECT * FROM app_sources WHERE packageName = :packageName LIMIT 1")
    suspend fun get(packageName: String): AppSourceEntity?

    @Query("SELECT * FROM app_sources")
    suspend fun getAll(): List<AppSourceEntity>

    @Upsert
    suspend fun upsert(entity: AppSourceEntity): Long

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertIgnore(entity: AppSourceEntity): Long

    @Query(
        """
        UPDATE app_sources
        SET displayName = :displayName,
            category = :category,
            sensitive = :sensitive,
            lastSeenAt = :lastSeenAt
        WHERE packageName = :packageName
        """,
    )
    suspend fun updateSeenMetadata(
        packageName: String,
        displayName: String,
        category: String,
        sensitive: Boolean,
        lastSeenAt: Long,
    ): Int

    @Query("UPDATE app_sources SET captureEnabled = :enabled WHERE packageName = :packageName")
    suspend fun setCaptureEnabled(packageName: String, enabled: Boolean): Int

    @Query("UPDATE app_sources SET eventCount = eventCount + :delta, lastSeenAt = :at WHERE packageName = :packageName")
    suspend fun incrementEventCount(packageName: String, delta: Long, at: Long): Int

    @Query("DELETE FROM app_sources WHERE packageName = :packageName")
    suspend fun delete(packageName: String): Int
}
