package com.oponexis.companion.ui.screen.diagnostics

import android.content.Intent
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
import androidx.compose.material3.Button
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.oponexis.companion.domain.model.DiagnosticSnapshot
import com.oponexis.companion.domain.repository.CompanionRepository
import com.oponexis.companion.platform.callscreening.CallScreeningDiagnostics
import com.oponexis.companion.platform.callscreening.CallScreeningRoleController
import com.oponexis.companion.platform.callscreening.CallScreeningRoleStatus
import com.oponexis.companion.platform.callscreening.CallScreeningSnapshot
import com.oponexis.companion.platform.callscreening.INTERNAL_RESPONSE_BUDGET_MILLIS
import com.oponexis.companion.ui.theme.Success
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.StateFlow
import javax.inject.Inject

@HiltViewModel
class DiagnosticsViewModel @Inject constructor(
    repository: CompanionRepository,
    private val roleController: CallScreeningRoleController,
) : ViewModel() {
    val state: StateFlow<DiagnosticSnapshot> = repository.diagnostics
    val screening: StateFlow<CallScreeningSnapshot> = CallScreeningDiagnostics.snapshot

    private val mutableRoleStatus = kotlinx.coroutines.flow.MutableStateFlow(roleController.status())
    val roleStatus: StateFlow<CallScreeningRoleStatus> = mutableRoleStatus

    fun refreshRoleStatus() {
        mutableRoleStatus.value = roleController.status()
    }

    fun roleRequestIntent(): Intent? = roleController.createRequestIntent()
}

@Composable
fun DiagnosticsRoute(
    contentPadding: PaddingValues,
    viewModel: DiagnosticsViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val roleStatus by viewModel.roleStatus.collectAsStateWithLifecycle()
    val screening by viewModel.screening.collectAsStateWithLifecycle()
    val roleLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult(),
        onResult = { viewModel.refreshRoleStatus() },
    )
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
        Text("Diagnostics", style = MaterialTheme.typography.headlineMedium)
        Text(
            "Privacy-safe local health overview.",
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
                        Text("Foundation healthy", style = MaterialTheme.typography.titleLarge)
                        Text(state.lastCheckLabel, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
                Spacer(Modifier.size(20.dp))
                DiagnosticRow("Application", state.appState)
                HorizontalDivider()
                DiagnosticRow("Local database", state.localDatabase)
                HorizontalDivider()
                DiagnosticRow("Background sync", state.backgroundSync)
                HorizontalDivider()
                DiagnosticRow("Queued events", state.queuedEvents.toString())
            }
        }
        CallScreeningCard(
            roleStatus = roleStatus,
            screening = screening,
            onRequestRole = {
                viewModel.roleRequestIntent()?.let(roleLauncher::launch)
            },
            modifier = Modifier.padding(top = 16.dp),
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
                "M2 stores only process-local counters and response timing. Phone numbers, contacts, and CRM data are never recorded. Diagnostic export remains unavailable.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun CallScreeningCard(
    roleStatus: CallScreeningRoleStatus,
    screening: CallScreeningSnapshot,
    onRequestRole: () -> Unit,
    modifier: Modifier = Modifier,
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
                    Text("Call screening proof of concept", style = MaterialTheme.typography.titleLarge)
                    Text(
                        when {
                            !roleStatus.available -> "Unavailable on this device"
                            roleStatus.held -> "Active · calls are always allowed"
                            else -> "Not selected as the screening app"
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
                    Text("Enable call screening")
                }
            }

            Spacer(Modifier.size(16.dp))
            DiagnosticRow("Observed callbacks", screening.callbackCount.toString())
            HorizontalDivider()
            DiagnosticRow("Incoming / outgoing", "${screening.incomingCount} / ${screening.outgoingCount}")
            HorizontalDivider()
            DiagnosticRow("Last response", screening.lastResponseMillis.asTimingLabel())
            HorizontalDivider()
            DiagnosticRow("Slowest response", screening.maximumResponseMillis.asTimingLabel())

            Text(
                "Android requires an incoming-call response within 5 seconds. This PoC has a ${INTERNAL_RESPONSE_BUDGET_MILLIS} ms internal target and performs no network or disk work before responding. Calls saved in system contacts may not be delivered because READ_CONTACTS is intentionally not requested.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 12.dp),
            )
        }
    }
}

private fun Double?.asTimingLabel(): String = this?.let { "%.3f ms".format(it) } ?: "No evidence yet"

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
