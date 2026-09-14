package com.oponexis.companion.data.local

import com.oponexis.companion.domain.model.CallOutcomeCode
import com.oponexis.companion.domain.model.CallDirection
import com.oponexis.companion.domain.model.PendingCallOutcome
import com.oponexis.companion.domain.repository.CallOutcomeRepository
import com.oponexis.companion.domain.repository.OutboxScheduler
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

@Singleton
class RoomCallOutcomeRepository @Inject constructor(
    private val dao: CallOutcomeDao,
    private val outboxScheduler: OutboxScheduler,
) : CallOutcomeRepository {
    override fun observePending(): Flow<List<PendingCallOutcome>> =
        dao.observePending().map { entities -> entities.map(CallOutcomeEntity::toDomain) }

    override suspend fun pending(callRef: String): PendingCallOutcome? =
        dao.findByCallRef(callRef)
            ?.takeIf { it.outcomeCode == null && it.dismissedAtEpochMillis == null }
            ?.toDomain()

    override suspend fun createPending(
        disconnectCategory: String,
        durationBucket: String,
        phoneNumber: String?,
		direction: CallDirection,
    ): PendingCallOutcome {
        val entity = CallOutcomeEntity(
            callRef = UUID.randomUUID().toString(),
            observedAtEpochMillis = System.currentTimeMillis(),
            disconnectCategory = disconnectCategory,
            durationBucket = durationBucket,
            phoneNumber = phoneNumber,
			direction = direction.name,
        )
        dao.insert(entity)
        return entity.toDomain()
    }

    override suspend fun select(callRef: String, outcome: CallOutcomeCode, topicLabel: String?): Boolean {
        val eventId = UUID.randomUUID().toString()
        val selected = dao.replaceOutcomeAndEnqueue(
            callRef = callRef,
            outcomeCode = outcome.persistedCode,
            topicLabel = topicLabel,
            eventId = eventId,
            nowEpochMillis = System.currentTimeMillis(),
        )
        if (selected) outboxScheduler.schedule(eventId)
        return selected
    }

    override suspend fun dismiss(callRef: String): Boolean =
        dao.dismiss(callRef, System.currentTimeMillis()) == 1

    override suspend fun recordSmsReceipt(
        callRef: String,
        receiptId: String,
        receiptType: String,
        status: String,
    ): Boolean = dao.recordSmsReceipt(callRef, receiptId, receiptType, status) == 1

    override suspend fun recordSmsFailure(callRef: String, detail: String): Boolean =
        dao.updateSmsDeliveryStatus(callRef, "FAILED", detail) == 1

    override suspend fun updateSmsDeliveryStatus(callRef: String, status: String, detail: String?): Boolean =
        dao.updateSmsDeliveryStatus(callRef, status, detail) == 1

    override suspend fun attachIdentity(
        callRef: String,
        displayName: String?,
        customerRef: String,
		isReturningCustomer: Boolean,
	): Boolean = dao.attachIdentity(callRef, displayName, customerRef, isReturningCustomer) == 1

    override suspend fun attachIdentityForPhone(
        phoneNumber: String,
        displayName: String?,
        customerRef: String,
		isReturningCustomer: Boolean,
    ): Boolean = dao.attachIdentityForPhone(
        phoneNumber,
        displayName,
        customerRef,
        isReturningCustomer,
    ) > 0

    override suspend fun clearIdentityForPhone(phoneNumber: String): Boolean =
        dao.clearIdentityForPhone(phoneNumber) > 0
}

private fun CallOutcomeEntity.toDomain(): PendingCallOutcome = PendingCallOutcome(
    callRef = callRef,
    observedAtEpochMillis = observedAtEpochMillis,
    disconnectCategory = disconnectCategory,
    durationBucket = durationBucket,
    phoneNumber = phoneNumber,
    displayName = displayName,
    isReturningCustomer = isReturningCustomer,
)
