package com.oponexis.companion.domain.repository

import com.oponexis.companion.domain.model.ThemePreference
import com.oponexis.companion.domain.model.UserPreferences
import kotlinx.coroutines.flow.Flow

interface SettingsRepository {
    val preferences: Flow<UserPreferences>
    suspend fun completeOnboarding()
    suspend fun setTheme(theme: ThemePreference)
    suspend fun setDiagnosticDetails(enabled: Boolean)
}

