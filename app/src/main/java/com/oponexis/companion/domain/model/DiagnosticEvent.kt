package com.oponexis.companion.domain.model

data class DiagnosticEvent(
    val id: String,
    val occurredAtEpochMillis: Long,
    val severity: DiagnosticSeverity,
    val category: DiagnosticCategory,
    val outcome: DiagnosticOutcome,
    val reasonCode: String? = null,
    val correlationId: String? = null,
)

enum class DiagnosticSeverity { Info, Warning, Error }

enum class DiagnosticCategory {
    SmsGateway,
    SmsSend,
    CallerLookup,
    CallEventSync,
    PushSync,
}

enum class DiagnosticOutcome { Succeeded, Failed }
