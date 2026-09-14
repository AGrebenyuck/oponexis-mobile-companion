package com.oponexis.companion.domain.repository

import com.oponexis.companion.domain.model.CallOutcomeCode
import com.oponexis.companion.domain.model.CallDirection
import com.oponexis.companion.domain.model.PendingCallOutcome
import kotlinx.coroutines.flow.Flow

interface CallOutcomeRepository {
    fun observePending(): Flow<List<PendingCallOutcome>>

    suspend fun pending(callRef: String): PendingCallOutcome?

    suspend fun createPending(
        disconnectCategory: String,
        durationBucket: String,
        phoneNumber: String?,
		direction: CallDirection,
    ): PendingCallOutcome

    suspend fun select(callRef: String, outcome: CallOutcomeCode, topicLabel: String? = null): Boolean

    suspend fun dismiss(callRef: String): Boolean

    suspend fun recordSmsReceipt(callRef: String, receiptId: String, receiptType: String, status: String): Boolean

    suspend fun recordSmsFailure(callRef: String, detail: String): Boolean

    suspend fun updateSmsDeliveryStatus(callRef: String, status: String, detail: String? = null): Boolean

    suspend fun attachIdentity(
        callRef: String,
        displayName: String?,
        customerRef: String,
		isReturningCustomer: Boolean,
    ): Boolean

    suspend fun attachIdentityForPhone(
        phoneNumber: String,
        displayName: String?,
        customerRef: String,
		isReturningCustomer: Boolean,
    ): Boolean

    suspend fun clearIdentityForPhone(phoneNumber: String): Boolean
}
