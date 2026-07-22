package com.oponexis.companion.domain.repository

import com.oponexis.companion.domain.model.CallPreview
import com.oponexis.companion.domain.model.DashboardSnapshot
import com.oponexis.companion.domain.model.DiagnosticSnapshot
import kotlinx.coroutines.flow.StateFlow

interface CompanionRepository {
    val dashboard: StateFlow<DashboardSnapshot>
    val calls: StateFlow<List<CallPreview>>
    val diagnostics: StateFlow<DiagnosticSnapshot>
}

