package com.oponexis.companion.ui.screen.diagnostics

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.PhoneInTalk
import androidx.compose.material.icons.rounded.DeleteSweep
import androidx.compose.material3.Button
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.oponexis.companion.domain.model.DiagnosticSnapshot
import com.oponexis.companion.domain.model.DiagnosticEvent
import com.oponexis.companion.domain.model.DiagnosticCategory
import com.oponexis.companion.domain.model.DiagnosticOutcome
import com.oponexis.companion.domain.repository.CompanionRepository
import com.oponexis.companion.domain.repository.DiagnosticJournal
import com.oponexis.companion.platform.callscreening.CallScreeningDiagnostics
import com.oponexis.companion.platform.callscreening.CallScreeningRoleController
import com.oponexis.companion.platform.callscreening.CallScreeningRoleStatus
import com.oponexis.companion.platform.callscreening.CallScreeningSnapshot
import com.oponexis.companion.platform.callscreening.INTERNAL_RESPONSE_BUDGET_MILLIS
import com.oponexis.companion.platform.calllifecycle.PostCallDiagnostics
import com.oponexis.companion.platform.calllifecycle.PostCallDisconnectCategory
import com.oponexis.companion.platform.calllifecycle.PostCallDurationBucket
import com.oponexis.companion.platform.calllifecycle.PostCallSnapshot
import com.oponexis.companion.ui.theme.Success
import com.oponexis.companion.ui.localization.LocalUiLanguage
import com.oponexis.companion.ui.localization.text
import com.oponexis.companion.domain.model.UiLanguage
import com.oponexis.companion.BuildConfig
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import androidx.lifecycle.viewModelScope
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import javax.inject.Inject

@HiltViewModel
class DiagnosticsViewModel @Inject constructor(
    repository: CompanionRepository,
    private val roleController: CallScreeningRoleController,
    private val journal: DiagnosticJournal,
) : ViewModel() {
    val state: StateFlow<DiagnosticSnapshot> = repository.diagnostics
    val screening: StateFlow<CallScreeningSnapshot> = CallScreeningDiagnostics.snapshot
    val postCall: StateFlow<PostCallSnapshot> = PostCallDiagnostics.snapshot
    val journalEvents: StateFlow<List<DiagnosticEvent>> = journal.events
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    private val mutableRoleStatus = kotlinx.coroutines.flow.MutableStateFlow(roleController.status())
    val roleStatus: StateFlow<CallScreeningRoleStatus> = mutableRoleStatus

    fun refreshRoleStatus() {
        mutableRoleStatus.value = roleController.status()
    }

    fun roleRequestIntent(): Intent? = roleController.createRequestIntent()

    fun clearJournal() {
        viewModelScope.launch { journal.clear() }
    }
}

@Composable
fun DiagnosticsRoute(
    contentPadding: PaddingValues,
    viewModel: DiagnosticsViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val roleStatus by viewModel.roleStatus.collectAsStateWithLifecycle()
    val screening by viewModel.screening.collectAsStateWithLifecycle()
    val postCall by viewModel.postCall.collectAsStateWithLifecycle()
    val journalEvents by viewModel.journalEvents.collectAsStateWithLifecycle()
    val language = LocalUiLanguage.current
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    var contactAccessGranted by remember {
        mutableStateOf(
            context.checkSelfPermission(Manifest.permission.READ_CONTACTS) ==
                PackageManager.PERMISSION_GRANTED,
        )
    }
    val roleLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult(),
        onResult = { viewModel.refreshRoleStatus() },
    )
    val contactPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
        onResult = { contactAccessGranted = it },
    )
    DisposableEffect(lifecycleOwner, context) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                viewModel.refreshRoleStatus()
                contactAccessGranted =
                    context.checkSelfPermission(Manifest.permission.READ_CONTACTS) ==
                    PackageManager.PERMISSION_GRANTED
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(
                start = 20.dp,
                top = contentPadding.calculateTopPadding() + 24.dp,
                end = 20.dp,
                bottom = contentPadding.calculateBottomPadding() + 24.dp,
            ),
    ) {
        Text(language.text("Diagnostyka", "Diagnostics", "Діагностика"), style = MaterialTheme.typography.headlineMedium)
        Text(
            language.text("Bezpieczny przegląd stanu aplikacji.", "Privacy-safe local health overview.", "Безпечний огляд стану застосунку."),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 4.dp, bottom = 24.dp),
        )
        Surface(shape = MaterialTheme.shapes.extraLarge, tonalElevation = 1.dp) {
            Column(Modifier.padding(20.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Rounded.CheckCircle,
                        contentDescription = null,
                        tint = Success,
                        modifier = Modifier
                            .size(44.dp)
                            .background(Success.copy(alpha = 0.12f), CircleShape)
                            .padding(10.dp),
                    )
                    Column(Modifier.padding(start = 14.dp)) {
                        Text(language.text("Aplikacja działa prawidłowo", "Foundation healthy", "Застосунок працює справно"), style = MaterialTheme.typography.titleLarge)
                        Text(state.lastCheckLabel.localizedDiagnosticValue(language), color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
                Spacer(Modifier.size(20.dp))
                DiagnosticRow(language.text("Aplikacja", "Application", "Застосунок"), state.appState.localizedDiagnosticValue(language))
                HorizontalDivider()
                DiagnosticRow(language.text("Lokalna baza danych", "Local database", "Локальна база даних"), state.localDatabase.localizedDiagnosticValue(language))
                HorizontalDivider()
                DiagnosticRow(language.text("Synchronizacja w tle", "Background sync", "Фонова синхронізація"), state.backgroundSync.localizedDiagnosticValue(language))
                HorizontalDivider()
                DiagnosticRow(language.text("Zdarzenia w kolejce", "Queued events", "Події в черзі"), state.queuedEvents.toString())
                HorizontalDivider()
                Column(Modifier.padding(vertical = 15.dp)) {
                    Text(
                        language.text("Adres CRM", "CRM address", "Адреса CRM"),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Text(
                        BuildConfig.CRM_BASE_URL,
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(top = 6.dp),
                    )
                }
            }
        }
        CallScreeningCard(
            roleStatus = roleStatus,
            screening = screening,
            onRequestRole = {
                viewModel.roleRequestIntent()?.let(roleLauncher::launch)
            },
            contactAccessGranted = contactAccessGranted,
            onRequestContactAccess = {
                contactPermissionLauncher.launch(Manifest.permission.READ_CONTACTS)
            },
            modifier = Modifier.padding(top = 16.dp),
            language = language,
        )
        PostCallCard(
            snapshot = postCall,
            supported = Build.VERSION.SDK_INT >= Build.VERSION_CODES.R,
            modifier = Modifier.padding(top = 16.dp),
            language = language,
        )
        DiagnosticJournalCard(
            events = journalEvents,
            onClear = viewModel::clearJournal,
            modifier = Modifier.padding(top = 16.dp),
            language = language,
        )
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 20.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.Top,
        ) {
            Icon(Icons.Rounded.Info, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
            Text(
                language.text(
                    "Dziennik jest zapisany lokalnie i przechowuje maksymalnie 200 zdarzeń technicznych. Nie zapisuje haseł, tokenów, treści SMS ani pełnych numerów telefonu.",
                    "The journal is stored locally and retains up to 200 technical events. It never stores passwords, tokens, SMS content, or full phone numbers.",
                    "Журнал зберігається локально й містить до 200 технічних подій. Паролі, токени, тексти SMS і повні номери телефонів не зберігаються.",
                ),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun DiagnosticJournalCard(
    events: List<DiagnosticEvent>,
    onClear: () -> Unit,
    modifier: Modifier = Modifier,
    language: UiLanguage,
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.extraLarge,
        tonalElevation = 1.dp,
    ) {
        Column(Modifier.padding(20.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Column(Modifier.weight(1f)) {
                    Text(language.text("Dziennik zdarzeń", "Event journal", "Журнал подій"), style = MaterialTheme.typography.titleLarge)
                    Text(
                        language.text("${events.size} z 200 zdarzeń", "${events.size} of 200 events", "${events.size} із 200 подій"),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                TextButton(onClick = onClear, enabled = events.isNotEmpty()) {
                    Icon(Icons.Rounded.DeleteSweep, contentDescription = null)
                    Text(language.text("Wyczyść", "Clear", "Очистити"), modifier = Modifier.padding(start = 4.dp))
                }
            }
            if (events.isEmpty()) {
                Text(
                    language.text("Brak zapisanych zdarzeń.", "No recorded events.", "Немає записаних подій."),
                    modifier = Modifier.padding(top = 14.dp),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            } else {
                events.take(50).forEachIndexed { index, event ->
                    if (index > 0) HorizontalDivider()
                    Column(Modifier.padding(vertical = 10.dp)) {
                        Text(event.category.localizedLabel(language), style = MaterialTheme.typography.titleSmall)
                        Text(
                            "${event.occurredAtEpochMillis.diagnosticTime()} · ${event.outcome.localizedLabel(language)}",
                            style = MaterialTheme.typography.bodySmall,
                            color = if (event.outcome == DiagnosticOutcome.Failed) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        event.reasonCode?.let { reason ->
                            Text("Kod: $reason", style = MaterialTheme.typography.bodySmall)
                        }
                        event.correlationId?.let { correlationId ->
                            Text("ID: $correlationId", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
                if (events.size > 50) {
                    Text(
                        language.text("Pokazano 50 najnowszych zdarzeń.", "Showing the latest 50 events.", "Показано 50 останніх подій."),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
    }
}

private fun DiagnosticCategory.localizedLabel(language: UiLanguage): String = when (this) {
    DiagnosticCategory.SmsGateway -> language.text("Sprawdzenie SMS Gateway", "SMS Gateway check", "Перевірка SMS Gateway")
    DiagnosticCategory.SmsSend -> language.text("Wysłanie SMS", "SMS send", "Надсилання SMS")
    DiagnosticCategory.CallerLookup -> language.text("Wyszukiwanie klienta", "Caller lookup", "Пошук клієнта")
    DiagnosticCategory.CallEventSync -> language.text("Synchronizacja połączenia", "Call event sync", "Синхронізація дзвінка")
    DiagnosticCategory.PushSync -> language.text("Synchronizacja push", "Push sync", "Синхронізація push")
}

private fun DiagnosticOutcome.localizedLabel(language: UiLanguage): String = when (this) {
    DiagnosticOutcome.Succeeded -> language.text("powodzenie", "succeeded", "успішно")
    DiagnosticOutcome.Failed -> language.text("błąd", "failed", "помилка")
}

private fun Long.diagnosticTime(): String = DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm:ss")
    .format(Instant.ofEpochMilli(this).atZone(ZoneId.systemDefault()))

@Composable
private fun PostCallCard(
    snapshot: PostCallSnapshot,
    supported: Boolean,
    modifier: Modifier = Modifier,
    language: UiLanguage,
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.extraLarge,
        tonalElevation = 1.dp,
    ) {
        Column(Modifier.padding(20.dp)) {
            Text(language.text("Stan po zakończeniu połączenia", "Post-call lifecycle proof of concept", "Стан після завершення дзвінка"), style = MaterialTheme.typography.titleLarge)
            Text(
                if (supported) language.text("Sygnał Androida jest dostępny", "Android post-call signal available", "Сигнал Android доступний") else language.text("Niedostępne poniżej Androida 11", "Unavailable below Android 11", "Недоступно нижче Android 11"),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 4.dp, bottom = 12.dp),
            )
            DiagnosticRow(language.text("Zarejestrowane sygnały", "Observed post-call signals", "Зареєстровані сигнали"), snapshot.observationCount.toString())
            HorizontalDivider()
            DiagnosticRow(
                language.text("Ostatni typ zakończenia", "Last disconnect category", "Останній тип завершення"),
                snapshot.lastDisconnectCategory.asLabel(language),
            )
            HorizontalDivider()
            DiagnosticRow(language.text("Ostatni zakres czasu", "Last duration bucket", "Останній діапазон часу"), snapshot.lastDurationBucket.asLabel(language))
            Text(
                language.text("Sygnał zawiera tylko kategorię zakończenia i przybliżony czas. Nie zapisuje dokładnego czasu rozmowy.", "This signal confirms a post-call opportunity and categorical Android metadata only. It does not prove an exact ANSWERED or ENDED timestamp and does not expose exact duration.", "Сигнал містить лише категорію завершення та приблизний час. Точна тривалість не зберігається."),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 12.dp),
            )
        }
    }
}

private fun PostCallDisconnectCategory?.asLabel(language: UiLanguage): String = when (this) {
    null -> language.text("Brak danych", "No evidence yet", "Немає даних")
    PostCallDisconnectCategory.Unknown -> language.text("Nieznane", "Unknown", "Невідомо")
    PostCallDisconnectCategory.Local -> language.text("Zakończone lokalnie", "Local", "Завершено локально")
    PostCallDisconnectCategory.Remote -> language.text("Zakończone zdalnie", "Remote", "Завершено віддалено")
    PostCallDisconnectCategory.Rejected -> language.text("Odrzucone", "Rejected", "Відхилено")
    PostCallDisconnectCategory.Missed -> language.text("Nieodebrane", "Missed", "Пропущено")
    PostCallDisconnectCategory.Other -> language.text("Inne", "Other", "Інше")
}

private fun PostCallDurationBucket?.asLabel(language: UiLanguage): String = when (this) {
    null -> language.text("Brak danych", "No evidence yet", "Немає даних")
    PostCallDurationBucket.Unknown -> language.text("Nieznany", "Unknown", "Невідомо")
    PostCallDurationBucket.VeryShort -> language.text("Poniżej 3 sekund", "Under 3 seconds", "Менше 3 секунд")
    PostCallDurationBucket.Short -> "3–59 s"
    PostCallDurationBucket.Medium -> "60–119 s"
    PostCallDurationBucket.Long -> language.text("120 sekund lub dłużej", "120 seconds or longer", "120 секунд або довше")
}

@Composable
private fun CallScreeningCard(
    roleStatus: CallScreeningRoleStatus,
    screening: CallScreeningSnapshot,
    onRequestRole: () -> Unit,
    contactAccessGranted: Boolean,
    onRequestContactAccess: () -> Unit,
    modifier: Modifier = Modifier,
    language: UiLanguage,
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.extraLarge,
        tonalElevation = 1.dp,
    ) {
        Column(Modifier.padding(20.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    Icons.Rounded.PhoneInTalk,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier
                        .size(44.dp)
                        .background(MaterialTheme.colorScheme.primaryContainer, CircleShape)
                        .padding(10.dp),
                )
                Column(Modifier.padding(start = 14.dp)) {
                    Text(language.text("Obsługa połączeń", "Call screening proof of concept", "Обробка дзвінків"), style = MaterialTheme.typography.titleLarge)
                    Text(
                        when {
                            !roleStatus.available -> language.text("Niedostępne na tym urządzeniu", "Unavailable on this device", "Недоступно на цьому пристрої")
                            roleStatus.held -> language.text("Aktywne · połączenia są zawsze dozwolone", "Active · calls are always allowed", "Активно · дзвінки завжди дозволені")
                            else -> language.text("Aplikacja nie jest wybrana do obsługi połączeń", "Not selected as the screening app", "Застосунок не вибрано для обробки дзвінків")
                        },
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }

            if (roleStatus.available && !roleStatus.held) {
                Button(
                    onClick = onRequestRole,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 20.dp),
                ) {
                    Text(language.text("Włącz obsługę połączeń", "Enable call screening", "Увімкнути обробку дзвінків"))
                }
            }

            Spacer(Modifier.size(16.dp))
            DiagnosticRow(
                language.text("Zapisane kontakty", "Saved callers", "Збережені контакти"),
                if (contactAccessGranted) language.text("Włączone", "Enabled", "Увімкнено") else language.text("Ograniczone", "Limited", "Обмежено"),
            )
            if (!contactAccessGranted) {
                Text(
                    language.text("Opcjonalny dostęp do kontaktów pozwala rozpoznawać zapisane numery. Oponexis nie zapisuje książki adresowej.", "Optional contact access lets Android send calls from saved numbers to this screening service. Oponexis does not read or store the address book in M2.", "Необов’язковий доступ до контактів дає змогу розпізнавати збережені номери. Oponexis не зберігає адресну книгу."),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 8.dp),
                )
                Button(
                    onClick = onRequestContactAccess,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 12.dp),
                ) {
                    Text(language.text("Zezwól na kontakty", "Allow saved callers", "Дозволити контакти"))
                }
            }

            Spacer(Modifier.size(16.dp))
            DiagnosticRow(language.text("Zarejestrowane wywołania", "Observed callbacks", "Зареєстровані виклики"), screening.callbackCount.toString())
            HorizontalDivider()
            DiagnosticRow(language.text("Przychodzące / wychodzące", "Incoming / outgoing", "Вхідні / вихідні"), "${screening.incomingCount} / ${screening.outgoingCount}")
            HorizontalDivider()
            DiagnosticRow(language.text("Ostatnia odpowiedź", "Last response", "Остання відповідь"), screening.lastResponseMillis.asTimingLabel(language))
            HorizontalDivider()
            DiagnosticRow(language.text("Najwolniejsza odpowiedź", "Slowest response", "Найповільніша відповідь"), screening.maximumResponseMillis.asTimingLabel(language))

            Text(
                language.text("Android wymaga odpowiedzi w ciągu 5 sekund. Aplikacja odpowiada lokalnie, bez oczekiwania na sieć lub dysk.", "Android requires an incoming-call response within 5 seconds. This PoC has a ${INTERNAL_RESPONSE_BUDGET_MILLIS} ms internal target and performs no network or disk work before responding. Contact access only changes whether Android delivers saved callers to the service.", "Android вимагає відповіді протягом 5 секунд. Застосунок відповідає локально, не очікуючи мережі чи диска."),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 12.dp),
            )
        }
    }
}

private fun Double?.asTimingLabel(language: UiLanguage): String = this?.let { "%.3f ms".format(it) }
    ?: language.text("Brak danych", "No evidence yet", "Немає даних")

private fun String.localizedDiagnosticValue(language: UiLanguage): String = when (this) {
    "Ready" -> language.text("Gotowa", "Ready", "Готово")
    "Action required" -> language.text("Wymaga działania", "Action required", "Потрібна дія")
    "Available" -> language.text("Dostępna", "Available", "Доступно")
    "Waiting to sync" -> language.text("Oczekuje na synchronizację", "Waiting to sync", "Очікує на синхронізацію")
    "Up to date" -> language.text("Aktualna", "Up to date", "Актуально")
    "Live local status" -> language.text("Bieżący stan lokalny", "Live local status", "Поточний локальний стан")
    else -> this
}

@Composable
private fun DiagnosticRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 15.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(label, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value, style = MaterialTheme.typography.labelLarge)
    }
}
