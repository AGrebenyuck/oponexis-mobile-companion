package com.oponexis.companion.ui.screen.calls

import android.Manifest
import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.content.pm.PackageManager
import android.content.Intent
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.CloudOff
import androidx.compose.material.icons.rounded.PersonSearch
import androidx.compose.material.icons.rounded.PhoneInTalk
import androidx.compose.material.icons.rounded.Warning
import androidx.compose.material.icons.rounded.NotificationsActive
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material3.Button
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.Checkbox
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import androidx.core.net.toUri
import com.oponexis.companion.domain.model.CallPreview
import com.oponexis.companion.domain.model.CallOutcomeCode
import com.oponexis.companion.domain.model.CallerCardState
import com.oponexis.companion.domain.model.CallerLookupFailure
import com.oponexis.companion.domain.callers.CallerCardStore
import com.oponexis.companion.domain.repository.CompanionRepository
import com.oponexis.companion.domain.repository.CallOutcomeRepository
import com.oponexis.companion.domain.model.PendingCallOutcome
import com.oponexis.companion.domain.model.SmsActionResult
import com.oponexis.companion.domain.model.SmsGatewayReadiness
import com.oponexis.companion.domain.model.SmsTemplate
import com.oponexis.companion.domain.model.SmsTemplateResult
import com.oponexis.companion.domain.model.SmsActivityItem
import com.oponexis.companion.domain.model.CallerIdentity
import com.oponexis.companion.domain.model.CallerLookupResult
import com.oponexis.companion.platform.notifications.CallOutcomeNotificationManager
import com.oponexis.companion.platform.sync.MobilePushSyncScheduler
import com.oponexis.companion.domain.repository.SmsActionRepository
import com.oponexis.companion.domain.repository.SettingsRepository
import com.oponexis.companion.domain.callers.CallerLookupCoordinator
import com.oponexis.companion.ui.components.CallRow
import com.oponexis.companion.ui.localization.LocalUiLanguage
import com.oponexis.companion.ui.localization.text
import com.oponexis.companion.domain.model.UiLanguage
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.async
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale
import javax.inject.Inject

@HiltViewModel
class CallsViewModel @Inject constructor(
    repository: CompanionRepository,
    private val callerCardStore: CallerCardStore,
    private val callOutcomeRepository: CallOutcomeRepository,
    private val outcomeNotifications: CallOutcomeNotificationManager,
    private val smsActions: SmsActionRepository,
    private val callerLookup: CallerLookupCoordinator,
    private val mobilePushSync: MobilePushSyncScheduler,
    private val settingsRepository: SettingsRepository,
) : ViewModel() {
    val calls: StateFlow<List<CallPreview>> = repository.calls
    val smsActivity: StateFlow<List<SmsActivityItem>> = repository.smsActivity
    val callerCard: StateFlow<CallerCardState> = callerCardStore.state
    val pendingOutcomes: StateFlow<List<PendingCallOutcome>> = callOutcomeRepository
        .observePending()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())
    val smsStatus = MutableStateFlow<String?>(null)
    val smsComposer = MutableStateFlow(SmsComposerState())
    val preferences = settingsRepository.preferences
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), com.oponexis.companion.domain.model.UserPreferences())

    fun clearCallerCard() = callerCardStore.clear()

    fun selectOutcome(callRef: String, outcome: CallOutcomeCode, topicLabel: String? = null) {
        viewModelScope.launch {
            if (callOutcomeRepository.select(callRef, outcome, topicLabel)) {
                outcomeNotifications.cancel(callRef)
            }
        }
    }

    fun dismissOutcome(callRef: String) {
        viewModelScope.launch {
            if (callOutcomeRepository.dismiss(callRef)) {
                outcomeNotifications.cancel(callRef)
            }
        }
    }

    fun dismissAllOutcomes(pending: List<PendingCallOutcome>) {
        viewModelScope.launch {
            pending.forEach { outcome ->
                if (callOutcomeRepository.dismiss(outcome.callRef)) {
                    outcomeNotifications.cancel(outcome.callRef)
                }
            }
        }
    }

    fun addConversationTopic(topic: String) {
        viewModelScope.launch { settingsRepository.addConversationTopic(topic) }
    }

    fun removeConversationTopic(topic: String) {
        viewModelScope.launch { settingsRepository.removeConversationTopic(topic) }
    }

    fun sendBookingSms(pending: PendingCallOutcome, visitDate: String, visitTime: String, messageOverride: String?) {
        if (smsStatus.value == "Sending SMS…") return
        viewModelScope.launch {
            smsStatus.value = "Sending SMS…"
            handleSmsResult(pending, smsActions.sendBookingForm(pending, visitDate, visitTime, messageOverride))
        }
    }

    fun sendCustomSms(pending: PendingCallOutcome, message: String) {
        if (smsStatus.value == "Sending SMS…") return
        viewModelScope.launch {
            smsStatus.value = "Sending SMS…"
            handleSmsResult(pending, smsActions.sendCustomMessage(pending, message))
        }
    }

    fun prepareSmsComposer(pending: PendingCallOutcome) {
        viewModelScope.launch {
            val initialAudience = if (pending.isReturningCustomer) "RETURNING" else "NEW"
            val initialTemplates = when (val result = smsActions.templates(initialAudience)) {
                is SmsTemplateResult.Loaded -> result.templates
                is SmsTemplateResult.Created -> listOf(result.template)
                is SmsTemplateResult.Updated -> listOf(result.template)
                is SmsTemplateResult.Failed -> emptyList()
            }
            smsComposer.value = SmsComposerState(templates = initialTemplates)
            val gatewayReadiness = async { smsActions.gatewayReadiness() }
            val identityLookup = async {
                pending.phoneNumber?.let { phone ->
                    when (val lookup = callerLookup.lookup(phone)) {
                        is CallerLookupResult.Matched -> lookup.identity.also { matched ->
                            callOutcomeRepository.attachIdentityForPhone(
                                phoneNumber = phone,
                                displayName = matched.displayName,
                                customerRef = matched.customerRef,
                                isReturningCustomer = matched.isReturningCustomer,
                            )
                        }
                        CallerLookupResult.NotFound -> {
                            callOutcomeRepository.clearIdentityForPhone(phone)
                            null
                        }
                        else -> null
                    }
                }
            }
            val readiness = gatewayReadiness.await()
            smsComposer.value = smsComposer.value.copy(gatewayReadiness = readiness)
            val identity = identityLookup.await()
            val audience = if (identity?.isReturningCustomer == true) "RETURNING" else "NEW"
            smsComposer.value = when (val result = smsActions.templates(audience)) {
                is SmsTemplateResult.Loaded -> SmsComposerState(identity = identity, templates = result.templates, gatewayReadiness = readiness)
                is SmsTemplateResult.Failed -> SmsComposerState(identity = identity, error = result.detail, gatewayReadiness = readiness)
                is SmsTemplateResult.Created -> SmsComposerState(identity = identity, templates = listOf(result.template), gatewayReadiness = readiness)
				is SmsTemplateResult.Updated -> SmsComposerState(identity = identity, templates = listOf(result.template), gatewayReadiness = readiness)
            }
        }
    }

    fun refreshGatewayReadiness() {
        val current = smsComposer.value
        viewModelScope.launch {
            smsComposer.value = current.copy(gatewayReadiness = SmsGatewayReadiness.Checking)
            smsComposer.value = smsComposer.value.copy(gatewayReadiness = smsActions.gatewayReadiness())
        }
    }

    fun createSmsTemplate(name: String, body: String, kind: String) {
        val current = smsComposer.value
        val audience = if (current.identity?.isReturningCustomer == true) "RETURNING" else "NEW"
        viewModelScope.launch {
            when (val result = smsActions.createTemplate(name, body, kind, audience)) {
                is SmsTemplateResult.Created -> smsComposer.value = current.copy(
                    templates = current.templates + result.template,
                    createdTemplateId = result.template.id,
                    error = null,
                )
                is SmsTemplateResult.Failed -> smsComposer.value = current.copy(error = result.detail)
                is SmsTemplateResult.Loaded -> Unit
				is SmsTemplateResult.Updated -> Unit
            }
        }
    }

	fun updateSmsTemplate(template: SmsTemplate, body: String) {
		val current = smsComposer.value
		viewModelScope.launch {
			when (val result = smsActions.updateTemplate(
				id = template.id,
				name = template.name,
				body = body,
				kind = template.kind,
				audience = template.audience,
			)) {
				is SmsTemplateResult.Updated -> smsComposer.value = current.copy(
					templates = current.templates.filterNot { it.id == template.id } + result.template,
					createdTemplateId = result.template.id,
					error = null,
				)
				is SmsTemplateResult.Failed -> smsComposer.value = current.copy(error = result.detail)
				is SmsTemplateResult.Loaded, is SmsTemplateResult.Created -> Unit
			}
		}
	}

    fun refreshSmsStatus(call: CallPreview) {
        val receiptId = call.smsReceiptId ?: return
        val receiptType = call.smsReceiptType ?: return
        viewModelScope.launch { refreshSmsStatus(call.id, receiptId, receiptType) }
    }

    private suspend fun handleSmsResult(pending: PendingCallOutcome, result: SmsActionResult) {
        when (result) {
            is SmsActionResult.Sent -> {
                callOutcomeRepository.recordSmsReceipt(
                    callRef = pending.callRef,
                    receiptId = result.receiptId,
                    receiptType = result.receiptType,
                    status = "QUEUED",
                )
                mobilePushSync.trackSmsReceipt(
                    callRef = pending.callRef,
                    receiptId = result.receiptId,
                    receiptType = result.receiptType,
                )
                smsStatus.value = "SMS queued for sending."
                if (callOutcomeRepository.select(pending.callRef, CallOutcomeCode.Interested)) {
                    outcomeNotifications.cancel(pending.callRef)
                }
                mobilePushSync.syncActivity()
            }
            SmsActionResult.InvalidPhone -> setSmsFailure(pending.callRef, "No valid phone number for this call.")
            SmsActionResult.NotConfigured -> setSmsFailure(pending.callRef, "CRM is not configured for this build.")
            SmsActionResult.Unauthorized -> setSmsFailure(pending.callRef, "CRM authorization failed.")
            is SmsActionResult.Failed -> {
                setSmsFailure(pending.callRef, result.detail ?: "The SMS channel rejected the message. Try again.")
            }
        }
    }

    private suspend fun setSmsFailure(callRef: String, detail: String) {
        smsStatus.value = detail
        callOutcomeRepository.recordSmsFailure(callRef, detail)
    }

    private suspend fun refreshSmsStatus(callRef: String, receiptId: String, receiptType: String): String? {
        val status = smsActions.deliveryStatus(receiptId, receiptType) ?: return null
        callOutcomeRepository.updateSmsDeliveryStatus(callRef, status.status, status.detail)
        return status.status
    }
}

data class SmsComposerState(
    val loading: Boolean = false,
    val identity: CallerIdentity? = null,
    val templates: List<SmsTemplate> = emptyList(),
    val createdTemplateId: String? = null,
    val error: String? = null,
    val gatewayReadiness: SmsGatewayReadiness = SmsGatewayReadiness.Checking,
)

@Composable
fun CallsRoute(
    contentPadding: PaddingValues,
    openSmsSchedulerRequested: Boolean = false,
    onOpenSmsSchedulerHandled: () -> Unit = {},
    viewModel: CallsViewModel = hiltViewModel(),
) {
    val calls by viewModel.calls.collectAsStateWithLifecycle()
    val smsActivity by viewModel.smsActivity.collectAsStateWithLifecycle()
    val callerCard by viewModel.callerCard.collectAsStateWithLifecycle()
    val pendingOutcomes by viewModel.pendingOutcomes.collectAsStateWithLifecycle()
    val smsStatus by viewModel.smsStatus.collectAsStateWithLifecycle()
    val smsComposer by viewModel.smsComposer.collectAsStateWithLifecycle()
    val preferences by viewModel.preferences.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val language = LocalUiLanguage.current
    var notificationAccessGranted by remember {
        mutableStateOf(
            Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
                context.checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) ==
                PackageManager.PERMISSION_GRANTED,
        )
    }
    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
        onResult = { notificationAccessGranted = it },
    )
    var query by rememberSaveable { mutableStateOf("") }
    var smsCallRef by rememberSaveable { mutableStateOf<String?>(null) }
    var selectedHistoryCallRef by rememberSaveable { mutableStateOf<String?>(null) }
    val followUpCalls = calls.filter { it.company?.startsWith("Follow up required") == true }
    val filteredCallGroups = groupConsecutiveCalls(calls).filter { group ->
        query.isBlank() || group.calls.any {
            it.displayName.contains(query, ignoreCase = true) ||
                it.company?.contains(query, ignoreCase = true) == true
        }
    }

    LaunchedEffect(openSmsSchedulerRequested, pendingOutcomes) {
        if (openSmsSchedulerRequested) {
            pendingOutcomes.firstOrNull()?.let { pending ->
                smsCallRef = pending.callRef
                onOpenSmsSchedulerHandled()
            }
        }
    }

    LaunchedEffect(selectedHistoryCallRef) {
        selectedHistoryCallRef
            ?.let { callRef -> calls.firstOrNull { it.id == callRef } }
            ?.let(viewModel::refreshSmsStatus)
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(
            start = 20.dp,
            top = contentPadding.calculateTopPadding() + 24.dp,
            end = 20.dp,
            bottom = contentPadding.calculateBottomPadding() + 24.dp,
        ),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            Text(language.text("Połączenia", "Calls", "Дзвінки"), style = MaterialTheme.typography.headlineMedium)
            Text(
                language.text(
                    "Kontekst z CRM pojawia się tutaj po rozpoczęciu połączenia.",
                    "Incoming CRM context appears here after Android has already allowed the call.",
                    "Контекст із CRM з’являється тут після початку дзвінка.",
                ),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 4.dp, bottom = 16.dp),
            )
            if (!notificationAccessGranted) {
                OutcomeNotificationPermissionCard(
                    language = language,
                    onEnable = {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                            notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                        }
                    },
                    modifier = Modifier.padding(bottom = 12.dp),
                )
            }
            pendingOutcomes.firstOrNull()?.let { pending ->
                CallOutcomeCard(
                    pending = pending,
                    pendingCount = pendingOutcomes.size,
                    onSelect = { viewModel.selectOutcome(pending.callRef, it) },
                    onSelectTopic = { outcome, topicLabel -> viewModel.selectOutcome(pending.callRef, outcome, topicLabel) },
                    customTopics = preferences.customConversationTopics.sorted(),
                    onSaveTopic = viewModel::addConversationTopic,
                    onDeleteTopic = viewModel::removeConversationTopic,
                    onSendSms = { smsCallRef = pending.callRef },
                    onDismiss = { viewModel.dismissOutcome(pending.callRef) },
                    onDismissAll = { viewModel.dismissAllOutcomes(pendingOutcomes) },
                    smsStatus = smsStatus,
                    language = language,
                    modifier = Modifier.padding(bottom = 12.dp),
                )
            }
            CallerCard(
                state = callerCard,
                onClear = viewModel::clearCallerCard,
                modifier = Modifier.padding(bottom = 20.dp),
            )
            if (smsActivity.isNotEmpty()) {
                SmsActivityCard(
                    activity = smsActivity.take(3),
                    language = language,
                    modifier = Modifier.padding(bottom = 20.dp),
                )
            }
            if (followUpCalls.isNotEmpty()) {
                Text(language.text("Do ponownego kontaktu", "Follow-ups required", "Потрібен повторний контакт"), style = MaterialTheme.typography.titleMedium)
                Text(
                    language.text("Z tymi klientami trzeba skontaktować się ponownie.", "These clients still need a call back.", "З цими клієнтами потрібно зв’язатися повторно."),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 4.dp, bottom = 8.dp),
                )
                followUpCalls.forEach { call ->
                    CallRow(call, onClick = { selectedHistoryCallRef = call.id })
                }
            }
            Text(
                language.text("Historia połączeń", "Call history", "Історія дзвінків"),
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(bottom = 10.dp),
            )
            OutlinedTextField(
                value = query,
                onValueChange = { query = it },
                modifier = Modifier.fillParentMaxWidth(),
                placeholder = { Text(language.text("Szukaj połączeń", "Search calls", "Пошук дзвінків")) },
                leadingIcon = { Icon(Icons.Rounded.Search, contentDescription = null) },
                singleLine = true,
                shape = MaterialTheme.shapes.large,
            )
        }
        items(filteredCallGroups, key = { it.representative.id }) { group ->
            CallRow(
                call = group.representative,
                repeatCount = group.count,
                onClick = { selectedHistoryCallRef = group.representative.id },
            )
        }
        if (filteredCallGroups.isEmpty()) {
            item {
                Text(
                    if (calls.isEmpty()) language.text("Brak zapisanych połączeń.", "No calls recorded yet.", "Ще немає збережених дзвінків.")
                    else language.text("Brak wyników wyszukiwania.", "No calls match your search.", "Немає дзвінків за цим запитом."),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(vertical = 28.dp),
                )
            }
        }
    }

    val smsPending = smsCallRef?.let { callRef ->
        pendingOutcomes.firstOrNull { it.callRef == callRef }
            ?: calls.firstOrNull { it.id == callRef }?.toSmsPending()
    }
    LaunchedEffect(smsPending?.callRef) {
        smsPending?.let(viewModel::prepareSmsComposer)
    }
    smsPending
        ?.let { pending ->
            BookingSmsDialog(
                pending = pending,
				composer = smsComposer,
                sending = smsStatus == "Sending SMS…",
                onDismiss = { smsCallRef = null },
                onSend = { visitDate, visitTime, messageOverride ->
                    smsCallRef = null
                    viewModel.sendBookingSms(pending, visitDate, visitTime, messageOverride)
                },
                onSendCustom = { message ->
                    smsCallRef = null
                    viewModel.sendCustomSms(pending, message)
                },
				onCreateTemplate = viewModel::createSmsTemplate,
				onSaveTemplate = viewModel::updateSmsTemplate,
                onRetryGateway = viewModel::refreshGatewayReadiness,
            )
        }

    selectedHistoryCallRef
        ?.let { callRef -> calls.firstOrNull { it.id == callRef } }
        ?.let { call ->
            HistoryActionsSheet(
                call = call,
                pending = pendingOutcomes.firstOrNull { it.callRef == call.id },
                onDismiss = { selectedHistoryCallRef = null },
                onDial = {
                    call.phoneNumber?.let { phone ->
                        context.startActivity(Intent(Intent.ACTION_DIAL, "tel:$phone".toUri()))
                    }
                },
                onSendSms = {
                    selectedHistoryCallRef = null
                    smsCallRef = call.id
                },
                onOpenSmsConversation = {
                    call.phoneNumber?.let { phone ->
                        context.startActivity(Intent(Intent.ACTION_SENDTO, "smsto:$phone".toUri()))
                    }
                },
                onSelectOutcome = { outcome ->
                    selectedHistoryCallRef = null
                    viewModel.selectOutcome(call.id, outcome)
                },
                language = language,
            )
        }
}

@Composable
private fun SmsActivityCard(
    activity: List<SmsActivityItem>,
    language: UiLanguage,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        tonalElevation = 1.dp,
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Text(
                language.text("Aktywność SMS", "SMS activity", "Активність SMS"),
                style = MaterialTheme.typography.titleMedium,
            )
            if (activity.isEmpty()) {
                Text(
                    language.text(
                        "Brak ostatnich wiadomości.",
                        "No recent messages.",
                        "Немає останніх повідомлень.",
                    ),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.bodySmall,
                )
            } else {
                activity.forEach { item ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        val icon = when (item.status) {
                            "SENT", "DELIVERED" -> Icons.Rounded.CheckCircle
                            "FAILED", "CANCELLED" -> Icons.Rounded.Warning
                            else -> Icons.Rounded.NotificationsActive
                        }
                        val color = when (item.status) {
                            "FAILED", "CANCELLED" -> MaterialTheme.colorScheme.error
                            "QUEUED" -> MaterialTheme.colorScheme.tertiary
                            else -> MaterialTheme.colorScheme.primary
                        }
                        Icon(icon, contentDescription = null, tint = color, modifier = Modifier.size(20.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                item.phoneNumber ?: smsSourceLabel(item.source, language),
                                style = MaterialTheme.typography.bodyMedium,
                            )
                            Text(
                                "${smsStatusLabel(item.status, language)} · ${smsSourceLabel(item.source, language)} · ${smsActivityTime(item.occurredAtEpochMillis)}",
                                color = color,
                                style = MaterialTheme.typography.bodySmall,
                            )
                        }
                    }
                }
            }
        }
    }
}

private fun smsStatusLabel(status: String, language: UiLanguage): String = when (status) {
    "QUEUED" -> language.text("W kolejce", "Queued", "У черзі")
    "SENT" -> language.text("Wysłano", "Sent", "Надіслано")
    "DELIVERED" -> language.text("Dostarczono", "Delivered", "Доставлено")
    "FAILED" -> language.text("Błąd", "Failed", "Помилка")
    "CANCELLED" -> language.text("Anulowano", "Cancelled", "Скасовано")
    else -> status
}

private fun smsSourceLabel(source: String, language: UiLanguage): String = when (source) {
    "COMPANION" -> "Companion"
    "BOOKING_FORM" -> language.text("Formularz", "Booking form", "Формуляр")
    "FORM_COMPLETED" -> language.text("Potwierdzenie", "Confirmation", "Підтвердження")
	"APPOINTMENT_CHANGED" -> language.text("Zmiana terminu", "Appointment changed", "Зміна часу")
    "REMINDER" -> language.text("Przypomnienie", "Reminder", "Нагадування")
    "CAMPAIGN" -> language.text("Kampania", "Campaign", "Кампанія")
    else -> language.text("Platforma", "Platform", "Платформа")
}

private fun smsActivityTime(epochMillis: Long): String {
    val dateTime = Instant.ofEpochMilli(epochMillis).atZone(ZoneId.systemDefault())
    val pattern = if (dateTime.toLocalDate() == LocalDate.now()) "HH:mm" else "dd.MM HH:mm"
    return dateTime.format(DateTimeFormatter.ofPattern(pattern))
}

private fun CallPreview.toSmsPending() = PendingCallOutcome(
    callRef = id,
    observedAtEpochMillis = 0L,
    disconnectCategory = "history",
    durationBucket = "unknown",
    phoneNumber = phoneNumber,
    displayName = displayName.takeUnless { it == phoneNumber },
    isReturningCustomer = company?.contains("Returning customer") == true,
)

@Composable
@OptIn(ExperimentalMaterial3Api::class)
private fun HistoryActionsSheet(
    call: CallPreview,
    pending: PendingCallOutcome?,
    onDismiss: () -> Unit,
    onDial: () -> Unit,
    onOpenSmsConversation: () -> Unit,
    onSendSms: () -> Unit,
    onSelectOutcome: (CallOutcomeCode) -> Unit,
    language: UiLanguage,
) {
    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Text(call.displayName, style = MaterialTheme.typography.titleLarge)
            call.phoneNumber?.let { Text(it, color = MaterialTheme.colorScheme.onSurfaceVariant) }
            call.smsDeliveryStatus?.let { status ->
                Text(
                    text = when (status) {
                        "QUEUED" -> language.text("SMS czeka na wysłanie.", "SMS is queued for sending.", "SMS очікує на надсилання.")
                        "SENT" -> language.text("SMS został wysłany przez urządzenie.", "SMS was sent by the device.", "SMS надіслано пристроєм.")
                        "DELIVERED" -> language.text("SMS został dostarczony.", "SMS was delivered.", "SMS доставлено.")
                        "FAILED" -> language.text("Nie wysłano SMS", "SMS was not sent", "SMS не надіслано") + call.smsDeliveryDetail?.let { ": $it" }.orEmpty()
                        "CANCELLED" -> language.text("SMS został anulowany.", "SMS was cancelled.", "SMS скасовано.")
                        else -> language.text("Status SMS: $status", "SMS status: $status", "Статус SMS: $status")
                    },
                    style = MaterialTheme.typography.bodyMedium,
                    color = if (status == "FAILED") MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            if (call.phoneNumber != null) {
                Button(onClick = onDial, modifier = Modifier.fillMaxWidth()) { Text(language.text("Zadzwoń", "Call", "Зателефонувати")) }
                OutlinedButton(onClick = onSendSms, modifier = Modifier.fillMaxWidth()) { Text(language.text("Wyślij SMS", "Send SMS", "Надіслати SMS")) }
                OutlinedButton(onClick = onOpenSmsConversation, modifier = Modifier.fillMaxWidth()) { Text(language.text("Otwórz rozmowę SMS", "Open SMS conversation", "Відкрити SMS-розмову")) }
            }
            Text(
                if (pending != null) language.text("Wynik połączenia", "Call outcome", "Результат дзвінка") else language.text("Zmień wynik połączenia", "Update call outcome", "Змінити результат дзвінка"),
                style = MaterialTheme.typography.labelLarge,
                modifier = Modifier.padding(top = 8.dp),
            )
            CallOutcomeCode.entries.filter { it != CallOutcomeCode.Interested }.forEach { outcome ->
                TextButton(onClick = { onSelectOutcome(outcome) }, modifier = Modifier.fillMaxWidth()) {
                    Text(outcome.localizedLabel(language))
                }
            }
            TextButton(onClick = onDismiss, modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp)) { Text(language.text("Zamknij", "Close", "Закрити")) }
        }
    }
}

@Composable
@OptIn(ExperimentalMaterial3Api::class)
private fun BookingSmsDialog(
    pending: PendingCallOutcome,
    composer: SmsComposerState,
    sending: Boolean,
    onDismiss: () -> Unit,
    onSend: (String, String, String?) -> Unit,
    onSendCustom: (String) -> Unit,
    onCreateTemplate: (String, String, String) -> Unit,
	onSaveTemplate: (SmsTemplate, String) -> Unit,
    onRetryGateway: () -> Unit,
) {
    val context = LocalContext.current
    val language = LocalUiLanguage.current
    val initial = remember(pending.callRef) { LocalDateTime.now() }
    var visitDate by rememberSaveable(pending.callRef) { mutableStateOf(initial.toLocalDate().toString()) }
    var visitTime by rememberSaveable(pending.callRef) {
        mutableStateOf(String.format(Locale.ROOT, "%02d:%02d", initial.hour, initial.minute))
    }
    var mode by rememberSaveable(pending.callRef) { mutableStateOf(SmsComposerMode.Template) }
    var selectedTemplateId by rememberSaveable(pending.callRef) { mutableStateOf<String?>(null) }
    var templateMenuExpanded by remember { mutableStateOf(false) }
    var messageText by rememberSaveable(pending.callRef) { mutableStateOf("") }
    var customMessage by rememberSaveable(pending.callRef) { mutableStateOf("") }
    var showAddTemplate by remember { mutableStateOf(false) }
    val selectedTemplate = composer.templates.firstOrNull { it.id == selectedTemplateId }
        ?: composer.templates.firstOrNull()
    val customerName = composer.identity?.displayName ?: pending.displayName

    LaunchedEffect(composer.templates, composer.createdTemplateId) {
        selectedTemplateId = composer.createdTemplateId
            ?: selectedTemplateId?.takeIf { id -> composer.templates.any { it.id == id } }
            ?: composer.templates.firstOrNull()?.id
    }
    LaunchedEffect(selectedTemplate?.id, visitDate, visitTime, composer.identity, pending.displayName) {
        selectedTemplate?.let { template ->
            messageText = renderSmsPreview(
                template.body,
                customerName,
                composer.identity?.savedDetails,
                visitDate,
                visitTime,
                language,
            )
        }
    }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(language.text("Wyślij SMS", "Send SMS", "Надіслати SMS")) },
		text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp),
			) {
				Text(customerName ?: pending.phoneNumber ?: language.text("Nieznany numer", "Unknown number", "Невідомий номер"))
                SmsGatewayReadinessCard(
                    readiness = composer.gatewayReadiness,
                    onRetry = onRetryGateway,
                    language = language,
                )
                Text(
                    if (composer.identity?.isReturningCustomer == true) {
                        language.text(
                            "Stały klient — dane z poprzedniej wizyty zostaną wczytane do formularza.",
                            "Returning customer — previous visit details will be loaded into the form.",
                            "Постійний клієнт — дані попереднього візиту буде завантажено у форму.",
                        )
                    } else {
                        language.text(
                            "Nowy klient — formularz poprosi również o źródło kontaktu.",
                            "New customer — the form will also ask for the contact source.",
                            "Новий клієнт — форма також запитає джерело звернення.",
                        )
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(
						onClick = { mode = SmsComposerMode.Template },
						enabled = mode != SmsComposerMode.Template,
                        modifier = Modifier.weight(1f),
                    ) { Text(language.text("Szablon", "Template", "Шаблон")) }
                    Button(
						onClick = { mode = SmsComposerMode.Custom },
						enabled = mode != SmsComposerMode.Custom,
                        modifier = Modifier.weight(1f),
                    ) { Text(language.text("Własna", "Custom", "Власне")) }
                }
                if (mode == SmsComposerMode.Template) {
                    if (composer.loading) {
                        CircularProgressIndicator(Modifier.size(28.dp))
                    } else {
                        ExposedDropdownMenuBox(
                            expanded = templateMenuExpanded,
                            onExpandedChange = { templateMenuExpanded = it },
                        ) {
                            OutlinedTextField(
                                value = selectedTemplate?.name ?: language.text("Brak szablonów", "No templates", "Немає шаблонів"),
                                onValueChange = {},
                                readOnly = true,
                                label = { Text(language.text("Szablon", "Template", "Шаблон")) },
                                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(templateMenuExpanded) },
                                modifier = Modifier.fillMaxWidth().menuAnchor(),
                            )
                            ExposedDropdownMenu(
                                expanded = templateMenuExpanded,
                                onDismissRequest = { templateMenuExpanded = false },
                            ) {
                                composer.templates.forEach { template ->
                                    DropdownMenuItem(
                                        text = { Text(template.name) },
                                        onClick = {
                                            selectedTemplateId = template.id
                                            templateMenuExpanded = false
                                        },
                                    )
                                }
                            }
                        }
                        TextButton(onClick = { showAddTemplate = true }) {
                            Text(language.text("+ Dodaj nowy szablon", "+ Add new template", "+ Додати новий шаблон"))
                        }
                    }
                    composer.error?.let {
                        Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                    }
                    OutlinedTextField(
                        value = messageText,
                        onValueChange = { messageText = it.take(1000) },
                        label = { Text(language.text("Treść wiadomości", "Message content", "Текст повідомлення")) },
                        minLines = 6,
                        modifier = Modifier.fillMaxWidth(),
                    )
                    if (selectedTemplate != null) {
                        TextButton(
                            onClick = {
                                onSaveTemplate(
                                    selectedTemplate,
                                    messageText.toStoredSmsTemplate(
										name = customerName,
                                        savedDetails = composer.identity?.savedDetails,
                                        visitDate = visitDate,
                                        visitTime = visitTime,
                                    ),
                                )
                            },
                            enabled = messageText.isNotBlank(),
                        ) {
                            Text(language.text("Zapisz zmiany w szablonie", "Save template changes", "Зберегти зміни шаблону"))
                        }
                    }
                }
                if (mode == SmsComposerMode.Template && selectedTemplate?.kind == "BOOKING_FORM") {
                    OutlinedButton(
                        onClick = {
                            val selected = LocalDate.parse(visitDate)
                            DatePickerDialog(
                                context,
                                { _, year, month, day ->
                                    visitDate = LocalDate.of(year, month + 1, day).toString()
                                },
                                selected.year,
                                selected.monthValue - 1,
                                selected.dayOfMonth,
                            ).apply {
                                datePicker.minDate = System.currentTimeMillis() - 1_000
                            }.show()
                        },
                        modifier = Modifier.fillMaxWidth(),
                    ) { Text(language.text("Data: $visitDate", "Date: $visitDate", "Дата: $visitDate")) }
                    OutlinedButton(
                        onClick = {
                            val parts = visitTime.split(':')
                            TimePickerDialog(
                                context,
                                { _, hour, minute ->
                                    visitTime = String.format(Locale.ROOT, "%02d:%02d", hour, minute)
                                },
                                parts[0].toInt(),
                                parts[1].toInt(),
                                true,
                            ).show()
                        },
                        modifier = Modifier.fillMaxWidth(),
                    ) { Text(language.text("Godzina: $visitTime", "Time: $visitTime", "Час: $visitTime")) }
                    Text(
						language.text("Link do formularza zostanie dodany automatycznie.", "The form link will be added automatically.", "Посилання на форму буде додано автоматично."),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                } else if (mode == SmsComposerMode.Custom) {
                    OutlinedTextField(
                        value = customMessage,
                        onValueChange = { customMessage = it.take(1000) },
						label = { Text(language.text("Treść wiadomości", "Message content", "Текст повідомлення")) },
                        minLines = 3,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
					if (mode == SmsComposerMode.Template && selectedTemplate?.kind == "BOOKING_FORM") {
						onSend(visitDate, visitTime, messageText.toServerSmsTemplate())
					} else if (mode == SmsComposerMode.Template) {
                        onSendCustom(messageText.trim())
                    } else {
                        onSendCustom(customMessage.trim())
                    }
                },
                enabled = !sending &&
					composer.gatewayReadiness is SmsGatewayReadiness.Ready &&
					((mode == SmsComposerMode.Template && selectedTemplate != null && messageText.isNotBlank()) ||
                        (mode == SmsComposerMode.Custom && customMessage.isNotBlank())),
            ) {
				Text(if (sending) language.text("Wysyłanie…", "Sending…", "Надсилання…") else language.text("Wyślij SMS", "Send SMS", "Надіслати SMS"))
            }
        },
		dismissButton = { TextButton(onClick = onDismiss) { Text(language.text("Anuluj", "Cancel", "Скасувати")) } },
    )

    if (showAddTemplate) {
        AddSmsTemplateDialog(
            onDismiss = { showAddTemplate = false },
            onSave = { name, body, kind ->
                onCreateTemplate(name, body.toServerSmsTemplate(), kind)
                showAddTemplate = false
            },
        )
    }
}

@Composable
private fun SmsGatewayReadinessCard(
    readiness: SmsGatewayReadiness,
    onRetry: () -> Unit,
    language: UiLanguage,
) {
    val ready = readiness is SmsGatewayReadiness.Ready
    Surface(
        color = if (ready) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.55f)
        else MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.55f),
        shape = MaterialTheme.shapes.large,
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            if (readiness is SmsGatewayReadiness.Checking) {
                CircularProgressIndicator(Modifier.size(22.dp), strokeWidth = 2.dp)
            } else {
                Icon(
                    imageVector = if (ready) Icons.Rounded.CheckCircle else Icons.Rounded.Warning,
                    contentDescription = null,
                    tint = if (ready) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error,
                )
            }
            Column(Modifier.weight(1f)) {
                Text(
                    when (readiness) {
                        SmsGatewayReadiness.Checking -> language.text("Sprawdzanie kanału SMS…", "Checking SMS channel…", "Перевірка SMS-каналу…")
                        is SmsGatewayReadiness.Ready -> language.text("Kanał SMS gotowy", "SMS channel ready", "SMS-канал готовий")
                        SmsGatewayReadiness.NotConfigured -> language.text("Kanał SMS nie jest skonfigurowany", "SMS channel is not configured", "SMS-канал не налаштований")
                        SmsGatewayReadiness.Unauthorized -> language.text("Brak autoryzacji CRM", "CRM authorization failed", "Помилка авторизації CRM")
                        is SmsGatewayReadiness.Unavailable -> if (readiness.reasonCode == "companion_sms_permission_missing") {
							language.text("Brak uprawnienia do wysyłania SMS", "SMS permission is missing", "Немає дозволу на надсилання SMS")
						} else if (readiness.reasonCode == "companion_sms_sim_not_ready") {
							language.text("Karta SIM nie jest gotowa", "SIM card is not ready", "SIM-картка не готова")
						} else if (readiness.reasonCode == "companion_sms_no_telephony") {
							language.text("Urządzenie nie obsługuje SMS", "This device does not support SMS", "Пристрій не підтримує SMS")
						} else if (readiness.reasonCode == "network") {
							language.text("Brak połączenia z internetem", "No internet connection", "Немає підключення до інтернету")
						} else if (readiness.reasonCode == "sms_gateway_device_offline") {
                            language.text(
                                "Urządzenie SMS Gateway lub Cloud Server jest offline",
                                "SMS Gateway device or Cloud Server is offline",
                                "Пристрій SMS Gateway або Cloud Server офлайн",
                            )
                        } else {
                            language.text("Kanał SMS jest niedostępny", "SMS channel is unavailable", "SMS-канал недоступний")
                        }
                    },
                    style = MaterialTheme.typography.titleSmall,
                )
                if (readiness is SmsGatewayReadiness.Ready) {
                    Text(
                        language.text(
                            "Profil: ${readiness.profile}" + readiness.simNumber?.let { " · SIM $it" }.orEmpty(),
                            "Profile: ${readiness.profile}" + readiness.simNumber?.let { " · SIM $it" }.orEmpty(),
                            "Профіль: ${readiness.profile}" + readiness.simNumber?.let { " · SIM $it" }.orEmpty(),
                        ),
                        style = MaterialTheme.typography.bodySmall,
                    )
                    readiness.phoneNumber?.let { phone ->
                        Text(
                            language.text("Numer: $phone", "Number: $phone", "Номер: $phone"),
                            style = MaterialTheme.typography.bodySmall,
                        )
                    }
                    readiness.deviceName?.let { deviceName ->
                        Text(
                            language.text("Urządzenie: $deviceName", "Device: $deviceName", "Пристрій: $deviceName"),
                            style = MaterialTheme.typography.bodySmall,
                        )
                    }
                }
            }
            if (!ready && readiness !is SmsGatewayReadiness.Checking) {
                TextButton(onClick = onRetry) {
                    Text(language.text("Ponów", "Retry", "Повторити"))
                }
            }
        }
    }
}

@Composable
private fun AddSmsTemplateDialog(
    onDismiss: () -> Unit,
    onSave: (String, String, String) -> Unit,
) {
    val language = LocalUiLanguage.current
    var name by rememberSaveable { mutableStateOf("") }
    var body by rememberSaveable { mutableStateOf("") }
    var kind by rememberSaveable { mutableStateOf("MESSAGE") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(language.text("Nowy szablon", "New template", "Новий шаблон")) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it.take(80) },
                    label = { Text(language.text("Nazwa", "Name", "Назва")) },
                    modifier = Modifier.fillMaxWidth(),
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(
                        onClick = { kind = "MESSAGE" },
                        enabled = kind != "MESSAGE",
                        modifier = Modifier.weight(1f),
                    ) { Text(language.text("Wiadomość", "Message", "Повідомлення")) }
                    Button(
                        onClick = {
                            kind = "BOOKING_FORM"
                            if (!body.containsFormLinkMarker()) body = "$body\nFormularz: ${formLinkMarker(language)}".trim()
                        },
                        enabled = kind != "BOOKING_FORM",
                        modifier = Modifier.weight(1f),
                    ) { Text(language.text("Formularz", "Form", "Форма")) }
                }
                OutlinedTextField(
                    value = body,
                    onValueChange = { body = it.take(1000) },
                    label = { Text(language.text("Treść", "Content", "Текст")) },
                    minLines = 5,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { onSave(name.trim(), body.trim(), kind) },
                enabled = name.isNotBlank() && body.isNotBlank() &&
                    (kind != "BOOKING_FORM" || body.containsFormLinkMarker()),
            ) { Text(language.text("Zapisz", "Save", "Зберегти")) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(language.text("Anuluj", "Cancel", "Скасувати")) } },
    )
}

private fun renderSmsPreview(
    body: String,
    name: String?,
    savedDetails: String?,
    visitDate: String,
    visitTime: String,
    language: UiLanguage,
): String = body
    .replace("{{name}}", name.orEmpty())
    .replace("{{savedDetails}}", savedDetails ?: "brak dodatkowych danych")
    .replace("{{visitDate}}", visitDate)
    .replace("{{visitTime}}", visitTime)
    .replace("{{formUrl}}", formLinkMarker(language))

private val formLinkMarkers = listOf(
    "[link do formularza zostanie dodany automatycznie]",
    "[the form link will be added automatically]",
    "[посилання на форму буде додано автоматично]",
)

private fun formLinkMarker(language: UiLanguage): String = language.text(
    formLinkMarkers.elementAt(0),
    formLinkMarkers.elementAt(1),
    formLinkMarkers.elementAt(2),
)

private fun String.containsFormLinkMarker(): Boolean = contains("{{formUrl}}") || formLinkMarkers.any(::contains)

private fun String.toServerSmsTemplate(): String = formLinkMarkers.fold(trim()) { result, marker ->
    result.replace(marker, "{{formUrl}}")
}

private fun String.toStoredSmsTemplate(
    name: String?,
    savedDetails: String?,
    visitDate: String,
    visitTime: String,
): String {
    var template = toServerSmsTemplate()
    name?.takeIf(String::isNotBlank)?.let { template = template.replace(it, "{{name}}") }
    savedDetails?.takeIf(String::isNotBlank)?.let { template = template.replace(it, "{{savedDetails}}") }
    template = template.replace(visitDate, "{{visitDate}}")
    return template.replace(visitTime, "{{visitTime}}")
}

private enum class SmsComposerMode { Template, Custom }

@Composable
private fun OutcomeNotificationPermissionCard(
    onEnable: () -> Unit,
    language: UiLanguage,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.extraLarge,
        tonalElevation = 1.dp,
    ) {
        Row(
            modifier = Modifier.padding(18.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Icon(
                Icons.Rounded.NotificationsActive,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(34.dp),
            )
            Column(Modifier.weight(1f)) {
                Text(language.text("Przypomnienia są wyłączone", "Outcome reminders are off", "Нагадування вимкнені"), style = MaterialTheme.typography.titleMedium)
                Text(
                    language.text(
                        "Zezwól na prywatne przypomnienie po zakończeniu połączenia. Dane rozmówcy nie będą widoczne.",
                        "Allow a private reminder after a completed call. Caller and CRM details are not shown.",
                        "Дозвольте приватне нагадування після завершеного дзвінка. Дані абонента не відображаються.",
                    ),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 2.dp),
                )
                Button(onClick = onEnable, modifier = Modifier.padding(top = 10.dp)) {
                    Text(language.text("Włącz przypomnienia", "Enable reminders", "Увімкнути нагадування"))
                }
            }
        }
    }
}

@Composable
internal fun CallOutcomeCard(
    pending: PendingCallOutcome,
    pendingCount: Int,
    onSelect: (CallOutcomeCode) -> Unit,
    onSelectTopic: (CallOutcomeCode, String?) -> Unit,
    onSendSms: () -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
    customTopics: List<String> = emptyList(),
    onSaveTopic: (String) -> Unit = {},
    onDeleteTopic: (String) -> Unit = {},
    onDismissAll: () -> Unit = {},
    smsStatus: String? = null,
	language: UiLanguage = UiLanguage.Polish,
) {
	var topicDialogOpen by rememberSaveable(pending.callRef) { mutableStateOf(false) }
	var customTopic by rememberSaveable(pending.callRef) { mutableStateOf("") }
	var saveCustomTopic by rememberSaveable(pending.callRef) { mutableStateOf(true) }
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.extraLarge,
        color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.58f),
    ) {
        Column(Modifier.padding(18.dp)) {
            Text(language.text("Wybierz wynik połączenia", "Choose call outcome", "Оберіть результат дзвінка"), style = MaterialTheme.typography.titleLarge)
            Text(
                pending.displayName ?: pending.phoneNumber ?: language.text("Nieznany rozmówca", "Unknown caller", "Невідомий абонент"),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(top = 8.dp),
            )
            if (pending.displayName != null && pending.phoneNumber != null) {
                Text(
                    pending.phoneNumber,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 2.dp),
                )
            }
            Text(
                pending.disconnectCategory.asCallSummary(language),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 4.dp),
            )
            if (pendingCount > 1) {
                Text(
                    language.text(
                        "$pendingCount połączeń czeka na wynik.",
                        "$pendingCount calls are waiting for an outcome.",
                        "$pendingCount дзвінків очікують на результат.",
                    ),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(top = 6.dp),
                )
            }
            Column(
                modifier = Modifier.padding(top = 14.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Button(
                    onClick = onSendSms,
                    enabled = pending.phoneNumber != null && smsStatus != "Sending SMS…",
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text(language.text("Wyślij SMS", "Send SMS", "Надіслати SMS"))
                }
                listOf(CallOutcomeCode.FollowUpRequired, CallOutcomeCode.NotInterested, CallOutcomeCode.NoAvailability).forEach { outcome ->
                    OutlinedButton(
                        onClick = { onSelect(outcome) },
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Text(outcome.localizedLabel(language))
                    }
                }
				OutlinedButton(onClick = { topicDialogOpen = true }, modifier = Modifier.fillMaxWidth()) {
					Text(language.text("Wybierz temat rozmowy", "Choose conversation topic", "Оберіть тему розмови"))
				}
            }
            if (smsStatus != null) {
                Text(
                    smsStatus.localizedSmsStatus(language),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 8.dp),
                )
            }
            Row(
                modifier = Modifier.align(Alignment.End),
                horizontalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                if (pendingCount > 1) {
                    TextButton(onClick = onDismissAll) {
                        Text(language.text("Pomiń wszystkie", "Skip all", "Пропустити всі"))
                    }
                }
                TextButton(onClick = onDismiss) {
                    Text(language.text("Pomiń", "Skip", "Пропустити"))
                }
            }
			if (topicDialogOpen) {
				AlertDialog(
					onDismissRequest = { topicDialogOpen = false },
					title = { Text(language.text("Temat rozmowy", "Conversation topic", "Тема розмови")) },
					text = { Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
						listOf(
							CallOutcomeCode.TopicAgriculture to language.text("Rolnictwo", "Agriculture", "Сільське господарство"),
							CallOutcomeCode.TopicConstruction to language.text("Sprzęt budowlany", "Construction equipment", "Будівельна техніка"),
							CallOutcomeCode.TopicTrucks to language.text("Ciężarówki", "Trucks", "Вантажівки"),
							CallOutcomeCode.TopicOther to language.text("Inny temat", "Other topic", "Інша тема"),
						).filter { (topic, _) -> topic != CallOutcomeCode.TopicOther }.forEach { (topic, label) -> TextButton(onClick = { topicDialogOpen = false; onSelectTopic(topic, label) }, modifier = Modifier.fillMaxWidth()) { Text(label) } }
						customTopics.forEach { label ->
							Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
								TextButton(onClick = { topicDialogOpen = false; onSelectTopic(CallOutcomeCode.TopicOther, label) }, modifier = Modifier.weight(1f)) { Text(label) }
								IconButton(onClick = { onDeleteTopic(label) }) {
									Icon(Icons.Rounded.DeleteOutline, contentDescription = language.text("Usuń temat", "Delete topic", "Видалити тему"))
								}
							}
						}
						OutlinedTextField(
							value = customTopic,
							onValueChange = { customTopic = it.take(120) },
							label = { Text(language.text("Inny temat — wpisz nazwę", "Other topic — enter name", "Інша тема — введіть назву")) },
							modifier = Modifier.fillMaxWidth(),
						)
						Row(verticalAlignment = Alignment.CenterVertically) {
							Checkbox(checked = saveCustomTopic, onCheckedChange = { saveCustomTopic = it })
							Text(language.text("Zapisz na liście tematów", "Save in topic list", "Зберегти у списку тем"))
						}
					} },
					confirmButton = {
						TextButton(onClick = { val topic = customTopic.trim(); if (saveCustomTopic) onSaveTopic(topic); topicDialogOpen = false; onSelectTopic(CallOutcomeCode.TopicOther, topic) }, enabled = customTopic.isNotBlank()) {
							Text(language.text("Zapisz temat", "Save topic", "Зберегти тему"))
						}
					},
					dismissButton = { TextButton(onClick = { topicDialogOpen = false }) { Text(language.text("Anuluj", "Cancel", "Скасувати")) } },
				)
			}
        }
    }
}

private fun String.asCallSummary(language: UiLanguage): String = when (this) {
    "missed" -> language.text("Połączenie nieodebrane. Wybierz wynik lub pomiń.", "The call was missed. You can choose a follow-up outcome or skip.", "Дзвінок пропущено. Оберіть результат або пропустіть.")
    "rejected" -> language.text("Połączenie odrzucone. Wybierz wynik lub pomiń.", "The call was rejected. You can choose a follow-up outcome or skip.", "Дзвінок відхилено. Оберіть результат або пропустіть.")
    else -> language.text("Połączenie zakończone. Jaki był wynik?", "The call has ended. What was the result?", "Дзвінок завершено. Який результат?")
}

@Composable
internal fun CallerCard(
    state: CallerCardState,
    onClear: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val language = LocalUiLanguage.current
    val presentation = state.presentation(language)
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.extraLarge,
        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.48f),
    ) {
        Row(
            modifier = Modifier.padding(18.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            if (state == CallerCardState.Loading) {
                CircularProgressIndicator(
                    modifier = Modifier.size(38.dp),
                    strokeWidth = 3.dp,
                )
            } else {
                Icon(
                    imageVector = presentation.icon,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(38.dp),
                )
            }
            Column(Modifier.weight(1f)) {
                Text(presentation.title, style = MaterialTheme.typography.titleMedium)
                Text(
                    presentation.description,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 2.dp),
                )
            }
            if (state != CallerCardState.Idle && state != CallerCardState.Loading) {
                TextButton(onClick = onClear) { Text(language.text("Wyczyść", "Clear", "Очистити")) }
            }
        }
    }
}

private data class CallerCardPresentation(
    val title: String,
    val description: String,
    val icon: androidx.compose.ui.graphics.vector.ImageVector,
)

private fun CallerCardState.presentation(language: UiLanguage): CallerCardPresentation = when (this) {
    CallerCardState.Idle -> CallerCardPresentation(
        title = language.text("Gotowe na kolejne połączenie", "Ready for the next caller", "Готово до наступного дзвінка"),
        description = language.text("Tutaj pojawi się wynik wyszukiwania w CRM.", "OPONX CRM lookup will appear here.", "Тут з’явиться результат пошуку в CRM."),
        icon = Icons.Rounded.PhoneInTalk,
    )
    CallerCardState.Loading -> CallerCardPresentation(
        title = language.text("Wyszukiwanie rozmówcy", "Looking up caller", "Пошук абонента"),
        description = language.text("Połączenie jest aktywne. Wyszukiwanie wznowi się po powrocie sieci.", "The call is already allowed. Lookup resumes when mobile data returns.", "Дзвінок активний. Пошук відновиться після повернення мережі."),
        icon = Icons.Rounded.PersonSearch,
    )
    is CallerCardState.Matched -> CallerCardPresentation(
        title = identity.displayName ?: "OPONX CRM client",
		description = if (identity.isReturningCustomer) {
			language.text("Stały klient — użyjemy szablonu z zapisanymi danymi.", "Returning client — the saved-data SMS template will be used.", "Постійний клієнт — буде використано шаблон зі збереженими даними.")
		} else {
			language.text("Nowy klient — użyjemy szablonu pierwszej wizyty.", "New client — the first-visit SMS template will be used.", "Новий клієнт — буде використано шаблон першого візиту.")
		},
        icon = Icons.Rounded.CheckCircle,
    )
    CallerCardState.NotFound -> CallerCardPresentation(
        title = language.text("Nie znaleziono rozmówcy", "Caller not found", "Абонента не знайдено"),
        description = language.text("Żaden klient CRM nie ma tego numeru.", "No OPONX CRM client matches this number.", "У CRM немає клієнта з цим номером."),
        icon = Icons.Rounded.PersonSearch,
    )
    CallerCardState.NumberUnavailable -> CallerCardPresentation(
        title = language.text("Numer rozmówcy niedostępny", "Caller number unavailable", "Номер абонента недоступний"),
        description = language.text("Android nie udostępnił numeru do wyszukania w CRM.", "Android did not provide a number for CRM lookup.", "Android не надав номер для пошуку в CRM."),
        icon = Icons.Rounded.PhoneInTalk,
    )
    CallerCardState.Unauthorized -> CallerCardPresentation(
        title = language.text("Brak dostępu do CRM", "CRM access unavailable", "Немає доступу до CRM"),
        description = language.text("Lokalne dane dostępowe zostały odrzucone.", "The local M3 credential was rejected.", "Локальні дані доступу відхилено."),
        icon = Icons.Rounded.Warning,
    )
    is CallerCardState.Unavailable -> CallerCardPresentation(
        title = language.text("Wyszukiwanie w CRM niedostępne", "CRM lookup unavailable", "Пошук у CRM недоступний"),
        description = reason.description(language),
        icon = Icons.Rounded.CloudOff,
    )
}

private fun CallerLookupFailure.description(language: UiLanguage): String = when (this) {
    CallerLookupFailure.NotConfigured -> language.text("CRM nie jest skonfigurowany w tej wersji.", "CRM is not configured for this build.", "CRM не налаштовано в цій версії.")
    CallerLookupFailure.Network -> language.text("Brak połączenia z CRM. Sprawdź internet w telefonie.", "Cannot reach CRM. Check the phone's internet connection.", "Немає зв’язку з CRM. Перевірте інтернет на телефоні.")
    CallerLookupFailure.Server -> language.text("CRM zwrócił błąd serwera. Spróbuj ponownie później.", "CRM returned a server error. Try again later.", "CRM повернула помилку сервера. Спробуйте пізніше.")
    CallerLookupFailure.Throttled -> language.text("CRM tymczasowo ograniczył liczbę zapytań.", "CRM temporarily limited lookup requests.", "CRM тимчасово обмежила кількість запитів.")
    CallerLookupFailure.InvalidResponse -> language.text("CRM zwrócił nieobsługiwaną odpowiedź.", "CRM returned an unsupported response.", "CRM повернула непідтримувану відповідь.")
}

private fun CallOutcomeCode.localizedLabel(language: UiLanguage): String = when (this) {
    CallOutcomeCode.Interested -> language.text("Zainteresowany", "Interested", "Зацікавлений")
    CallOutcomeCode.FollowUpRequired -> language.text("Wymaga kontaktu", "Follow-up required", "Потрібен повторний контакт")
    CallOutcomeCode.NotInterested -> language.text("Niezainteresowany", "Not interested", "Не зацікавлений")
    CallOutcomeCode.WrongNumber -> language.text("Błędny numer", "Wrong number", "Неправильний номер")
	CallOutcomeCode.NoAvailability -> language.text("Brak wolnego terminu", "No available appointment", "Немає вільного часу")
	CallOutcomeCode.TopicAgriculture -> language.text("Rolnictwo", "Agriculture", "Сільське господарство")
	CallOutcomeCode.TopicConstruction -> language.text("Sprzęt budowlany", "Construction equipment", "Будівельна техніка")
	CallOutcomeCode.TopicTrucks -> language.text("Ciężarówki", "Trucks", "Вантажівки")
	CallOutcomeCode.TopicOther -> language.text("Inny temat", "Other topic", "Інша тема")
    CallOutcomeCode.Other -> language.text("Inne", "Other", "Інше")
}

private fun String.localizedSmsStatus(language: UiLanguage): String = when (this) {
    "Sending SMS…" -> language.text("Wysyłanie SMS…", "Sending SMS…", "Надсилання SMS…")
    "SMS queued for sending." -> language.text("SMS czeka na wysłanie.", "SMS queued for sending.", "SMS очікує на надсилання.")
    "No valid phone number for this call." -> language.text("Brak prawidłowego numeru telefonu.", "No valid phone number for this call.", "Немає правильного номера телефону.")
    "CRM is not configured for this build." -> language.text("CRM nie jest skonfigurowany w tej wersji.", "CRM is not configured for this build.", "CRM не налаштовано в цій версії.")
    "CRM authorization failed." -> language.text("Autoryzacja CRM nie powiodła się.", "CRM authorization failed.", "Помилка авторизації CRM.")
    else -> this
}
