package com.oponexis.companion.data.mock

import com.oponexis.companion.domain.model.CallDirection
import com.oponexis.companion.domain.model.CallState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class MockCompanionRepositoryTest {
    private val repository = MockCompanionRepository()

    @Test
    fun `dashboard summary is consistent with mock calls`() {
        val dashboard = repository.dashboard.value

        assertEquals(3, dashboard.recentCalls.size)
        assertTrue(dashboard.identifiedCalls <= dashboard.callsToday)
        assertEquals(0, dashboard.pendingEvents)
    }

    @Test
    fun `mock call history covers foundation states`() {
        val calls = repository.calls.value

        assertTrue(calls.any { it.direction == CallDirection.Incoming })
        assertTrue(calls.any { it.direction == CallDirection.Outgoing })
        assertEquals(CallState.entries.toSet(), calls.map { it.state }.toSet())
    }
}

