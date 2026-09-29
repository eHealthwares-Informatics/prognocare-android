package com.ehealthinformatics.prognocare.feature.dashboard.nurse

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.MedicalServices
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ehealthinformatics.prognocare.data.remote.models.Medication
import com.ehealthinformatics.prognocare.designsystem.components.EmptyState
import com.ehealthinformatics.prognocare.designsystem.components.ErrorState
import com.ehealthinformatics.prognocare.designsystem.theme.Tertiary
import com.ehealthinformatics.prognocare.designsystem.theme.Spacing
import android.widget.Toast

private val FILTERS = listOf("All", "Due", "Administered")

@Composable
private fun statusColorFor(status: String) = when (status) {
    "PRESCRIBED" -> MaterialTheme.colorScheme.error
    "PARTIALLY_ADMINISTERED" -> MaterialTheme.colorScheme.secondary
    "ADMINISTERED" -> Tertiary
    else -> MaterialTheme.colorScheme.onSurfaceVariant
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MedicationAdministrationScreen(
    onBack: () -> Unit,
    viewModel: MedicationsViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    var selectedFilter by remember { mutableStateOf("All") }
    var showAdminDialog by remember { mutableStateOf<Medication?>(null) }
    var showPrescriptions by remember { mutableStateOf(false) }

    val snackbarHostState = remember { SnackbarHostState() }
    val context = LocalContext.current

    LaunchedEffect(Unit) {
        viewModel.events.collect { event ->
            when (event) {
                is MedicationUiEvent.Success -> {
                    snackbarHostState.showSnackbar(event.message)
                    showPrescriptions = false
                }
                is MedicationUiEvent.Error -> Toast.makeText(context, event.message, Toast.LENGTH_LONG).show()
            }
        }
    }

    val filteredMeds = when (selectedFilter) {
        "Due" -> state.medications.filter { it.isDue }
        "Administered" -> state.medications.filter { it.administeredAt != null }
        else -> state.medications
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        floatingActionButton = {
            androidx.compose.material3.ExtendedFloatingActionButton(
                onClick = { showPrescriptions = true },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
            ) {
                Icon(Icons.Default.Add, contentDescription = null)
                Spacer(modifier = Modifier.width(Spacing.sm))
                Text("From prescription", fontWeight = FontWeight.SemiBold)
            }
        },
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Medications",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.SemiBold,
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface),
            )
        },
    ) { innerPadding ->
        PullToRefreshBox(
            isRefreshing = state.isLoading,
            onRefresh = { viewModel.refresh() },
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                // Filter chips
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = Spacing.lg, vertical = Spacing.sm),
                    horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
                ) {
                    FILTERS.forEach { filter ->
                        FilterChip(
                            selected = selectedFilter == filter,
                            onClick = { selectedFilter = filter },
                            label = { Text(filter) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                            ),
                        )
                    }
                }

                when {
                    state.error != null && state.medications.isEmpty() -> {
                        ErrorState(
                            message = state.error ?: "Failed to load",
                            onRetry = { viewModel.refresh() },
                        )
                    }
                    !state.isLoading && filteredMeds.isEmpty() -> {
                        EmptyState(
                            title = "No medications",
                            message = "Create medications from open prescription requests with the + button.",
                        )
                    }
                    else -> {
                        LazyColumn(
                            contentPadding = PaddingValues(horizontal = Spacing.lg),
                            verticalArrangement = Arrangement.spacedBy(Spacing.sm),
                        ) {
                            items(filteredMeds, key = { it.id }) { med ->
                                MedicationCard(
                                    medication = med,
                                    onAdminister = { showAdminDialog = med },
                                )
                            }
                            item { Spacer(modifier = Modifier.height(Spacing.xxxl)) }
                        }
                    }
                }
            }
        }
    }

    // Create-from-prescription dialog
    if (showPrescriptions) {
        AlertDialog(
            onDismissRequest = { showPrescriptions = false },
            title = { Text("Create from prescription") },
            text = {
                if (state.prescriptions.isEmpty()) {
                    Text("No open prescription items. All prescription requests have been converted or completed.")
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxWidth().height(320.dp),
                        verticalArrangement = Arrangement.spacedBy(Spacing.xs),
                    ) {
                        items(state.prescriptions, key = { "${it.requestId}#${it.itemIndex}" }) { item ->
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { viewModel.createFromPrescription(item) },
                                shape = RoundedCornerShape(Spacing.sm),
                                colors = CardDefaults.cardColors(
                                    containerColor = MaterialTheme.colorScheme.surfaceVariant,
                                ),
                            ) {
                                Column(modifier = Modifier.padding(Spacing.sm)) {
                                    Text(
                                        text = "${item.name} · ${item.dose}",
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.SemiBold,
                                    )
                                    Text(
                                        text = "${item.patientName} · ${item.route} · ${item.frequency}",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    )
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showPrescriptions = false }) { Text("Close") }
            },
        )
    }

    // Admin confirmation dialog
    showAdminDialog?.let { med ->
        var notes by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { showAdminDialog = null },
            title = { Text("Confirm Administration") },
            text = {
                Column {
                    Text("Mark ${med.name} ${med.doseDisplay} as administered to ${med.patientName ?: "the patient"}?")
                    Spacer(modifier = Modifier.height(Spacing.sm))
                    Text(
                        text = "Route: ${med.route ?: "—"} · Frequency: ${med.frequency ?: "—"}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Spacer(modifier = Modifier.height(Spacing.sm))
                    OutlinedTextField(
                        value = notes,
                        onValueChange = { notes = it },
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text("Administration notes (optional)") },
                        minLines = 2,
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    showAdminDialog = null
                    viewModel.administer(med.id, notes = notes)
                }) {
                    Text("Confirm", color = Tertiary)
                }
            },
            dismissButton = {
                TextButton(onClick = { showAdminDialog = null }) {
                    Text("Cancel")
                }
            },
        )
    }
}

@Composable
private fun MedicationCard(
    medication: Medication,
    onAdminister: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val isDue = medication.isDue
    val statusColor = statusColorFor(medication.status)

    Card(
        modifier = modifier
            .fillMaxWidth()
            .then(if (isDue) Modifier.clickable(onClick = onAdminister) else Modifier),
        shape = RoundedCornerShape(Spacing.base),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(Spacing.base),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            // Medication icon
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(RoundedCornerShape(Spacing.sm))
                    .background(statusColor.copy(alpha = 0.1f)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = Icons.Default.MedicalServices,
                    contentDescription = null,
                    tint = statusColor,
                    modifier = Modifier.size(22.dp),
                )
            }

            Spacer(modifier = Modifier.width(Spacing.md))

            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = medication.name,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Medium,
                    )
                    Spacer(modifier = Modifier.width(Spacing.sm))
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(Spacing.xs))
                            .background(statusColor.copy(alpha = 0.1f))
                            .padding(horizontal = Spacing.sm, vertical = Spacing.xs),
                    ) {
                        Text(
                            text = medication.statusDisplay,
                            style = MaterialTheme.typography.labelSmall,
                            color = statusColor,
                            fontWeight = FontWeight.Bold,
                        )
                    }
                }
                Spacer(modifier = Modifier.height(Spacing.xs))
                Text(
                    text = "${medication.patientName ?: "—"} · ${medication.doseDisplay} · ${medication.route ?: "—"}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(
                    text = medication.administeredAt?.let { "Administered: ${it.take(10)}" }
                        ?: "Prescribed: ${medication.createdAt?.take(10) ?: "—"} · ${medication.frequency ?: "—"}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            if (isDue) {
                IconButton(onClick = onAdminister) {
                    Icon(
                        Icons.Default.Check,
                        contentDescription = "Administer",
                        tint = Tertiary,
                        modifier = Modifier.size(28.dp),
                    )
                }
            }
        }
    }
}
