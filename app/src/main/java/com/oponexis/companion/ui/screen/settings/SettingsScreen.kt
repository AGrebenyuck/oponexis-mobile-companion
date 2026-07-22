package com.oponexis.companion.ui.screen.settings

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.DarkMode
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.LightMode
import androidx.compose.material.icons.rounded.SettingsBrightness
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import com.oponexis.companion.BuildConfig
import com.oponexis.companion.domain.model.ThemePreference
import com.oponexis.companion.domain.model.UserPreferences
import com.oponexis.companion.domain.repository.SettingsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val repository: SettingsRepository,
) : ViewModel() {
    val preferences: StateFlow<UserPreferences> = repository.preferences.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = UserPreferences(),
    )

    fun setTheme(theme: ThemePreference) {
        viewModelScope.launch { repository.setTheme(theme) }
    }

    fun setDiagnosticDetails(enabled: Boolean) {
        viewModelScope.launch { repository.setDiagnosticDetails(enabled) }
    }
}

@Composable
fun SettingsRoute(
    contentPadding: PaddingValues,
    viewModel: SettingsViewModel = hiltViewModel(),
) {
    val preferences by viewModel.preferences.collectAsStateWithLifecycle()
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
        Text("Settings", style = MaterialTheme.typography.headlineMedium)
        Text(
            "Make the companion feel at home.",
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 4.dp, bottom = 24.dp),
        )
        Text("Appearance", style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(bottom = 10.dp))
        Surface(shape = MaterialTheme.shapes.extraLarge, tonalElevation = 1.dp) {
            Column {
                ThemePreference.entries.forEachIndexed { index, theme ->
                    ThemeRow(
                        theme = theme,
                        selected = preferences.themePreference == theme,
                        onClick = { viewModel.setTheme(theme) },
                    )
                    if (index < ThemePreference.entries.lastIndex) HorizontalDivider(Modifier.padding(horizontal = 16.dp))
                }
            }
        }
        Text(
            "Diagnostics",
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.padding(top = 28.dp, bottom = 10.dp),
        )
        Surface(shape = MaterialTheme.shapes.extraLarge, tonalElevation = 1.dp) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { viewModel.setDiagnosticDetails(!preferences.diagnosticDetailsEnabled) }
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(Icons.Rounded.Info, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                Column(Modifier.weight(1f).padding(horizontal = 14.dp)) {
                    Text("Local health details", style = MaterialTheme.typography.titleMedium)
                    Text(
                        "Shows sanitized app status only",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Switch(
                    checked = preferences.diagnosticDetailsEnabled,
                    onCheckedChange = viewModel::setDiagnosticDetails,
                )
            }
        }
        Text(
            "Oponexis Mobile Companion ${BuildConfig.VERSION_NAME}\nInternal CallScreening PoC · M2",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 28.dp),
        )
    }
}

@Composable
private fun ThemeRow(theme: ThemePreference, selected: Boolean, onClick: () -> Unit) {
    val (label, icon) = when (theme) {
        ThemePreference.System -> "Use device setting" to Icons.Rounded.SettingsBrightness
        ThemePreference.Light -> "Light" to Icons.Rounded.LightMode
        ThemePreference.Dark -> "Dark" to Icons.Rounded.DarkMode
    }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
        Text(label, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
        RadioButton(selected = selected, onClick = onClick)
    }
}
