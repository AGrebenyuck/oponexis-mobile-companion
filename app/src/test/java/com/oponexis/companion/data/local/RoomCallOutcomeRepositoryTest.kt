package com.oponexis.companion.data.local

import com.oponexis.companion.domain.model.CallOutcomeCode
import com.oponexis.companion.domain.model.CallDirection
import java.util.UUID
import com.oponexis.companion.domain.repository.OutboxScheduler
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class RoomCallOutcomeRepositoryTest {
    @Test
    fun `creates identity-free pending draft`() = runTest {
        val dao = FakeCallOutcomeDao()
        val repository = RoomCallOutcomeRepository(dao, FakeOutboxScheduler())

        val pending = repository.createPending("remote", "short", "+48123456789", CallDirection.Incoming)

        assertNotNull(UUID.fromString(pending.callRef))
        assertEquals("remote", pending.disconnectCategory)
        assertEquals("short", pending.durationBucket)
        assertEquals("+48123456789", pending.phoneNumber)
        assertEquals(listOf(pending), repository.observePending().first())
        assertEquals(pending, repository.pending(pending.callRef))
    }

    @Test
    fun `selection can update a resolved follow-up`() = runTest {
        val dao = FakeCallOutcomeDao()
        val scheduler = FakeOutboxScheduler()
        val repository = RoomCallOutcomeRepository(dao, scheduler)
        val pending = repository.createPending("local", "short", "+48123456789", CallDirection.Incoming)

        assertTrue(repository.select(pending.callRef, CallOutcomeCode.Interested))
		assertTrue(repository.select(pending.callRef, CallOutcomeCode.Other))
        assertTrue(repository.observePending().first().isEmpty())
		assertEquals("other", dao.entities.value.single().outcomeCode)
        assertEquals(null, dao.entities.value.single().phoneNumber)
        assertEquals(1, dao.outbox.size)
		assertEquals(dao.outbox.single().eventId, scheduler.scheduled.last())
    }

    @Test
    fun `dismiss keeps choice optional and resolves draft`() = runTest {
        val dao = FakeCallOutcomeDao()
        val repository = RoomCallOutcomeRepository(dao, FakeOutboxScheduler())
        val pending = repository.createPending("missed", "veryshort", "+48123456789", CallDirection.Incoming)

        assertTrue(repository.dismiss(pending.callRef))
        assertTrue(repository.observePending().first().isEmpty())
        assertNotNull(dao.entities.value.single().dismissedAtEpochMillis)
    }
}

private class FakeCallOutcomeDao : CallOutcomeDao {
    val entities = MutableStateFlow<List<CallOutcomeEntity>>(emptyList())
    val outbox = mutableListOf<EventOutboxEntity>()

    override suspend fun insert(entity: CallOutcomeEntity) {
        entities.value = entities.value + entity
    }

    override fun observePending(): Flow<List<CallOutcomeEntity>> =
        MutableStateFlow(
            entities.value.filter { it.outcomeCode == null && it.dismissedAtEpochMillis == null },
        )

    override fun observeHistory(): Flow<List<CallOutcomeEntity>> = entities

    override suspend fun countAll(): Int = entities.value.size

    override suspend fun findByCallRef(callRef: String): CallOutcomeEntity? =
        entities.value.firstOrNull { it.callRef == callRef }

    override suspend fun findSmsReceiptsAwaitingStatus(): List<CallOutcomeEntity> =
        entities.value.filter {
            it.smsReceiptId != null &&
                it.smsReceiptType != null &&
                it.smsDeliveryStatus !in setOf("SENT", "DELIVERED", "FAILED", "CANCELLED")
        }

    override suspend fun insertOutbox(entity: EventOutboxEntity) {
        outbox += entity
    }

    override suspend fun selectOutcome(
        callRef: String,
        outcomeCode: String,
        topicLabel: String?,
        resolvedAtEpochMillis: Long,
    ): Int = updatePending(callRef) {
        copy(
            outcomeCode = outcomeCode,
            topicLabel = topicLabel,
            resolvedAtEpochMillis = resolvedAtEpochMillis,
            historyPhoneNumber = phoneNumber,
            historyDisplayName = displayName,
            phoneNumber = null,
            displayName = null,
        )
    }

	override suspend fun replaceOutcome(
		callRef: String,
		outcomeCode: String,
		topicLabel: String?,
		resolvedAtEpochMillis: Long,
	): Int {
		var updated = false
		entities.value = entities.value.map { entity ->
			if (entity.callRef == callRef) {
				updated = true
				entity.copy(
					outcomeCode = outcomeCode,
					topicLabel = topicLabel,
					resolvedAtEpochMillis = resolvedAtEpochMillis,
					historyPhoneNumber = entity.historyPhoneNumber ?: entity.phoneNumber,
					historyDisplayName = entity.historyDisplayName ?: entity.displayName,
					phoneNumber = null,
					displayName = null,
					dismissedAtEpochMillis = null,
				)
			} else entity
		}
		return if (updated) 1 else 0
	}

	override suspend fun deleteOutboxForCall(callRef: String): Int {
		val count = outbox.count { it.callRef == callRef }
		outbox.removeAll { it.callRef == callRef }
		return count
	}

    override suspend fun dismiss(callRef: String, dismissedAtEpochMillis: Long): Int =
        updatePending(callRef) {
            copy(
                dismissedAtEpochMillis = dismissedAtEpochMillis,
                historyPhoneNumber = phoneNumber,
                historyDisplayName = displayName,
                phoneNumber = null,
                displayName = null,
            )
        }

    override suspend fun attachIdentity(
        callRef: String,
        displayName: String?,
        customerRef: String,
		isReturningCustomer: Boolean,
    ): Int = updatePending(callRef) {
		copy(displayName = displayName, customerRef = customerRef, isReturningCustomer = isReturningCustomer)
	}

    override suspend fun attachIdentityForPhone(
        phoneNumber: String,
        displayName: String?,
        customerRef: String,
		isReturningCustomer: Boolean,
    ): Int {
        var count = 0
        entities.value = entities.value.map { entity ->
            if (entity.phoneNumber == phoneNumber || entity.historyPhoneNumber == phoneNumber) {
                count += 1
                entity.copy(
                    displayName = displayName ?: entity.displayName,
                    historyDisplayName = displayName ?: entity.historyDisplayName,
                    customerRef = customerRef,
                    isReturningCustomer = isReturningCustomer,
                )
            } else {
                entity
            }
        }
        return count
    }

    override suspend fun clearIdentityForPhone(phoneNumber: String): Int {
        var count = 0
        entities.value = entities.value.map { entity ->
            if (entity.phoneNumber == phoneNumber || entity.historyPhoneNumber == phoneNumber) {
                count += 1
                entity.copy(
                    displayName = null,
                    historyDisplayName = null,
                    customerRef = null,
                    isReturningCustomer = false,
                )
            } else {
                entity
            }
        }
        return count
    }

    override suspend fun recordSmsReceipt(
        callRef: String,
        receiptId: String,
        receiptType: String,
        status: String,
    ): Int = updateAny(callRef) {
        copy(smsReceiptId = receiptId, smsReceiptType = receiptType, smsDeliveryStatus = status, smsDeliveryDetail = null)
    }

    override suspend fun updateSmsDeliveryStatus(callRef: String, status: String, detail: String?): Int =
        updateAny(callRef) { copy(smsDeliveryStatus = status, smsDeliveryDetail = detail) }

    override suspend fun updateSmsDeliveryStatusByReceipt(
        providerMessageId: String,
        status: String,
        detail: String?,
    ): Int {
        var count = 0
        entities.value = entities.value.map { entity ->
            if (entity.smsReceiptId == providerMessageId) {
                count += 1
                entity.copy(smsDeliveryStatus = status, smsDeliveryDetail = detail)
            } else {
                entity
            }
        }
        return count
    }

    override suspend fun deleteResolvedBefore(cutoffEpochMillis: Long): Int = 0

    private fun updatePending(
        callRef: String,
        update: CallOutcomeEntity.() -> CallOutcomeEntity,
    ): Int {
        var updated = false
        entities.value = entities.value.map { entity ->
            if (entity.callRef == callRef &&
                entity.outcomeCode == null &&
                entity.dismissedAtEpochMillis == null
            ) {
                updated = true
                entity.update()
            } else {
                entity
            }
        }
        return if (updated) 1 else 0
    }

    private fun updateAny(callRef: String, update: CallOutcomeEntity.() -> CallOutcomeEntity): Int {
        var updated = false
        entities.value = entities.value.map { entity ->
            if (entity.callRef == callRef) {
                updated = true
                entity.update()
            } else entity
        }
        return if (updated) 1 else 0
    }
}

private class FakeOutboxScheduler : OutboxScheduler {
    val scheduled = mutableListOf<String>()
    override fun schedule(eventId: String) {
        scheduled += eventId
    }
}
