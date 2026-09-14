package com.oponexis.companion.domain.repository

import com.oponexis.companion.domain.model.ThemePreference
import com.oponexis.companion.domain.model.HistoryRetention
import com.oponexis.companion.domain.model.UserPreferences
import com.oponexis.companion.domain.model.UiLanguage
import kotlinx.coroutines.flow.Flow

interface SettingsRepository {
    val preferences: Flow<UserPreferences>
    suspend fun completeOnboarding()
    suspend fun setTheme(theme: ThemePreference)
    suspend fun setDiagnosticDetails(enabled: Boolean)
    suspend fun setHistoryRetention(retention: HistoryRetention)
    suspend fun setLanguage(language: UiLanguage)
    suspend fun addConversationTopic(topic: String)
    suspend fun removeConversationTopic(topic: String)
}
