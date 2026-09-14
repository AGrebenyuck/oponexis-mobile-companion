package com.oponexis.companion.platform.sync

import com.oponexis.companion.data.local.EventOutboxDao
import com.oponexis.companion.domain.repository.OutboxScheduler
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class OutboxRecoveryCoordinator @Inject constructor(
    private val dao: EventOutboxDao,
    private val scheduler: OutboxScheduler,
) {
    suspend fun recover() {
        dao.findSchedulable(System.currentTimeMillis()).forEach { scheduler.schedule(it.eventId) }
    }
}
