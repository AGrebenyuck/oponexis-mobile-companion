package com.oponexis.companion.domain.repository

import com.oponexis.companion.data.local.EventOutboxEntity

sealed interface CallEventDeliveryResult {
    data class Delivered(val receiptId: String) : CallEventDeliveryResult
    data class RetryableFailure(val category: String) : CallEventDeliveryResult
    data class PermanentFailure(val category: String) : CallEventDeliveryResult
}

interface CallEventDeliveryRepository {
    suspend fun deliver(event: EventOutboxEntity): CallEventDeliveryResult
}
