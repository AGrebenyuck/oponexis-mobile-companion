package com.oponexis.companion.platform.callscreening

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

internal const val INTERNAL_RESPONSE_BUDGET_MILLIS = 100L

data class CallScreeningSnapshot(
    val callbackCount: Long = 0,
    val incomingCount: Long = 0,
    val outgoingCount: Long = 0,
    val lastResponseMillis: Double? = null,
    val maximumResponseMillis: Double? = null,
    val lastResponseWithinBudget: Boolean? = null,
)

/**
 * Process-local, privacy-safe evidence for the M2 proof of concept.
 *
 * No call handle, phone number, contact data, or CRM data is accepted by this API.
 */
object CallScreeningDiagnostics {
    private val mutableSnapshot = MutableStateFlow(CallScreeningSnapshot())
    val snapshot: StateFlow<CallScreeningSnapshot> = mutableSnapshot.asStateFlow()

    @Synchronized
    fun recordIncomingResponse(elapsedNanos: Long) {
        val elapsedMillis = elapsedNanos.coerceAtLeast(0) / NANOS_PER_MILLISECOND
        val current = mutableSnapshot.value
        mutableSnapshot.value = current.copy(
            callbackCount = current.callbackCount + 1,
            incomingCount = current.incomingCount + 1,
            lastResponseMillis = elapsedMillis,
            maximumResponseMillis = maxOf(current.maximumResponseMillis ?: 0.0, elapsedMillis),
            lastResponseWithinBudget = elapsedMillis <= INTERNAL_RESPONSE_BUDGET_MILLIS,
        )
    }

    @Synchronized
    fun recordOutgoingObservation() {
        val current = mutableSnapshot.value
        mutableSnapshot.value = current.copy(
            callbackCount = current.callbackCount + 1,
            outgoingCount = current.outgoingCount + 1,
        )
    }

    internal fun resetForTest() {
        mutableSnapshot.value = CallScreeningSnapshot()
    }

    private const val NANOS_PER_MILLISECOND = 1_000_000.0
}
