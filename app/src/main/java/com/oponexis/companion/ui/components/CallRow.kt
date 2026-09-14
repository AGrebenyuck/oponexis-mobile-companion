package com.oponexis.companion.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.CallMade
import androidx.compose.material.icons.automirrored.rounded.CallReceived
import androidx.compose.material.icons.automirrored.rounded.Help
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Schedule
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.oponexis.companion.domain.model.CallDirection
import com.oponexis.companion.domain.model.CallPreview
import com.oponexis.companion.domain.model.CallState
import com.oponexis.companion.ui.theme.BrandBlue
import com.oponexis.companion.ui.theme.Success
import com.oponexis.companion.ui.theme.Warning
import com.oponexis.companion.ui.localization.LocalUiLanguage
import com.oponexis.companion.ui.localization.text
import com.oponexis.companion.domain.model.UiLanguage
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

@Composable
fun CallRow(
    call: CallPreview,
    modifier: Modifier = Modifier,
    repeatCount: Int = 1,
    onClick: (() -> Unit)? = null,
) {
    val language = LocalUiLanguage.current
    val displayName = if (call.displayName == "Unknown caller") {
        language.text("Nieznany rozmówca", "Unknown caller", "Невідомий абонент")
    } else {
        call.displayName
    }
    Surface(
        modifier = modifier.fillMaxWidth().then(if (onClick == null) Modifier else Modifier.clickable(onClick = onClick)),
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 1.dp,
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .background(MaterialTheme.colorScheme.primaryContainer, CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = if (call.direction == CallDirection.Incoming) Icons.AutoMirrored.Rounded.CallReceived else Icons.AutoMirrored.Rounded.CallMade,
                    contentDescription = null,
                    tint = BrandBlue,
                )
            }
            Column(Modifier.weight(1f)) {
                Text(
                    text = if (repeatCount > 1) "$displayName ($repeatCount)" else displayName,
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                if (call.phoneNumber != null && call.phoneNumber != call.displayName) {
                    Text(
                        text = call.phoneNumber,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Text(
                    text = call.company?.localizedCallContext(language) ?: language.text("Brak klienta w CRM", "No CRM match", "Немає клієнта в CRM"),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                call.smsDeliveryStatus?.let { status ->
                    Text(
                        text = smsDeliveryLabel(status, call.smsDeliveryDetail, language),
                        style = MaterialTheme.typography.labelMedium,
                        color = if (status == "FAILED") MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = call.observedAtEpochMillis.localizedDateTime(language),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(Modifier.size(5.dp))
                StateBadge(call.state)
            }
        }
    }
}

private fun Long.localizedDateTime(language: UiLanguage): String {
    val locale = when (language) {
        UiLanguage.Polish -> Locale.forLanguageTag("pl-PL")
        UiLanguage.English -> Locale.forLanguageTag("en-GB")
        UiLanguage.Ukrainian -> Locale.forLanguageTag("uk-UA")
    }
    return DateTimeFormatter.ofPattern("dd MMM, HH:mm", locale).format(
        Instant.ofEpochMilli(this).atZone(ZoneId.systemDefault()),
    )
}

private fun String.localizedCallContext(language: UiLanguage): String {
    val translations = listOf(
        "Follow up required" to language.text("Wymaga kontaktu", "Follow-up required", "Потрібен повторний контакт"),
        "Not interested" to language.text("Niezainteresowany", "Not interested", "Не зацікавлений"),
        "Wrong number" to language.text("Błędny numer", "Wrong number", "Неправильний номер"),
        "Interested" to language.text("Zainteresowany", "Interested", "Зацікавлений"),
		"Returning customer" to language.text("Stały klient", "Returning customer", "Постійний клієнт"),
        "Outcome skipped" to language.text("Wynik pominięty", "Outcome skipped", "Результат пропущено"),
        "Awaiting outcome" to language.text("Oczekuje na wynik", "Awaiting outcome", "Очікує на результат"),
        "Sync failed" to language.text("Błąd synchronizacji", "Sync failed", "Помилка синхронізації"),
        "Waiting to sync" to language.text("Oczekuje na synchronizację", "Waiting to sync", "Очікує на синхронізацію"),
        "Synced" to language.text("Zsynchronizowano", "Synced", "Синхронізовано"),
        "Other" to language.text("Inne", "Other", "Інше"),
    )
    return translations.fold(this) { value, (source, target) -> value.replace(source, target) }
}

private fun smsDeliveryLabel(status: String, detail: String?, language: UiLanguage): String = when (status) {
    "QUEUED" -> language.text("SMS: w kolejce bramki", "SMS: queued in Gateway", "SMS: у черзі шлюзу")
    "SENT" -> language.text("SMS: wysłany przez urządzenie", "SMS: sent by device", "SMS: надіслано пристроєм")
    "DELIVERED" -> language.text("SMS: dostarczony", "SMS: delivered", "SMS: доставлено")
    "FAILED" -> language.text("SMS nie został wysłany", "SMS not sent", "SMS не надіслано") + detail?.let { ": $it" }.orEmpty()
    "CANCELLED" -> language.text("SMS: anulowany", "SMS: cancelled", "SMS: скасовано")
    else -> "SMS: ${status.lowercase()}"
}

@Composable
private fun StateBadge(state: CallState) {
    val (icon, color) = when (state) {
        CallState.Identified -> Icons.Rounded.CheckCircle to Success
        CallState.Unknown -> Icons.AutoMirrored.Rounded.Help to MaterialTheme.colorScheme.onSurfaceVariant
        CallState.Pending -> Icons.Rounded.Schedule to Warning
    }
    Icon(
        imageVector = icon,
        contentDescription = state.name,
        tint = color,
        modifier = Modifier.size(18.dp),
    )
}
