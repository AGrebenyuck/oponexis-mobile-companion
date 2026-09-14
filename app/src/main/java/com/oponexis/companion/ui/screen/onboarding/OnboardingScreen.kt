package com.oponexis.companion.ui.screen.onboarding

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CloudDone
import androidx.compose.material.icons.rounded.ContactPhone
import androidx.compose.material.icons.rounded.PrivacyTip
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.oponexis.companion.domain.repository.SettingsRepository
import com.oponexis.companion.ui.components.OponexisWordmark
import com.oponexis.companion.ui.localization.LocalUiLanguage
import com.oponexis.companion.ui.localization.text
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class OnboardingViewModel @Inject constructor(
    private val settingsRepository: SettingsRepository,
) : ViewModel() {
    fun complete(onComplete: () -> Unit) {
        viewModelScope.launch {
            settingsRepository.completeOnboarding()
            onComplete()
        }
    }
}

@Composable
fun OnboardingRoute(
    onComplete: () -> Unit,
    viewModel: OnboardingViewModel = hiltViewModel(),
) {
    OnboardingScreen(onContinue = { viewModel.complete(onComplete) })
}

@Composable
private fun OnboardingScreen(onContinue: () -> Unit) {
    val language = LocalUiLanguage.current
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .safeDrawingPadding()
            .padding(horizontal = 24.dp, vertical = 28.dp),
    ) {
        OponexisWordmark()
        Spacer(Modifier.height(48.dp))
        Text(language.text("Kontekst wtedy, gdy go potrzebujesz.", "Context when it matters.", "Контекст саме тоді, коли він потрібен."), style = MaterialTheme.typography.displaySmall)
        Text(
            text = language.text(
                "Prosty pomocnik do sprawniejszej obsługi klientów — bez zbędnych przeszkód.",
                "A focused companion for clearer customer conversations — designed to stay out of your way.",
                "Зручний помічник для кращого спілкування з клієнтами — без зайвих перешкод.",
            ),
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 16.dp, bottom = 32.dp),
        )
        Feature(Icons.Rounded.ContactPhone, language.text("Rozpoznawaj klienta", "Recognize context", "Розпізнавайте клієнта"), language.text("Zobacz krótki opis klienta, gdy dane są dostępne.", "See a concise customer summary when supported.", "Переглядайте коротку інформацію про клієнта, коли дані доступні."))
        Feature(Icons.Rounded.CloudDone, language.text("Niezawodne działanie", "Built for reliability", "Надійна робота"), language.text("Lokalne dane pomagają pracować także przy słabszej sieci.", "Local-first foundations keep work resilient.", "Локальні дані допомагають працювати навіть за слабкої мережі."))
        Feature(
            Icons.Rounded.PrivacyTip,
            language.text("Prywatność domyślnie", "Private by default", "Приватність за замовчуванням"),
            language.text("Dostęp do kontaktów jest opcjonalny i służy tylko rozpoznawaniu zapisanych numerów.", "Contact access is optional and requested only to support saved callers.", "Доступ до контактів необов’язковий і потрібен лише для розпізнавання збережених номерів."),
        )
        Spacer(Modifier.height(40.dp))
        Button(
            onClick = onContinue,
            modifier = Modifier.fillMaxWidth(),
            shape = MaterialTheme.shapes.large,
        ) {
            Text(language.text("Otwórz Companion", "Enter companion", "Відкрити Companion"), modifier = Modifier.padding(vertical = 6.dp))
        }
    }
}

@Composable
private fun Feature(icon: ImageVector, title: String, description: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 9.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier
                .size(46.dp)
                .background(MaterialTheme.colorScheme.primaryContainer, CircleShape)
                .padding(11.dp),
        )
        Column {
            Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            Text(description, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}
