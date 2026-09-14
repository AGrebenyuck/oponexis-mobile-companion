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
import com.oponexis.companion.domain.model.HistoryRetention
import com.oponexis.companion.domain.model.UserPreferences
import com.oponexis.companion.domain.model.UiLanguage
import com.oponexis.companion.domain.repository.SettingsRepository
import com.oponexis.companion.ui.localization.LocalUiLanguage
import com.oponexis.companion.ui.localization.text
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

    fun setHistoryRetention(retention: HistoryRetention) {
        viewModelScope.launch { repository.setHistoryRetention(retention) }
    }

    fun setLanguage(language: UiLanguage) {
        viewModelScope.launch { repository.setLanguage(language) }
    }
}

@Composable
fun SettingsRoute(
    contentPadding: PaddingValues,
    viewModel: SettingsViewModel = hiltViewModel(),
) {
    val preferences by viewModel.preferences.collectAsStateWithLifecycle()
    val language = LocalUiLanguage.current
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
        Text(language.text("Ustawienia", "Settings", "Налаштування"), style = MaterialTheme.typography.headlineMedium)
        Text(
            language.text(
                "Dostosuj Companion do swojej pracy.",
                "Make the companion feel at home.",
                "Налаштуйте Companion для своєї роботи.",
            ),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 4.dp, bottom = 24.dp),
        )
        Text(language.text("Wygląd", "Appearance", "Вигляд"), style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(bottom = 10.dp))
        Surface(shape = MaterialTheme.shapes.extraLarge, tonalElevation = 1.dp) {
            Column {
                ThemePreference.entries.forEachIndexed { index, theme ->
                    ThemeRow(
                        theme = theme,
                        language = language,
                        selected = preferences.themePreference == theme,
                        onClick = { viewModel.setTheme(theme) },
                    )
                    if (index < ThemePreference.entries.lastIndex) HorizontalDivider(Modifier.padding(horizontal = 16.dp))
                }
            }
        }
        Text(
            language.text("Język", "Language", "Мова"),
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.padding(top = 28.dp, bottom = 10.dp),
        )
        Surface(shape = MaterialTheme.shapes.extraLarge, tonalElevation = 1.dp) {
            Column {
                UiLanguage.entries.forEachIndexed { index, item ->
                    LanguageRow(
                        language = item,
                        selected = preferences.uiLanguage == item,
                        onClick = { viewModel.setLanguage(item) },
                    )
                    if (index < UiLanguage.entries.lastIndex) HorizontalDivider(Modifier.padding(horizontal = 16.dp))
                }
            }
        }
        Text(
            language.text("Historia połączeń", "Call history", "Історія дзвінків"),
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.padding(top = 28.dp, bottom = 10.dp),
        )
        Surface(shape = MaterialTheme.shapes.extraLarge, tonalElevation = 1.dp) {
            Column {
                HistoryRetention.entries.forEachIndexed { index, retention ->
                    HistoryRetentionRow(
                        retention = retention,
                        language = language,
                        selected = preferences.historyRetention == retention,
                        onClick = { viewModel.setHistoryRetention(retention) },
                    )
                    if (index < HistoryRetention.entries.lastIndex) {
                        HorizontalDivider(Modifier.padding(horizontal = 16.dp))
                    }
                }
            }
        }
        Text(
            language.text("Diagnostyka", "Diagnostics", "Діагностика"),
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
                    Text(language.text("Stan aplikacji", "Local health details", "Стан застосунку"), style = MaterialTheme.typography.titleMedium)
                    Text(
                        language.text(
                            "Pokazuje tylko bezpieczne dane diagnostyczne",
                            "Shows sanitized app status only",
                            "Показує лише безпечні діагностичні дані",
                        ),
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
            language.text(
                "Oponexis Mobile Companion ${BuildConfig.VERSION_NAME}\nŚrodowisko ${BuildConfig.BUILD_TYPE}",
                "Oponexis Mobile Companion ${BuildConfig.VERSION_NAME}\n${BuildConfig.BUILD_TYPE} environment",
                "Oponexis Mobile Companion ${BuildConfig.VERSION_NAME}\nСередовище ${BuildConfig.BUILD_TYPE}",
            ),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 28.dp),
        )
    }
}

@Composable
private fun HistoryRetentionRow(
    retention: HistoryRetention,
    language: UiLanguage,
    selected: Boolean,
    onClick: () -> Unit,
) {
    val label = when (retention) {
        HistoryRetention.ThirtyDays -> language.text("Przechowuj przez 30 dni", "Keep for 30 days", "Зберігати 30 днів")
        HistoryRetention.Forever -> language.text("Przechowuj bezterminowo", "Keep until manually changed", "Зберігати безстроково")
    }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(label, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
        RadioButton(selected = selected, onClick = onClick)
    }
}

@Composable
private fun ThemeRow(theme: ThemePreference, language: UiLanguage, selected: Boolean, onClick: () -> Unit) {
    val (label, icon) = when (theme) {
        ThemePreference.System -> language.text("Zgodnie z urządzeniem", "Use device setting", "Як на пристрої") to Icons.Rounded.SettingsBrightness
        ThemePreference.Light -> language.text("Jasny", "Light", "Світла") to Icons.Rounded.LightMode
        ThemePreference.Dark -> language.text("Ciemny", "Dark", "Темна") to Icons.Rounded.DarkMode
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

@Composable
private fun LanguageRow(language: UiLanguage, selected: Boolean, onClick: () -> Unit) {
    val label = when (language) {
        UiLanguage.Polish -> "Polski"
        UiLanguage.English -> "English"
        UiLanguage.Ukrainian -> "Українська"
    }
    Row(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick).padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(label, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
        RadioButton(selected = selected, onClick = onClick)
    }
}
