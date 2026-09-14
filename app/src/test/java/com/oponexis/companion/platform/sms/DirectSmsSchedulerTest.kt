package com.oponexis.companion.platform.sms

import androidx.work.NetworkType
import org.junit.Assert.assertEquals
import org.junit.Test

class DirectSmsSchedulerTest {
    @Test
    fun `incoming SMS persistence does not wait for internet`() {
        assertEquals(NetworkType.NOT_REQUIRED, incomingSmsConstraints().requiredNetworkType)
    }
}
