package com.oponexis.companion.domain.repository

import com.oponexis.companion.domain.model.DiagnosticCategory
import com.oponexis.companion.domain.model.DiagnosticEvent
import com.oponexis.companion.domain.model.DiagnosticOutcome
import com.oponexis.companion.domain.model.DiagnosticSeverity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf

interface DiagnosticJournal {
    val events: Flow<List<DiagnosticEvent>>

    suspend fun record(
        severity: DiagnosticSeverity,
        category: DiagnosticCategory,
        outcome: DiagnosticOutcome,
        reasonCode: String? = null,
        correlationId: String? = null,
    )

    suspend fun clear()
}

object NoOpDiagnosticJournal : DiagnosticJournal {
    override val events: Flow<List<DiagnosticEvent>> = flowOf(emptyList())

    override suspend fun record(
        severity: DiagnosticSeverity,
        category: DiagnosticCategory,
        outcome: DiagnosticOutcome,
        reasonCode: String?,
        correlationId: String?,
    ) = Unit

    override suspend fun clear() = Unit
}
