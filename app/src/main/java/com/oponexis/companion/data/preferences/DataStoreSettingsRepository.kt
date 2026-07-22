package com.oponexis.companion.data.preferences

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.oponexis.companion.domain.model.ThemePreference
import com.oponexis.companion.domain.model.UserPreferences
import com.oponexis.companion.domain.repository.SettingsRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

private val Context.settingsDataStore by preferencesDataStore(name = "companion_settings")

@Singleton
class DataStoreSettingsRepository @Inject constructor(
    @ApplicationContext private val context: Context,
) : SettingsRepository {
    override val preferences: Flow<UserPreferences> = context.settingsDataStore.data.map { values ->
        UserPreferences(
            onboardingComplete = values[ONBOARDING_COMPLETE] ?: false,
            themePreference = values[THEME]?.let(::themeFromStorage) ?: ThemePreference.System,
            diagnosticDetailsEnabled = values[DIAGNOSTIC_DETAILS] ?: true,
        )
    }

    override suspend fun completeOnboarding() {
        context.settingsDataStore.edit { it[ONBOARDING_COMPLETE] = true }
    }

    override suspend fun setTheme(theme: ThemePreference) {
        context.settingsDataStore.edit { it[THEME] = theme.name }
    }

    override suspend fun setDiagnosticDetails(enabled: Boolean) {
        context.settingsDataStore.edit { it[DIAGNOSTIC_DETAILS] = enabled }
    }

    private fun themeFromStorage(value: String): ThemePreference =
        ThemePreference.entries.firstOrNull { it.name == value } ?: ThemePreference.System

    private companion object {
        val ONBOARDING_COMPLETE = booleanPreferencesKey("onboarding_complete")
        val THEME = stringPreferencesKey("theme")
        val DIAGNOSTIC_DETAILS = booleanPreferencesKey("diagnostic_details")
    }
}

