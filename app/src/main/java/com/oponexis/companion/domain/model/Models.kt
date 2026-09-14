package com.oponexis.companion.domain.model

enum class CallDirection { Incoming, Outgoing }

enum class CallState { Identified, Unknown, Pending }

data class CallPreview(
    val id: String,
    val displayName: String,
    val phoneNumber: String?,
    val company: String?,
    val smsReceiptId: String?,
    val smsReceiptType: String?,
    val smsDeliveryStatus: String?,
    val smsDeliveryDetail: String?,
    val observedAtEpochMillis: Long,
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

data class SmsActivityItem(
    val id: String,
    val providerMessageId: String?,
    val status: String,
    val source: String,
    val phoneNumber: String?,
    val detail: String?,
    val occurredAtEpochMillis: Long,
)

enum class ThemePreference { System, Light, Dark }

enum class HistoryRetention { ThirtyDays, Forever }

enum class UiLanguage { Polish, English, Ukrainian }

data class UserPreferences(
    val onboardingComplete: Boolean = false,
    val themePreference: ThemePreference = ThemePreference.System,
    val diagnosticDetailsEnabled: Boolean = true,
    val historyRetention: HistoryRetention = HistoryRetention.ThirtyDays,
    val uiLanguage: UiLanguage = UiLanguage.Polish,
    val customConversationTopics: Set<String> = emptySet(),
)
