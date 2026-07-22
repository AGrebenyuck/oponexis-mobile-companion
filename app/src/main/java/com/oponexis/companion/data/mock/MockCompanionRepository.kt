package com.oponexis.companion.data.mock

import com.oponexis.companion.domain.model.CallDirection
import com.oponexis.companion.domain.model.CallPreview
import com.oponexis.companion.domain.model.CallState
import com.oponexis.companion.domain.model.DashboardSnapshot
import com.oponexis.companion.domain.model.DiagnosticSnapshot
import com.oponexis.companion.domain.repository.CompanionRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class MockCompanionRepository @Inject constructor() : CompanionRepository {
    private val mockCalls = listOf(
        CallPreview("call-1", "Anna Kowalska", "Northstar Studio", "10:42", CallDirection.Incoming, CallState.Identified),
        CallPreview("call-2", "Unknown caller", null, "09:18", CallDirection.Incoming, CallState.Unknown),
        CallPreview("call-3", "Marek Nowak", "Orion Logistics", "Yesterday", CallDirection.Outgoing, CallState.Identified),
        CallPreview("call-4", "Sofia Martin", "Helio Partners", "Mon", CallDirection.Incoming, CallState.Pending),
    )

    override val calls: StateFlow<List<CallPreview>> = MutableStateFlow(mockCalls)

    override val dashboard: StateFlow<DashboardSnapshot> = MutableStateFlow(
        DashboardSnapshot(
            employeeName = "Alex",
            callsToday = 12,
            identifiedCalls = 9,
            pendingEvents = 0,
            recentCalls = mockCalls.take(3),
        ),
    )

    override val diagnostics: StateFlow<DiagnosticSnapshot> = MutableStateFlow(
        DiagnosticSnapshot(
            appState = "Ready",
            localDatabase = "Available",
            backgroundSync = "Not configured",
            queuedEvents = 0,
            lastCheckLabel = "Just now",
        ),
    )
}
