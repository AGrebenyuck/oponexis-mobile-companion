package com.oponexis.companion.platform.callscreening

import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class CallScreeningDiagnosticsTest {
    @Before
    fun setUp() = CallScreeningDiagnostics.resetForTest()

    @After
    fun tearDown() = CallScreeningDiagnostics.resetForTest()

    @Test
    fun `incoming responses update counts timing and maximum`() {
        CallScreeningDiagnostics.recordIncomingResponse(2_500_000)
        CallScreeningDiagnostics.recordIncomingResponse(4_000_000)

        val snapshot = CallScreeningDiagnostics.snapshot.value
        assertEquals(2, snapshot.callbackCount)
        assertEquals(2, snapshot.incomingCount)
        assertEquals(0, snapshot.outgoingCount)
        assertEquals(4.0, snapshot.lastResponseMillis ?: -1.0, 0.0)
        assertEquals(4.0, snapshot.maximumResponseMillis ?: -1.0, 0.0)
        assertTrue(snapshot.lastResponseWithinBudget == true)
    }

    @Test
    fun `outgoing observation records no response timing`() {
        CallScreeningDiagnostics.recordOutgoingObservation()

        val snapshot = CallScreeningDiagnostics.snapshot.value
        assertEquals(1, snapshot.callbackCount)
        assertEquals(0, snapshot.incomingCount)
        assertEquals(1, snapshot.outgoingCount)
        assertEquals(null, snapshot.lastResponseMillis)
    }

    @Test
    fun `response over internal target is visible`() {
        CallScreeningDiagnostics.recordIncomingResponse(
            (INTERNAL_RESPONSE_BUDGET_MILLIS + 1) * 1_000_000,
        )

        assertFalse(CallScreeningDiagnostics.snapshot.value.lastResponseWithinBudget ?: true)
    }
}
