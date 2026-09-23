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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.launch

private val REQUEST_TYPES = listOf("LAB", "RADIOLOGY", "OTHER_TEST", "PRESCRIPTION")
private val PRIORITIES = listOf("ROUTINE", "URGENT", "EMERGENCY")

@OptIn(ExperimentalMaterial3Api::class, FlowPreview::class)
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

    // Pre-select a patient passed from another screen.
    LaunchedEffect(patientId) {
        if (patientId != null && state.selectedPatient == null) {
            viewModel.update { it.copy(selectedPatient = null) }
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
            // ── Patient ──────────────────────────────────────
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(Spacing.base),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            ) {
                Column(modifier = Modifier.padding(Spacing.base)) {
                    Text(
                        text = "Patient",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold,
                    )
                    Spacer(modifier = Modifier.height(Spacing.sm))
                    if (state.selectedPatient != null) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text(
                                text = "${state.selectedPatient!!.displayName} (${state.selectedPatient!!.patientId})",
                                style = MaterialTheme.typography.bodyMedium,
                            )
                            IconButton(onClick = { viewModel.update { it.copy(selectedPatient = null) } }) {
                                Icon(Icons.Default.Close, contentDescription = "Clear")
                            }
                        }
                    } else {
                        OutlinedTextField(
                            value = patientSearch,
                            onValueChange = { patientSearch = it },
                            modifier = Modifier.fillMaxWidth(),
                            label = { Text("Search patient by name or MRN") },
                            singleLine = true,
                        )
                        state.patientResults.forEach { patient ->
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = Spacing.xs),
                            ) {
                                OutlinedButton(
                                    onClick = {
                                        viewModel.selectPatient(patient)
                                        patientSearch = ""
                                    },
                                ) {
                                    Text("${patient.displayName} · ${patient.patientId}")
                                }
                            }
                        }
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
                    Row(horizontalArrangement = Arrangement.spacedBy(Spacing.sm)) {
                        REQUEST_TYPES.forEach { type ->
                            FilterChip(
                                selected = state.requestType == type,
                                onClick = { viewModel.update { it.copy(requestType = type) } },
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
                    Row(horizontalArrangement = Arrangement.spacedBy(Spacing.sm)) {
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
                Text(if (state.isSaving) "Saving…" else "Create request")
            }
        }
    }
}
