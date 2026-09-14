package com.oponexis.companion.ui.screen.dashboard

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Sync
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.oponexis.companion.domain.model.DashboardSnapshot
import com.oponexis.companion.domain.repository.CompanionRepository
import com.oponexis.companion.ui.components.CallRow
import com.oponexis.companion.ui.components.OponexisWordmark
import com.oponexis.companion.ui.theme.BrandBlue
import com.oponexis.companion.ui.theme.BrandMint
import com.oponexis.companion.ui.localization.LocalUiLanguage
import com.oponexis.companion.ui.localization.text
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.StateFlow
import javax.inject.Inject

@HiltViewModel
class DashboardViewModel @Inject constructor(repository: CompanionRepository) : ViewModel() {
    val state: StateFlow<DashboardSnapshot> = repository.dashboard
}

@Composable
fun DashboardRoute(
    contentPadding: PaddingValues,
    viewModel: DashboardViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    DashboardScreen(state, contentPadding)
}

@Composable
private fun DashboardScreen(state: DashboardSnapshot, contentPadding: PaddingValues) {
    val language = LocalUiLanguage.current
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(
            start = 20.dp,
            top = contentPadding.calculateTopPadding() + 20.dp,
            end = 20.dp,
            bottom = contentPadding.calculateBottomPadding() + 24.dp,
        ),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        item {
            OponexisWordmark()
            Spacer(Modifier.height(28.dp))
            Text(language.text("Przegląd aktywności", "Activity overview", "Огляд активності"), style = MaterialTheme.typography.headlineMedium)
            Text(
                language.text("Połączenia i synchronizacja CRM z tego urządzenia.", "Real call and CRM sync status from this device.", "Дзвінки та синхронізація CRM із цього пристрою."),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        item { HeroCard(state, language.text("połączeń dzisiaj", "calls today", "дзвінків сьогодні")) }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                MetricCard(language.text("Rozpoznane", "Identified", "Розпізнані"), state.identifiedCalls.toString(), Icons.Rounded.CheckCircle, Modifier.weight(1f))
                MetricCard(language.text("Oczekujące", "Pending", "Очікують"), state.pendingEvents.toString(), Icons.Rounded.Sync, Modifier.weight(1f))
            }
        }
        item {
            Text(
                language.text("Ostatnie połączenia", "Recent calls", "Останні дзвінки"),
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier.padding(top = 10.dp, bottom = 2.dp),
            )
        }
        items(state.recentCalls, key = { it.id }) { call -> CallRow(call) }
    }
}

@Composable
private fun HeroCard(state: DashboardSnapshot, callsTodayLabel: String) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 8.dp),
        shape = MaterialTheme.shapes.extraLarge,
        color = Color.Transparent,
    ) {
        Column(
            modifier = Modifier
                .background(Brush.linearGradient(listOf(BrandBlue, Color(0xFF114AB8))))
                .padding(22.dp),
        ) {
            Icon(
                Icons.Rounded.AutoAwesome,
                contentDescription = null,
                tint = BrandMint,
                modifier = Modifier.size(28.dp),
            )
            Spacer(Modifier.height(24.dp))
            Text(
                text = state.callsToday.toString(),
                color = Color.White,
                style = MaterialTheme.typography.displaySmall,
                fontWeight = FontWeight.Bold,
            )
            Text(callsTodayLabel, color = Color.White.copy(alpha = 0.76f), style = MaterialTheme.typography.bodyLarge)
        }
    }
}

@Composable
private fun MetricCard(label: String, value: String, icon: ImageVector, modifier: Modifier = Modifier) {
    Surface(modifier = modifier, shape = MaterialTheme.shapes.large, tonalElevation = 1.dp) {
        Column(Modifier.padding(16.dp)) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier
                    .size(34.dp)
                    .background(MaterialTheme.colorScheme.primaryContainer, CircleShape)
                    .padding(7.dp),
            )
            Text(value, style = MaterialTheme.typography.headlineMedium, modifier = Modifier.padding(top = 16.dp))
            Text(label, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}
