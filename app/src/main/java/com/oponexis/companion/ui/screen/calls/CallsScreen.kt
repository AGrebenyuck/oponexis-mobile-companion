package com.oponexis.companion.ui.screen.calls

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.oponexis.companion.domain.model.CallPreview
import com.oponexis.companion.domain.repository.CompanionRepository
import com.oponexis.companion.ui.components.CallRow
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.StateFlow
import javax.inject.Inject

@HiltViewModel
class CallsViewModel @Inject constructor(repository: CompanionRepository) : ViewModel() {
    val calls: StateFlow<List<CallPreview>> = repository.calls
}

@Composable
fun CallsRoute(
    contentPadding: PaddingValues,
    viewModel: CallsViewModel = hiltViewModel(),
) {
    val calls by viewModel.calls.collectAsStateWithLifecycle()
    var query by rememberSaveable { mutableStateOf("") }
    val filteredCalls = calls.filter {
        query.isBlank() || it.displayName.contains(query, ignoreCase = true) ||
            it.company?.contains(query, ignoreCase = true) == true
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(
            start = 20.dp,
            top = contentPadding.calculateTopPadding() + 24.dp,
            end = 20.dp,
            bottom = contentPadding.calculateBottomPadding() + 24.dp,
        ),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            Text("Calls", style = MaterialTheme.typography.headlineMedium)
            Text(
                "Local mock history. CRM integration is not active.",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 4.dp, bottom = 20.dp),
            )
            OutlinedTextField(
                value = query,
                onValueChange = { query = it },
                modifier = Modifier.fillParentMaxWidth(),
                placeholder = { Text("Search calls") },
                leadingIcon = { Icon(Icons.Rounded.Search, contentDescription = null) },
                singleLine = true,
                shape = MaterialTheme.shapes.large,
            )
        }
        items(filteredCalls, key = { it.id }) { call -> CallRow(call) }
        if (filteredCalls.isEmpty()) {
            item {
                Text(
                    "No mock calls match your search.",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(vertical = 28.dp),
                )
            }
        }
    }
}
