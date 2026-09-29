package com.ehealthinformatics.prognocare.feature.requests

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Biotech
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Medication
import androidx.compose.material.icons.filled.MonitorHeart
import androidx.compose.material.icons.filled.Science
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
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
import com.ehealthinformatics.prognocare.designsystem.theme.Spacing
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.launch

private val REQUEST_TYPES = listOf("LAB", "RADIOLOGY", "OTHER_TEST", "PRESCRIPTION")
private val PRIORITIES = listOf("ROUTINE", "URGENT", "EMERGENCY")

private fun typeIcon(type: String) = when (type) {
    "LAB" -> Icons.Filled.Science
    "RADIOLOGY" -> Icons.Filled.Biotech
    "PRESCRIPTION" -> Icons.Filled.Medication
    else -> Icons.Filled.MonitorHeart
}

@OptIn(ExperimentalMaterial3Api::class, FlowPreview::class, ExperimentalLayoutApi::class)
@Composable
fun CreateRequestScreen(
    patientId: String? = null,
    patientName: String? = null,
    onBack: () -> Unit,
    onSaved: () -> Unit,
    viewModel: CreateRequestViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    LaunchedEffect(Unit) {
        viewModel.events.collect { event ->
            when (event) {
                is RequestUiEvent.Success -> onSaved()
                is RequestUiEvent.Error -> Unit // error text shown inline below
            }
        }
    }

    var patientSearch by remember { mutableStateOf("") }
    LaunchedEffect(patientSearch) {
        if (patientSearch.isNotBlank() && state.selectedPatient == null) {
            viewModel.searchPatients(patientSearch)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("New Request") },
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
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(Spacing.lg),
            verticalArrangement = Arrangement.spacedBy(Spacing.md),
        ) {
            // ── Scope: Patient / Visit / Encounter ────────────
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(Spacing.base),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            ) {
                Column(modifier = Modifier.padding(Spacing.base)) {
                    Text(
                        text = "Request for",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold,
                    )
                    Spacer(modifier = Modifier.height(Spacing.xs))
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(Spacing.sm)) {
                        RequestScope.entries.forEach { scope ->
                            FilterChip(
                                selected = state.scope == scope,
                                onClick = { viewModel.setScope(scope) },
                                label = { Text(scope.label) },
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(Spacing.sm))
                    when (state.scope) {
                        RequestScope.PATIENT -> PatientScopePicker(viewModel, state, patientSearch)
                        RequestScope.VISIT -> VisitScopePicker(viewModel, state)
                        RequestScope.ENCOUNTER -> EncounterScopePicker(viewModel, state)
                    }
                }
            }

            // ── Type & priority ──────────────────────────────
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(Spacing.base),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            ) {
                Column(modifier = Modifier.padding(Spacing.base)) {
                    Text(
                        text = "Type",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold,
                    )
                    Spacer(modifier = Modifier.height(Spacing.xs))
                    // FlowRow so the chips wrap to the next line instead of
                    // overflowing off-screen.
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(Spacing.sm)) {
                        REQUEST_TYPES.forEach { type ->
                            FilterChip(
                                selected = state.requestType == type,
                                onClick = { viewModel.update { it.copy(requestType = type) } },
                                leadingIcon = {
                                    Icon(
                                        imageVector = typeIcon(type),
                                        contentDescription = null,
                                        modifier = Modifier.size(16.dp),
                                    )
                                },
                                label = {
                                    Text(type.replace("_", " ").lowercase().replaceFirstChar { c -> c.uppercase() })
                                },
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(Spacing.sm))
                    Text(
                        text = "Priority",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold,
                    )
                    Spacer(modifier = Modifier.height(Spacing.xs))
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(Spacing.sm)) {
                        PRIORITIES.forEach { priority ->
                            FilterChip(
                                selected = state.priority == priority,
                                onClick = { viewModel.update { it.copy(priority = priority) } },
                                label = {
                                    Text(priority.lowercase().replaceFirstChar { c -> c.uppercase() })
                                },
                            )
                        }
                    }
                }
            }

            // ── Diagnosis & notes ────────────────────────────
            OutlinedTextField(
                value = state.diagnosis,
                onValueChange = { v -> viewModel.update { it.copy(diagnosis = v) } },
                modifier = Modifier.fillMaxWidth(),
                label = { Text("Diagnosis / indication") },
                singleLine = true,
            )
            OutlinedTextField(
                value = state.clinicalNotes,
                onValueChange = { v -> viewModel.update { it.copy(clinicalNotes = v) } },
                modifier = Modifier.fillMaxWidth(),
                label = { Text("Clinical notes") },
                minLines = 2,
            )

            // ── Items ────────────────────────────────────────
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(Spacing.base),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            ) {
                Column(modifier = Modifier.padding(Spacing.base)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            text = "Items",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.SemiBold,
                        )
                        OutlinedButton(onClick = { viewModel.addItem() }) { Text("Add") }
                    }
                    state.items.forEachIndexed { index, item ->
                        Column(modifier = Modifier.padding(vertical = Spacing.xs)) {
                            OutlinedTextField(
                                value = item.name,
                                onValueChange = { v -> viewModel.updateItem(index) { it.copy(name = v) } },
                                modifier = Modifier.fillMaxWidth(),
                                label = { Text("Item ${index + 1} name") },
                                singleLine = true,
                            )
                            if (state.requestType == "PRESCRIPTION") {
                                Row(horizontalArrangement = Arrangement.spacedBy(Spacing.sm)) {
                                    OutlinedTextField(
                                        value = item.dose.orEmpty(),
                                        onValueChange = { v -> viewModel.updateItem(index) { it.copy(dose = v) } },
                                        modifier = Modifier.weight(1f),
                                        label = { Text("Dose") },
                                        singleLine = true,
                                    )
                                    OutlinedTextField(
                                        value = item.frequency.orEmpty(),
                                        onValueChange = { v -> viewModel.updateItem(index) { it.copy(frequency = v) } },
                                        modifier = Modifier.weight(1f),
                                        label = { Text("Frequency") },
                                        singleLine = true,
                                    )
                                }
                                Row(horizontalArrangement = Arrangement.spacedBy(Spacing.sm)) {
                                    OutlinedTextField(
                                        value = item.route.orEmpty(),
                                        onValueChange = { v -> viewModel.updateItem(index) { it.copy(route = v) } },
                                        modifier = Modifier.weight(1f),
                                        label = { Text("Route") },
                                        singleLine = true,
                                    )
                                    OutlinedTextField(
                                        value = item.quantity?.toString().orEmpty(),
                                        onValueChange = { v ->
                                            viewModel.updateItem(index) { it.copy(quantity = v.toIntOrNull()) }
                                        },
                                        modifier = Modifier.weight(1f),
                                        label = { Text("Qty") },
                                        singleLine = true,
                                    )
                                }
                            }
                            if (state.items.size > 1) {
                                OutlinedButton(onClick = { viewModel.removeItem(index) }) {
                                    Text("Remove item ${index + 1}")
                                }
                            }
                        }
                    }
                }
            }

            state.error?.let {
                Text(text = it, color = MaterialTheme.colorScheme.error)
            }

            Button(
                onClick = { viewModel.save() },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                enabled = !state.isSaving,
            ) {
                if (state.isSaving) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(18.dp),
                        strokeWidth = 2.dp,
                        color = MaterialTheme.colorScheme.onPrimary,
                    )
                    Spacer(modifier = Modifier.size(Spacing.sm))
                }
                Text(if (state.isSaving) "Saving…" else "Create request")
            }
        }
    }
}

/** Patient scope: free search; patients covered by a visit/encounter are hidden. */
@Composable
private fun PatientScopePicker(
    viewModel: CreateRequestViewModel,
    state: CreateRequestState,
    patientSearch: String,
) {
    if (state.selectedPatient != null) {
        ListItem(
            headlineContent = {
                Text("${state.selectedPatient!!.displayName} (${state.selectedPatient!!.patientId})")
            },
            trailingContent = {
                IconButton(onClick = { viewModel.clearSelection() }) {
                    Icon(Icons.Default.Close, contentDescription = "Clear")
                }
            },
        )
    } else {
        OutlinedTextField(
            value = patientSearch,
            onValueChange = { viewModel.searchPatients(it) },
            modifier = Modifier.fillMaxWidth(),
            label = { Text("Search patient by name or MRN") },
            singleLine = true,
        )
        LazyColumn(modifier = Modifier.fillMaxWidth().heightIn(max = 200.dp)) {
            items(state.patientResults) { patient ->
                ListItem(
                    headlineContent = { Text("${patient.displayName} · ${patient.patientId}") },
                    modifier = Modifier.clickable { viewModel.selectPatient(patient) },
                )
            }
        }
    }
}

/** Visit scope: ongoing visits, one per patient. */
@Composable
private fun VisitScopePicker(
    viewModel: CreateRequestViewModel,
    state: CreateRequestState,
) {
    if (state.visits.isEmpty() && !state.isLoadingScope) {
        Text(
            "No patients with open visits",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
    LazyColumn(modifier = Modifier.fillMaxWidth().heightIn(max = 220.dp)) {
        items(state.visits, key = { it.id }) { visit ->
            ListItem(
                headlineContent = { Text(visit.patientName.ifBlank { "Patient ${visit.patientId}" }) },
                supportingContent = { Text(visit.typeDisplay) },
                trailingContent = {
                    if (state.selectedVisit?.id == visit.id) {
                        Icon(Icons.Default.Check, contentDescription = "Selected")
                    }
                },
                modifier = Modifier.clickable { viewModel.selectVisit(visit) },
            )
        }
    }
}

/** Encounter scope: active encounters, one per patient. */
@Composable
private fun EncounterScopePicker(
    viewModel: CreateRequestViewModel,
    state: CreateRequestState,
) {
    if (state.encounters.isEmpty() && !state.isLoadingScope) {
        Text(
            "No active encounters",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
    LazyColumn(modifier = Modifier.fillMaxWidth().heightIn(max = 220.dp)) {
        items(state.encounters, key = { it.id }) { encounter ->
            ListItem(
                headlineContent = {
                    Text("Patient ${encounter.patientId} · ${encounter.typeDisplay}")
                },
                supportingContent = { Text(encounter.encounterDatetime?.take(10) ?: "") },
                trailingContent = {
                    if (state.selectedEncounter?.id == encounter.id) {
                        Icon(Icons.Default.Check, contentDescription = "Selected")
                    }
                },
                modifier = Modifier.clickable { viewModel.selectEncounter(encounter) },
            )
        }
    }
}
