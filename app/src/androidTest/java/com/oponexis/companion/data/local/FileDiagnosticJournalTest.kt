package com.oponexis.companion.data.local

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.oponexis.companion.domain.model.DiagnosticCategory
import com.oponexis.companion.domain.model.DiagnosticOutcome
import com.oponexis.companion.domain.model.DiagnosticSeverity
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class FileDiagnosticJournalTest {
    @Test
    fun persistsBoundedStructuredEventsAndRejectsUnsafeFields() = runTest {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val journal = FileDiagnosticJournal(context)
        journal.clear()

        repeat(205) { index ->
            journal.record(
                severity = DiagnosticSeverity.Warning,
                category = DiagnosticCategory.SmsGateway,
                outcome = DiagnosticOutcome.Failed,
                reasonCode = if (index == 204) "phone=+48123456789" else "network",
                correlationId = if (index == 204) "unsafe value" else "event-$index",
            )
        }

        val events = FileDiagnosticJournal(context).events.first()
        assertEquals(200, events.size)
        assertNull(events.first().reasonCode)
        assertNull(events.first().correlationId)
        journal.clear()
    }
}
