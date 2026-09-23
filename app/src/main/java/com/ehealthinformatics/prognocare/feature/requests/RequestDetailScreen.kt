package com.ehealthinformatics.prognocare.feature.requests

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ehealthinformatics.prognocare.designsystem.components.EmptyState
import com.ehealthinformatics.prognocare.designsystem.components.ErrorState
import com.ehealthinformatics.prognocare.designsystem.theme.Spacing

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RequestDetailScreen(
    requestId: String,
    onBack: () -> Unit,
    viewModel: RequestDetailViewModel = hiltViewModel(key = "request-$requestId"),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    LaunchedEffect(requestId) {
        viewModel.bind(requestId)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Request Detail") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                ),
            )
        },
    ) { innerPadding ->
        when {
            state.isLoading -> {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                ) {
                    CircularProgressIndicator()
                }
            }
            state.request == null -> {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding),
                ) {
                    ErrorState(
                        message = state.error ?: "Request not found",
                        onRetry = viewModel::load,
                    )
                }
            }
            else -> {
                val request = state.request!!
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding),
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(Spacing.lg),
                    verticalArrangement = Arrangement.spacedBy(Spacing.md),
                ) {
                    // ── Header ────────────────────────────────────
                    item {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(Spacing.base),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surface,
                            ),
                        ) {
                            Column(modifier = Modifier.padding(Spacing.base)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically,
                                ) {
                                    Text(
                                        text = request.typeDisplay,
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold,
                                    )
                                    RequestStatusBadge(request.status)
                                }
                                Spacer(modifier = Modifier.height(Spacing.xs))
                                Text(
                                    text = listOfNotNull(
                                        request.patientName.takeIf { it.isNotBlank() },
                                        request.requestNumber,
                                    ).joinToString(" · "),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                                Text(
                                    text = "Provider: ${request.orderingProviderName ?: "—"} · " +
                                        "Priority: ${request.priorityDisplay}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                                if (!request.diagnosis.isNullOrBlank()) {
                                    Text(
                                        text = "Diagnosis: ${request.diagnosis}",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    )
                                }
                                if (!request.clinicalNotes.isNullOrBlank()) {
                                    Text(
                                        text = request.clinicalNotes,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    )
                                }
                                request.syncStatus?.let { syncStatus ->
                                    Text(
                                        text = when (syncStatus) {
                                            "SYNCED" -> "External order: ${request.externalReference ?: "synced"}"
                                            "FAILED" -> "Sync failed: ${request.syncError ?: "error"}"
                                            else -> "Sync: $syncStatus"
                                        },
                                        style = MaterialTheme.typography.labelSmall,
                                        color = if (syncStatus == "FAILED") {
                                            MaterialTheme.colorScheme.error
                                        } else {
                                            MaterialTheme.colorScheme.primary
                                        },
                                    )
                                }
                            }
                        }
                    }

                    // ── Status actions ────────────────────────────
                    if (!request.isTerminal) {
                        item {
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(Spacing.base),
                                colors = CardDefaults.cardColors(
                                    containerColor = MaterialTheme.colorScheme.surface,
                                ),
                            ) {
                                Column(modifier = Modifier.padding(Spacing.base)) {
                                    Text(
                                        text = "Actions",
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.SemiBold,
                                    )
                                    Spacer(modifier = Modifier.height(Spacing.sm))
                                    Row(horizontalArrangement = Arrangement.spacedBy(Spacing.sm)) {
                                        if (request.status == "REQUESTED") {
                                            androidx.compose.material3.OutlinedButton(
                                                onClick = { viewModel.transition("IN_PROGRESS") },
                                                enabled = !state.isBusy,
                                            ) { Text("Start") }
                                        }
                                        androidx.compose.material3.OutlinedButton(
                                            onClick = { viewModel.transition("COMPLETED") },
                                            enabled = !state.isBusy,
                                        ) { Text("Complete") }
                                        androidx.compose.material3.OutlinedButton(
                                            onClick = { viewModel.transition("CANCELLED", "Cancelled from mobile") },
                                            enabled = !state.isBusy,
                                        ) { Text("Cancel") }
                                    }
                                }
                            }
                        }
                    }

                    // ── Line items ────────────────────────────────
                    item {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(Spacing.base),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surface,
                            ),
                        ) {
                            Column(modifier = Modifier.padding(Spacing.base)) {
                                Text(
                                    text = "Items (${request.items.size})",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.SemiBold,
                                )
                                Spacer(modifier = Modifier.height(Spacing.sm))
                                request.items.forEach { item ->
                                    Column(modifier = Modifier.padding(vertical = Spacing.xs)) {
                                        Text(
                                            text = item.name,
                                            style = MaterialTheme.typography.bodyMedium,
                                            fontWeight = FontWeight.Medium,
                                        )
                                        val detail = listOfNotNull(
                                            item.dose?.let { d -> item.doseUnit?.let { u -> "$d $u" } ?: d },
                                            item.frequency,
                                            item.route,
                                            item.quantity?.toString()?.let { q -> "Qty $q" },
                                            item.instructions,
                                        ).joinToString(" · ")
                                        if (detail.isNotBlank()) {
                                            Text(
                                                text = detail,
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            )
                                        }
                                    }
                                    HorizontalDivider(
                                        color = MaterialTheme.colorScheme.surfaceVariant,
                                    )
                                }
                            }
                        }
                    }

                    // ── Timeline ──────────────────────────────────
                    item {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(Spacing.base),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surface,
                            ),
                        ) {
                            Column(modifier = Modifier.padding(Spacing.base)) {
                                Text(
                                    text = "Activity",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.SemiBold,
                                )
                                Spacer(modifier = Modifier.height(Spacing.sm))
                                if (state.history.isEmpty()) {
                                    Text(
                                        text = "No activity recorded",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    )
                                } else {
                                    state.history.forEach { entry ->
                                        Column(modifier = Modifier.padding(vertical = Spacing.xs)) {
                                            Text(
                                                text = entry.toStatus.replace("_", " ").lowercase()
                                                    .replaceFirstChar { it.uppercase() },
                                                style = MaterialTheme.typography.bodyMedium,
                                                fontWeight = FontWeight.Medium,
                                            )
                                            Text(
                                                text = listOfNotNull(
                                                    entry.actorName,
                                                    entry.createdAt?.take(16)?.replace('T', ' '),
                                                    entry.reason,
                                                ).joinToString(" · "),
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // ── Add note ──────────────────────────────────
                    item {
                        var note by remember { mutableStateOf("") }
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(Spacing.base),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surface,
                            ),
                        ) {
                            Column(modifier = Modifier.padding(Spacing.base)) {
                                Text(
                                    text = "Add note",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.SemiBold,
                                )
                                Spacer(modifier = Modifier.height(Spacing.sm))
                                OutlinedTextField(
                                    value = note,
                                    onValueChange = { note = it },
                                    modifier = Modifier.fillMaxWidth(),
                                    placeholder = { Text("Write a note…") },
                                    minLines = 2,
                                )
                                Spacer(modifier = Modifier.height(Spacing.sm))
                                androidx.compose.material3.Button(
                                    onClick = {
                                        viewModel.addNote(note)
                                        note = ""
                                    },
                                    enabled = !state.isBusy && note.isNotBlank(),
                                ) { Text("Save note") }
                            }
                        }
                    }
                }
            }
        }
    }
}
