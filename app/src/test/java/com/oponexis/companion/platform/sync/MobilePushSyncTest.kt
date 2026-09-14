package com.oponexis.companion.platform.sync

import org.junit.Assert.assertEquals
import org.junit.Test

class MobilePushSyncTest {
    @Test
    fun `queued message waits exactly four minutes`() {
        val queuedAt = 1_000L

        assertEquals(4L * 60 * 1_000, stallDelayMillis(queuedAt, queuedAt))
        assertEquals(60_000L, stallDelayMillis(queuedAt, queuedAt + 3L * 60 * 1_000))
    }

    @Test
    fun `overdue message checks immediately`() {
        val queuedAt = 1_000L

        assertEquals(0L, stallDelayMillis(queuedAt, queuedAt + 5L * 60 * 1_000))
    }

    @Test
    fun `only recent queued messages create stall checks`() {
        val now = 20L * 60 * 1_000

        assertEquals(true, shouldScheduleStallCheck(now - 9L * 60 * 1_000, now))
        assertEquals(false, shouldScheduleStallCheck(now - 11L * 60 * 1_000, now))
        assertEquals(false, shouldScheduleStallCheck(now + 1_000, now))
    }

    @Test
    fun `receipt retry schedule reaches warning near four minutes`() {
        val warningAt = SMS_RECEIPT_INITIAL_DELAY_MILLIS +
            SMS_RECEIPT_BACKOFF_MILLIS * (1 + 2 + 4 + 8)

        assertEquals(245_000L, warningAt)
        assertEquals(4, SMS_RECEIPT_WARNING_ATTEMPT)
    }
}
