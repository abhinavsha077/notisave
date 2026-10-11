package com.echo.inbox.data.local

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow

@Dao
interface CaptureHealthDao {
    @Query("SELECT * FROM capture_health WHERE id = 1")
    fun observe(): Flow<CaptureHealthEntity?>

    @Query("SELECT * FROM capture_health WHERE id = 1")
    suspend fun get(): CaptureHealthEntity?

    @Upsert
    suspend fun upsert(entity: CaptureHealthEntity): Long
}
