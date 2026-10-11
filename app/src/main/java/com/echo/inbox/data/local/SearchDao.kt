package com.echo.inbox.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.RawQuery
import androidx.sqlite.db.SupportSQLiteQuery
import kotlinx.coroutines.flow.Flow

@Dao
interface SearchDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entity: MessageSearchFtsEntity): Long

    @Query("DELETE FROM message_search_fts WHERE rowid IN (:messageIds)")
    suspend fun deleteRows(messageIds: List<Long>): Int

    @Query("DELETE FROM message_search_fts")
    suspend fun clear(): Int

    @RawQuery(
        observedEntities = [
            MessageSearchFtsEntity::class,
            MessageEventEntity::class,
            ConversationEntity::class,
            AppSourceEntity::class,
        ],
    )
    fun search(query: SupportSQLiteQuery): Flow<List<SearchResultRow>>
}
