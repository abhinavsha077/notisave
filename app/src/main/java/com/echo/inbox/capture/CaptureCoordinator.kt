package com.echo.inbox.capture

import com.echo.inbox.core.model.RawNotificationSnapshot
import com.echo.inbox.data.repository.CaptureHealthRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.launch
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.atomic.AtomicReference

/**
 * A bounded, single-writer capture queue. Listener callbacks never block on database work. If a
 * pathological burst exceeds the 4,096-item buffer, the event is dropped deliberately and exposed
 * through Capture Health instead of creating an unbounded number of suspended coroutines.
 *
 * Health updates use conflated channels: a burst can create at most the fixed worker coroutines
 * below, rather than one telemetry coroutine per notification callback.
 */
class CaptureCoordinator(
    private val scope: CoroutineScope,
    private val processor: CaptureProcessor,
    private val healthRepository: CaptureHealthRepository,
) {
    private val queue = Channel<CaptureCommand>(capacity = QUEUE_CAPACITY)
    private val queueUpdateSignals = Channel<Unit>(capacity = Channel.CONFLATED)
    private val droppedUpdateSignals = Channel<Unit>(capacity = Channel.CONFLATED)
    private val pendingCount = AtomicInteger(0)
    private val unreportedDrops = AtomicInteger(0)
    private val lastDropCode = AtomicReference("CAPTURE_QUEUE_FULL")

    init {
        scope.launch {
            for (command in queue) {
                try {
                    processor.process(command)
                } catch (error: CancellationException) {
                    throw error
                } catch (_: Exception) {
                    healthRepository.databaseFailure("CAPTURE_COORDINATOR_FAILURE")
                } finally {
                    pendingCount.updateAndGet { count -> (count - 1).coerceAtLeast(0) }
                    signalQueueCount()
                }
            }
        }
        scope.launch {
            for (ignored in queueUpdateSignals) {
                healthRepository.queueCount(pendingCount.get().coerceAtLeast(0))
            }
        }
        scope.launch {
            for (ignored in droppedUpdateSignals) {
                while (true) {
                    val count = unreportedDrops.getAndSet(0)
                    if (count <= 0) break
                    healthRepository.droppedEvents(count, lastDropCode.get())
                }
            }
        }
    }

    fun posted(snapshot: RawNotificationSnapshot) = enqueue(CaptureCommand.Posted(snapshot))

    fun removed(systemKey: String, postedAt: Long, reason: Int?) = enqueue(
        CaptureCommand.Removed(
            systemKey = systemKey,
            postedAt = postedAt,
            removedAt = System.currentTimeMillis(),
            reason = reason,
        ),
    )

    fun listenerConnected() {
        scope.launch { healthRepository.listenerConnected() }
    }

    fun listenerDisconnected() {
        scope.launch { healthRepository.listenerDisconnected() }
    }

    fun extractionFailed() {
        scope.launch { healthRepository.parserFailure("SNAPSHOT_EXTRACTION_FAILURE") }
    }

    private fun enqueue(command: CaptureCommand) {
        pendingCount.incrementAndGet()
        val result = queue.trySend(command)
        if (result.isFailure) {
            pendingCount.updateAndGet { count -> (count - 1).coerceAtLeast(0) }
            lastDropCode.set(
                if (queue.isClosedForSend) "CAPTURE_QUEUE_CLOSED" else "CAPTURE_QUEUE_FULL",
            )
            unreportedDrops.incrementAndGet()
            droppedUpdateSignals.trySend(Unit)
        }
        signalQueueCount()
    }

    private fun signalQueueCount() {
        queueUpdateSignals.trySend(Unit)
    }

    private companion object {
        const val QUEUE_CAPACITY = 4_096
    }
}
