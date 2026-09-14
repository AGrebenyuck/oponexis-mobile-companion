package com.oponexis.companion.ui.screen.messages

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.MarkEmailRead
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.oponexis.companion.data.local.DirectSmsDao
import com.oponexis.companion.data.local.DirectSmsMessageEntity
import com.oponexis.companion.platform.sms.DirectSmsScheduler
import com.oponexis.companion.ui.localization.LocalUiLanguage
import com.oponexis.companion.ui.localization.text
import dagger.hilt.android.lifecycle.HiltViewModel
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import javax.inject.Inject

@HiltViewModel
class MessagesViewModel @Inject constructor(dao: DirectSmsDao, private val scheduler: DirectSmsScheduler) : ViewModel() {
    val messages = dao.observeAll()
    fun refresh() = scheduler.sync()
}

@Composable
fun MessagesRoute(contentPadding: PaddingValues, viewModel: MessagesViewModel = hiltViewModel()) {
    val messages by viewModel.messages.collectAsStateWithLifecycle(emptyList())
    MessagesScreen(contentPadding, messages, viewModel::refresh)
}

@Composable
private fun MessagesScreen(contentPadding: PaddingValues, messages: List<DirectSmsMessageEntity>, onRefresh: () -> Unit) {
    val language = LocalUiLanguage.current
    val context = LocalContext.current
    val permissions = arrayOf(Manifest.permission.SEND_SMS, Manifest.permission.RECEIVE_SMS, Manifest.permission.READ_SMS)
    fun granted() = permissions.all { context.checkSelfPermission(it) == PackageManager.PERMISSION_GRANTED }
    var hasPermissions by remember { mutableStateOf(granted()) }
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) {
        hasPermissions = granted()
        if (hasPermissions) onRefresh()
    }
    var direction by remember { mutableStateOf("OUT") }
    val visible = messages.filter { it.direction == direction }

    Column(Modifier.fillMaxSize().padding(contentPadding).padding(horizontal = 16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(language.text("Wiadomości", "Messages", "Повідомлення"), style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold, modifier = Modifier.padding(top = 14.dp))
        if (!hasPermissions) {
            Surface(shape = MaterialTheme.shapes.large, color = MaterialTheme.colorScheme.errorContainer) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(language.text("Wymagany dostęp do SMS", "SMS access is required", "Потрібен доступ до SMS"), fontWeight = FontWeight.SemiBold)
                    Text(language.text("Companion potrzebuje dostępu, aby wysyłać wiadomości, odbierać odpowiedzi i pokazywać prawdziwy status.", "Companion needs access to send messages, receive replies and show their real status.", "Companion потребує доступу, щоб надсилати повідомлення, отримувати відповіді та показувати реальний статус."))
                    Button(onClick = { launcher.launch(permissions) }) { Text(language.text("Zezwól na SMS", "Allow SMS", "Дозволити SMS")) }
                }
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            FilterChip(direction == "OUT", { direction = "OUT" }, { Text(language.text("Wychodzące", "Outgoing", "Вихідні")) })
            FilterChip(direction == "IN", { direction = "IN" }, { Text(language.text("Przychodzące", "Incoming", "Вхідні")) })
        }
        if (visible.isEmpty()) {
            Column(Modifier.fillMaxWidth().padding(vertical = 48.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Icon(Icons.Rounded.MarkEmailRead, null, tint = MaterialTheme.colorScheme.primary)
                Text(language.text("Brak wiadomości", "No messages", "Немає повідомлень"), style = MaterialTheme.typography.titleMedium)
                Text(language.text("Nowa aktywność pojawi się tutaj automatycznie.", "New activity will appear here automatically.", "Нова активність з’явиться тут автоматично."), color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        } else {
            LazyColumn(contentPadding = PaddingValues(bottom = 20.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                items(visible, key = { it.id }) { MessageCard(it) }
            }
        }
    }
}

@Composable
private fun MessageCard(message: DirectSmsMessageEntity) {
    val language = LocalUiLanguage.current
    val label = when (message.status) {
        "CLAIMED", "SENDING" -> language.text("Wysyłanie", "Sending", "Надсилання")
        "SENT" -> language.text("Wysłano", "Sent", "Надіслано")
        "DELIVERED" -> language.text("Dostarczono", "Delivered", "Доставлено")
        "FAILED" -> language.text("Błąd", "Failed", "Помилка")
        "RECEIVED" -> language.text("Odebrano", "Received", "Отримано")
        else -> message.status
    }
    Surface(Modifier.fillMaxWidth(), shape = MaterialTheme.shapes.large, color = if (message.status == "FAILED") MaterialTheme.colorScheme.errorContainer else MaterialTheme.colorScheme.surfaceContainer) {
        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(5.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(message.phoneNumber, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                Text(label, style = MaterialTheme.typography.labelMedium, color = if (message.status == "FAILED") MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary)
            }
            Text(message.body, maxLines = 3, overflow = TextOverflow.Ellipsis)
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(message.source, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text(formatTime(message.updatedAtEpochMillis), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            message.error?.let { Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error) }
        }
    }
}

private fun formatTime(epochMillis: Long): String = DateTimeFormatter.ofPattern("dd.MM, HH:mm").withZone(ZoneId.systemDefault()).format(Instant.ofEpochMilli(epochMillis))
