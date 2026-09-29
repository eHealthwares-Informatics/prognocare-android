package com.ehealthinformatics.prognocare.feature.forms

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Description
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ehealthinformatics.prognocare.data.remote.models.FormDefinition
import com.ehealthinformatics.prognocare.designsystem.theme.Spacing

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FormPickerScreen(
    patientId: String? = null,
    visitId: String? = null,
    encounterId: String? = null,
    onPick: (formId: String, patientId: String?, visitId: String?, encounterId: String?) -> Unit,
    onBack: () -> Unit,
    viewModel: FormViewModel = hiltViewModel(),
) {
    val available by viewModel.available.collectAsStateWithLifecycle()
    val loadError by viewModel.loadError.collectAsStateWithLifecycle()
    val loading by viewModel.loadingAvailable.collectAsStateWithLifecycle()

    // When no patient context is provided (e.g. the doctor's "Clinical Note"
    // quick action), ask for the patient/visit/encounter first — the same
    // scope flow as the New Request screen.
    var scopePatientId by androidx.compose.runtime.remember {
        androidx.compose.runtime.mutableStateOf<String?>(null)
    }
    var scopeVisitId by androidx.compose.runtime.remember {
        androidx.compose.runtime.mutableStateOf<String?>(null)
    }
    var scopeEncounterId by androidx.compose.runtime.remember {
        androidx.compose.runtime.mutableStateOf<String?>(null)
    }
    val activePatientId = patientId?.takeIf { it.isNotBlank() } ?: scopePatientId
    val activeVisitId = visitId?.takeIf { it.isNotBlank() } ?: scopeVisitId
    val activeEncounterId = encounterId?.takeIf { it.isNotBlank() } ?: scopeEncounterId
    val needsScope = activePatientId.isNullOrBlank() && activeVisitId.isNullOrBlank() && activeEncounterId.isNullOrBlank()

    LaunchedEffect(Unit) {
        viewModel.loadAvailable()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Documentation",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
            )
        },
    ) { innerPadding ->
        if (needsScope) {
            DocumentationScopePicker(
                onScopeResolved = { pid, vid, eid ->
                    scopePatientId = pid
                    scopeVisitId = vid
                    scopeEncounterId = eid
                },
            )
        } else {
            PullToRefreshBox(
                isRefreshing = loading,
                onRefresh = { viewModel.loadAvailable() },
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
            ) {
                when {
                    loadError != null -> ErrorState(error = loadError!!, retry = viewModel::loadAvailable, padding = PaddingValues(0.dp))
                    available.isEmpty() && loadError == null -> EmptyState(padding = PaddingValues(0.dp))
                    else -> FormList(
                        forms = available,
                        padding = PaddingValues(0.dp),
                        onPick = { formId ->
                            onPick(formId, activePatientId, activeVisitId, activeEncounterId)
                        },
                    )
                }
            }
        }
    }
}

/**
 * Patient → Visit → Encounter scope resolution, mirroring the New Request
 * screen: search patients, or attach to an ongoing visit / active encounter.
 * The chosen scope is handed back to the caller through [onScopeResolved].
 */
@Composable
private fun DocumentationScopePicker(
    onScopeResolved: (String, String?, String?) -> Unit,
    viewModel: FormScopeViewModel = androidx.hilt.navigation.compose.hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    var patientSearch by androidx.compose.runtime.remember {
        androidx.compose.runtime.mutableStateOf("")
    }

    LaunchedEffect(patientSearch) {
        viewModel.searchPatients(patientSearch)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(Spacing.lg),
        verticalArrangement = Arrangement.spacedBy(Spacing.md),
    ) {
        Text(
            text = "Who is this documentation for?",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
        )
        Text(
            text = "Pick the patient, an ongoing visit or an active encounter — the same scoping as a new request.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        androidx.compose.material3.OutlinedTextField(
            value = patientSearch,
            onValueChange = { patientSearch = it },
            modifier = Modifier.fillMaxWidth(),
            label = { Text("Search patient by name or MRN") },
            singleLine = true,
        )
        LazyColumn(modifier = Modifier.fillMaxWidth().height(160.dp)) {
            items(state.patientResults) { patient ->
                androidx.compose.material3.ListItem(
                    headlineContent = { Text("${patient.displayName} · ${patient.patientId}") },
                    modifier = Modifier.clickable {
                        onScopeResolved(patient.patientId.ifBlank { patient.id }, null, null)
                    },
                )
            }
        }

        Text(
            text = "…or attach to an ongoing visit",
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.SemiBold,
        )
        if (state.visits.isEmpty() && !state.isLoading) {
            Text(
                "No patients with open visits",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        LazyColumn(modifier = Modifier.fillMaxWidth().height(140.dp)) {
            items(state.visits, key = { it.id }) { visit ->
                androidx.compose.material3.ListItem(
                    headlineContent = { Text(visit.patientName.ifBlank { "Patient ${visit.patientId}" }) },
                    supportingContent = { Text(visit.typeDisplay) },
                    modifier = Modifier.clickable {
                        onScopeResolved(visit.patientId, visit.id, null)
                    },
                )
            }
        }

        Text(
            text = "…or an active encounter",
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.SemiBold,
        )
        LazyColumn(modifier = Modifier.fillMaxWidth().height(140.dp)) {
            items(state.encounters, key = { it.id }) { encounter ->
                androidx.compose.material3.ListItem(
                    headlineContent = { Text("Patient ${encounter.patientId} · ${encounter.typeDisplay}") },
                    supportingContent = { Text(encounter.encounterDatetime?.take(10) ?: "") },
                    modifier = Modifier.clickable {
                        onScopeResolved(encounter.patientId, encounter.visitId, encounter.id)
                    },
                )
            }
        }
    }
}

@Composable
private fun FormList(
    forms: List<FormDefinition>,
    padding: PaddingValues,
    onPick: (String) -> Unit,
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(padding),
        contentPadding = PaddingValues(Spacing.xl),
        verticalArrangement = Arrangement.spacedBy(Spacing.md),
    ) {
        items(forms, key = { it.id }) { form ->
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onPick(form.id) },
                shape = RoundedCornerShape(Spacing.md),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                ),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(Spacing.md),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(
                        Icons.Filled.Description,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                    )
                    Column(Modifier.weight(1f).padding(start = Spacing.md)) {
                        Text(
                            text = form.name,
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.SemiBold,
                        )
                        Spacer(Modifier.height(Spacing.xxs))
                        Text(
                            text = form.description ?: form.code,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun EmptyState(padding: PaddingValues) {
    Column(
        modifier = Modifier.fillMaxSize().padding(padding),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text("No published forms available", style = MaterialTheme.typography.bodyLarge)
    }
}

@Composable
private fun ErrorState(error: String, retry: () -> Unit, padding: PaddingValues) {
    Column(
        modifier = Modifier.fillMaxSize().padding(padding),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text(
            text = error,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.error,
        )
        Spacer(Modifier.height(Spacing.md))
        androidx.compose.material3.Button(onClick = retry) { Text("Retry") }
    }
}
