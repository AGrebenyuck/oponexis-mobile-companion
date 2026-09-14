package com.oponexis.companion.platform.calllifecycle

import android.telecom.DisconnectCause
import android.telecom.TelecomManager
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Test

class PostCallDiagnosticsTest {
    @After
    fun tearDown() {
        PostCallDiagnostics.resetForTest()
    }

    @Test
    fun `record maps approved categorical fields`() {
        PostCallDiagnostics.record(
            disconnectCause = DisconnectCause.REMOTE,
            duration = TelecomManager.DURATION_SHORT,
        )

        assertEquals(
            PostCallSnapshot(
                observationCount = 1,
                lastDisconnectCategory = PostCallDisconnectCategory.Remote,
                lastDurationBucket = PostCallDurationBucket.Short,
            ),
            PostCallDiagnostics.snapshot.value,
        )
    }

    @Test
    fun `unknown values remain explicit`() {
        PostCallDiagnostics.record(disconnectCause = Int.MAX_VALUE, duration = -1)

        assertEquals(PostCallDisconnectCategory.Other, PostCallDiagnostics.snapshot.value.lastDisconnectCategory)
        assertEquals(PostCallDurationBucket.Unknown, PostCallDiagnostics.snapshot.value.lastDurationBucket)
    }

    @Test
    fun `repeated observations increment without retaining call identity`() {
        PostCallDiagnostics.record(DisconnectCause.MISSED, TelecomManager.DURATION_VERY_SHORT)
        PostCallDiagnostics.record(DisconnectCause.LOCAL, TelecomManager.DURATION_MEDIUM)

        assertEquals(2, PostCallDiagnostics.snapshot.value.observationCount)
        assertEquals(PostCallDisconnectCategory.Local, PostCallDiagnostics.snapshot.value.lastDisconnectCategory)
        assertEquals(PostCallDurationBucket.Medium, PostCallDiagnostics.snapshot.value.lastDurationBucket)
    }
}
