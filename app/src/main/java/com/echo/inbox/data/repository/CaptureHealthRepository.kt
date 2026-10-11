package com.echo.inbox.data.repository

import com.echo.inbox.data.local.CaptureHealthDao
import com.echo.inbox.data.local.CaptureHealthEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

class CaptureHealthRepository(private val dao: CaptureHealthDao) {
    private val writeMutex = Mutex()

    val health: Flow<CaptureHealthEntity> = dao.observe().map { it ?: CaptureHealthEntity() }

    suspend fun listenerConnected(at: Long = System.currentTimeMillis()) = update {
        it.copy(listenerConnected = true, lastConnectedAt = at, lastErrorCode = null)
    }

    suspend fun listenerDisconnected(at: Long = System.currentTimeMillis()) = update {
        it.copy(listenerConnected = false, lastDisconnectedAt = at)
    }

    suspend fun callbackReceived(at: Long = System.currentTimeMillis()) = update {
        it.copy(lastCallbackAt = at)
    }

    suspend fun successfulWrite(at: Long = System.currentTimeMillis()) = update {
        it.copy(lastSuccessfulWriteAt = at, lastErrorCode = null)
    }

    suspend fun testCaptured(at: Long = System.currentTimeMillis()) = update {
        it.copy(lastCallbackAt = at, lastTestCapturedAt = at, lastErrorCode = null)
    }

    suspend fun queueCount(count: Int) = update {
        it.copy(pendingQueueCount = count.coerceAtLeast(0))
    }

    suspend fun parserFailure(code: String, at: Long = System.currentTimeMillis()) = update {
        it.copy(
            parserFailureCount = it.parserFailureCount + 1,
            lastErrorCode = code,
            lastErrorAt = at,
        )
    }

    suspend fun databaseFailure(code: String, at: Long = System.currentTimeMillis()) = update {
        it.copy(
            databaseFailureCount = it.databaseFailureCount + 1,
            lastErrorCode = code,
            lastErrorAt = at,
        )
    }

    suspend fun droppedEvent(code: String, at: Long = System.currentTimeMillis()) =
        droppedEvents(count = 1, code = code, at = at)

    suspend fun droppedEvents(
        count: Int,
        code: String,
        at: Long = System.currentTimeMillis(),
    ) = update {
        it.copy(
            droppedEventCount = it.droppedEventCount + count.coerceAtLeast(0),
            lastErrorCode = code,
            lastErrorAt = at,
        )
    }

    private suspend fun update(transform: (CaptureHealthEntity) -> CaptureHealthEntity) {
        writeMutex.withLock {
            val current = dao.get() ?: CaptureHealthEntity()
            dao.upsert(transform(current))
        }
    }
}
