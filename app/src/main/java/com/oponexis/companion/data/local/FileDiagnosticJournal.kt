package com.oponexis.companion.data.local

import android.content.Context
import android.util.AtomicFile
import com.oponexis.companion.domain.model.DiagnosticCategory
import com.oponexis.companion.domain.model.DiagnosticEvent
import com.oponexis.companion.domain.model.DiagnosticOutcome
import com.oponexis.companion.domain.model.DiagnosticSeverity
import com.oponexis.companion.domain.repository.DiagnosticJournal
import dagger.hilt.android.qualifiers.ApplicationContext
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import org.json.JSONObject

@Singleton
class FileDiagnosticJournal @Inject constructor(
    @ApplicationContext context: Context,
) : DiagnosticJournal {
    private val file = AtomicFile(context.filesDir.resolve(FILE_NAME))
    private val mutex = Mutex()
    private val mutableEvents = MutableStateFlow(readEvents())
    override val events = mutableEvents.asStateFlow()

    override suspend fun record(
        severity: DiagnosticSeverity,
        category: DiagnosticCategory,
        outcome: DiagnosticOutcome,
        reasonCode: String?,
        correlationId: String?,
    ) = mutex.withLock {
        val event = DiagnosticEvent(
            id = UUID.randomUUID().toString(),
            occurredAtEpochMillis = System.currentTimeMillis(),
            severity = severity,
            category = category,
            outcome = outcome,
            reasonCode = reasonCode.safeCode(),
            correlationId = correlationId.safeCorrelationId(),
        )
        val updated = (listOf(event) + mutableEvents.value).take(MAX_EVENTS)
        if (writeEvents(updated)) mutableEvents.value = updated
    }

    override suspend fun clear() = mutex.withLock {
        file.delete()
        mutableEvents.value = emptyList()
    }

    private fun readEvents(): List<DiagnosticEvent> = runCatching {
        if (!file.baseFile.exists()) return emptyList()
        file.readFully()
            .toString(Charsets.UTF_8)
            .lineSequence()
            .filter(String::isNotBlank)
            .mapNotNull(::eventFromJson)
            .take(MAX_EVENTS)
            .toList()
    }.getOrDefault(emptyList())

    private fun writeEvents(events: List<DiagnosticEvent>): Boolean {
        val bytes = events.joinToString("\n") { it.toJson().toString() }.toByteArray()
        val stream = runCatching(file::startWrite).getOrNull() ?: return false
        return try {
            stream.write(bytes)
            file.finishWrite(stream)
            true
        } catch (_: Exception) {
            file.failWrite(stream)
            false
        }
    }

    private companion object {
        const val FILE_NAME = "diagnostic-journal.jsonl"
        const val MAX_EVENTS = 200
        val SAFE_CODE = Regex("^[a-zA-Z0-9_.-]{1,80}$")
        val SAFE_CORRELATION_ID = Regex("^[a-zA-Z0-9-]{1,64}$")
    }

    private fun String?.safeCode(): String? = this?.takeIf(SAFE_CODE::matches)
    private fun String?.safeCorrelationId(): String? = this?.takeIf(SAFE_CORRELATION_ID::matches)
}

private fun DiagnosticEvent.toJson() = JSONObject()
    .put("id", id)
    .put("occurredAt", occurredAtEpochMillis)
    .put("severity", severity.name)
    .put("category", category.name)
    .put("outcome", outcome.name)
    .put("reasonCode", reasonCode ?: JSONObject.NULL)
    .put("correlationId", correlationId ?: JSONObject.NULL)

private fun eventFromJson(line: String): DiagnosticEvent? = runCatching {
    val json = JSONObject(line)
    DiagnosticEvent(
        id = json.getString("id"),
        occurredAtEpochMillis = json.getLong("occurredAt"),
        severity = DiagnosticSeverity.valueOf(json.getString("severity")),
        category = DiagnosticCategory.valueOf(json.getString("category")),
        outcome = DiagnosticOutcome.valueOf(json.getString("outcome")),
        reasonCode = if (json.isNull("reasonCode")) null else json.optString("reasonCode").takeIf(String::isNotBlank),
        correlationId = if (json.isNull("correlationId")) null else json.optString("correlationId").takeIf(String::isNotBlank),
    )
}.getOrNull()
