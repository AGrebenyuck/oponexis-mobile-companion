package com.oponexis.companion.data.local

import com.oponexis.companion.domain.model.CallDirection
import com.oponexis.companion.domain.model.CallPreview
import com.oponexis.companion.domain.model.CallState
import com.oponexis.companion.domain.model.DashboardSnapshot
import com.oponexis.companion.domain.model.DiagnosticSnapshot
import com.oponexis.companion.domain.model.HistoryRetention
import com.oponexis.companion.domain.model.SmsActivityItem
import com.oponexis.companion.domain.repository.CompanionRepository
import com.oponexis.companion.domain.repository.SettingsRepository
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

@Singleton
class RoomCompanionRepository @Inject constructor(
    private val callDao: CallOutcomeDao,
    private val outboxDao: EventOutboxDao,
    smsActivityDao: SmsActivityDao,
    settingsRepository: SettingsRepository,
) : CompanionRepository {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val source = combine(callDao.observeHistory(), outboxDao.observeAll(), ::RepositoryData)

    override val calls: StateFlow<List<CallPreview>> = source
        .map { data -> data.calls.map { it.toPreview(data.outboxByCallRef[it.callRef]) } }
        .stateIn(scope, SharingStarted.Eagerly, emptyList())

    override val dashboard: StateFlow<DashboardSnapshot> = source
        .map { data ->
            val previews = data.calls.map { it.toPreview(data.outboxByCallRef[it.callRef]) }
            val today = LocalDate.now()
            DashboardSnapshot(
                employeeName = "",
                callsToday = data.calls.count { it.observedDate() == today },
                identifiedCalls = data.calls.count { it.customerRef != null },
                pendingEvents = data.outbox.count { it.status != OutboxStatus.Delivered.persistedValue },
                recentCalls = previews.take(3),
            )
        }
        .stateIn(scope, SharingStarted.Eagerly, DashboardSnapshot("", 0, 0, 0, emptyList()))

    override val diagnostics: StateFlow<DiagnosticSnapshot> = source
        .map { data ->
            val queued = data.outbox.count {
                it.status != OutboxStatus.Delivered.persistedValue &&
                    it.status != OutboxStatus.PermanentFailure.persistedValue
            }
            val failures = data.outbox.count { it.status == OutboxStatus.PermanentFailure.persistedValue }
            DiagnosticSnapshot(
                appState = if (failures == 0) "Ready" else "Action required",
                localDatabase = "Available",
                backgroundSync = when {
                    failures > 0 -> "$failures permanent failure(s)"
                    queued > 0 -> "Waiting to sync"
                    else -> "Up to date"
                },
                queuedEvents = queued,
                lastCheckLabel = "Live local status",
            )
        }
        .stateIn(
            scope,
            SharingStarted.Eagerly,
            DiagnosticSnapshot("Ready", "Available", "Up to date", 0, "Live local status"),
        )

    override val smsActivity: StateFlow<List<SmsActivityItem>> = smsActivityDao.observeRecent()
        .map { events ->
            val now = System.currentTimeMillis()
            events
                .filter { event ->
                    val visibleFor = if (event.status == "QUEUED") {
                        SMS_QUEUED_VISIBLE_MILLIS
                    } else {
                        SMS_ACTIVITY_VISIBLE_MILLIS
                    }
                    event.occurredAtEpochMillis >= now - visibleFor
                }
                .distinctBy { it.providerMessageId ?: it.id }
                .take(8)
                .map { event ->
                    SmsActivityItem(
                        id = event.id,
                        providerMessageId = event.providerMessageId,
                        status = event.status,
                        source = event.source,
                        phoneNumber = event.phoneNumber,
                        detail = event.detail,
                        occurredAtEpochMillis = event.occurredAtEpochMillis,
                    )
                }
        }
        .stateIn(scope, SharingStarted.Eagerly, emptyList())

    init {
        scope.launch {
            settingsRepository.preferences.collect { preferences ->
                if (preferences.historyRetention == HistoryRetention.ThirtyDays) {
                    callDao.deleteResolvedBefore(System.currentTimeMillis() - THIRTY_DAYS_MILLIS)
                }
            }
        }
    }

    private data class RepositoryData(
        val calls: List<CallOutcomeEntity>,
        val outbox: List<EventOutboxEntity>,
    ) {
        val outboxByCallRef = outbox.associateBy(EventOutboxEntity::callRef)
    }

    private companion object {
        const val THIRTY_DAYS_MILLIS = 30L * 24 * 60 * 60 * 1_000
        const val SMS_ACTIVITY_VISIBLE_MILLIS = 4L * 60 * 60 * 1_000
        const val SMS_QUEUED_VISIBLE_MILLIS = 10L * 60 * 1_000
    }
}

private fun CallOutcomeEntity.observedDate(): LocalDate =
    Instant.ofEpochMilli(observedAtEpochMillis).atZone(ZoneId.systemDefault()).toLocalDate()

private fun CallOutcomeEntity.toPreview(outbox: EventOutboxEntity?): CallPreview {
    val phone = historyPhoneNumber ?: phoneNumber
    val name = historyDisplayName ?: displayName ?: phone ?: "Unknown caller"
    val outcome = outcomeCode?.replace('_', ' ')?.replaceFirstChar(Char::uppercase)
        ?: if (dismissedAtEpochMillis != null) "Outcome skipped" else "Awaiting outcome"
    val sync = when (outbox?.status) {
        OutboxStatus.Delivered.persistedValue -> "Synced"
        OutboxStatus.PermanentFailure.persistedValue -> "Sync failed"
        null -> null
        else -> "Waiting to sync"
    }
    return CallPreview(
        id = callRef,
        displayName = name,
        phoneNumber = phone,
        company = listOfNotNull(if (isReturningCustomer) "Returning customer" else null, outcome, sync).joinToString(" · "),
        smsReceiptId = smsReceiptId,
        smsReceiptType = smsReceiptType,
        smsDeliveryStatus = smsDeliveryStatus,
        smsDeliveryDetail = smsDeliveryDetail,
        observedAtEpochMillis = observedAtEpochMillis,
        direction = runCatching { CallDirection.valueOf(direction) }.getOrDefault(CallDirection.Incoming),
        state = when {
            outbox != null && outbox.status != OutboxStatus.Delivered.persistedValue -> CallState.Pending
            customerRef != null -> CallState.Identified
            else -> CallState.Unknown
        },
    )
}
