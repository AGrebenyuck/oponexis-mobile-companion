package com.oponexis.companion.domain.model

enum class CallDirection { Incoming, Outgoing }

enum class CallState { Identified, Unknown, Pending }

data class CallPreview(
    val id: String,
    val displayName: String,
    val company: String?,
    val timeLabel: String,
    val direction: CallDirection,
    val state: CallState,
)

data class DashboardSnapshot(
    val employeeName: String,
    val callsToday: Int,
    val identifiedCalls: Int,
    val pendingEvents: Int,
    val recentCalls: List<CallPreview>,
)

data class DiagnosticSnapshot(
    val appState: String,
    val localDatabase: String,
    val backgroundSync: String,
    val queuedEvents: Int,
    val lastCheckLabel: String,
)

enum class ThemePreference { System, Light, Dark }

data class UserPreferences(
    val onboardingComplete: Boolean = false,
    val themePreference: ThemePreference = ThemePreference.System,
    val diagnosticDetailsEnabled: Boolean = true,
)

