package com.oponexis.companion.data.preferences

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.oponexis.companion.domain.model.ThemePreference
import com.oponexis.companion.domain.model.HistoryRetention
import com.oponexis.companion.domain.model.UserPreferences
import com.oponexis.companion.domain.model.UiLanguage
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
            historyRetention = values[HISTORY_RETENTION]
                ?.let(::historyRetentionFromStorage)
                ?: HistoryRetention.ThirtyDays,
            uiLanguage = values[UI_LANGUAGE]?.let(::languageFromStorage) ?: UiLanguage.Polish,
            customConversationTopics = values[CUSTOM_CONVERSATION_TOPICS].orEmpty(),
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

    override suspend fun setHistoryRetention(retention: HistoryRetention) {
        context.settingsDataStore.edit { it[HISTORY_RETENTION] = retention.name }
    }

    override suspend fun setLanguage(language: UiLanguage) {
        context.settingsDataStore.edit { it[UI_LANGUAGE] = language.name }
    }

    override suspend fun addConversationTopic(topic: String) {
        val normalized = topic.trim().take(120)
        if (normalized.isEmpty()) return
        context.settingsDataStore.edit { values ->
            values[CUSTOM_CONVERSATION_TOPICS] = values[CUSTOM_CONVERSATION_TOPICS].orEmpty() + normalized
        }
    }

    override suspend fun removeConversationTopic(topic: String) {
        context.settingsDataStore.edit { values ->
            values[CUSTOM_CONVERSATION_TOPICS] = values[CUSTOM_CONVERSATION_TOPICS].orEmpty() - topic
        }
    }

    private fun themeFromStorage(value: String): ThemePreference =
        ThemePreference.entries.firstOrNull { it.name == value } ?: ThemePreference.System

    private fun historyRetentionFromStorage(value: String): HistoryRetention =
        HistoryRetention.entries.firstOrNull { it.name == value } ?: HistoryRetention.ThirtyDays

    private fun languageFromStorage(value: String): UiLanguage =
        UiLanguage.entries.firstOrNull { it.name == value } ?: UiLanguage.Polish

    private companion object {
        val ONBOARDING_COMPLETE = booleanPreferencesKey("onboarding_complete")
        val THEME = stringPreferencesKey("theme")
        val DIAGNOSTIC_DETAILS = booleanPreferencesKey("diagnostic_details")
        val HISTORY_RETENTION = stringPreferencesKey("history_retention")
        val UI_LANGUAGE = stringPreferencesKey("ui_language")
        val CUSTOM_CONVERSATION_TOPICS = stringSetPreferencesKey("custom_conversation_topics")
    }
}
