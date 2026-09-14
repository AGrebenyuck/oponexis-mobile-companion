package com.oponexis.companion.platform.sync

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.oponexis.companion.data.local.EventOutboxDao
import com.oponexis.companion.domain.repository.CallEventDeliveryRepository
import com.oponexis.companion.domain.repository.CallEventDeliveryResult
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.components.SingletonComponent

class OutboxSyncWorker(
    context: Context,
    parameters: WorkerParameters,
) : CoroutineWorker(context, parameters) {
    override suspend fun doWork(): Result {
        val dependencies = EntryPointAccessors.fromApplication(
            applicationContext,
            OutboxWorkerEntryPoint::class.java,
        )
        val dao = dependencies.outboxDao()
        val deliveryRepository = dependencies.deliveryRepository()
        val eventId = inputData.getString(KEY_EVENT_ID) ?: return Result.failure()
        val now = System.currentTimeMillis()
        if (dao.claim(eventId, now, now + LEASE_MILLIS) != 1) {
            val current = dao.findByEventId(eventId) ?: return Result.success()
            return if (current.status == "IN_FLIGHT") Result.retry() else Result.success()
        }
        val event = dao.findByEventId(eventId) ?: return Result.failure()
        return when (val result = deliveryRepository.deliver(event)) {
            is CallEventDeliveryResult.Delivered -> {
                dao.markDelivered(eventId, result.receiptId, System.currentTimeMillis())
                Result.success()
            }
            is CallEventDeliveryResult.PermanentFailure -> {
                dao.markPermanentFailure(eventId, result.category)
                Result.success()
            }
            is CallEventDeliveryResult.RetryableFailure -> {
                if (event.attemptCount + 1 >= MAX_ATTEMPTS) {
                    dao.markPermanentFailure(eventId, "retry_limit")
                    Result.success()
                } else {
                    dao.markRetry(eventId, result.category, System.currentTimeMillis())
                    Result.retry()
                }
            }
        }
    }

    companion object {
        const val KEY_EVENT_ID = "event_id"
        private const val LEASE_MILLIS = 5 * 60 * 1_000L
        private const val MAX_ATTEMPTS = 10
    }
}

@EntryPoint
@InstallIn(SingletonComponent::class)
internal interface OutboxWorkerEntryPoint {
    fun outboxDao(): EventOutboxDao
    fun deliveryRepository(): CallEventDeliveryRepository
}
