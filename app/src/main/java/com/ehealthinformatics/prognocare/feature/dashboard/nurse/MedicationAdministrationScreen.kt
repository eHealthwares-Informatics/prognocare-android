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
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.MedicalServices
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ehealthinformatics.prognocare.designsystem.components.DemoDataChip
import com.ehealthinformatics.prognocare.designsystem.components.EmptyState
import com.ehealthinformatics.prognocare.designsystem.components.ErrorState
import com.ehealthinformatics.prognocare.designsystem.theme.Spacing
import com.ehealthinformatics.prognocare.designsystem.theme.Tertiary
import com.ehealthinformatics.prognocare.feature.requests.RequestsListViewModel

private data class MedicationItem(
    val id: String,
    val patientName: String,
    val medicationName: String,
    val dosage: String,
    val route: String,
    val frequency: String,
    val scheduledTime: String,
    val status: String,
    val notes: String = "",
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MedicationAdministrationScreen(
    onBack: () -> Unit,
    viewModel: RequestsListViewModel = hiltViewModel(key = "med-admin"),
) {
    var selectedFilter by remember { mutableStateOf("All") }
    var showAdminDialog by remember { mutableStateOf<MedicationItem?>(null) }
    val filters = listOf("All", "Due", "Administered")
    val state by viewModel.state.collectAsStateWithLifecycle()

    // Map open PRESCRIPTION requests into the medication list.
    val medications = state.requests.map { request ->
        val first = request.items.firstOrNull()
        MedicationItem(
            id = request.id,
            patientName = request.patientName,
            medicationName = first?.name ?: "Medication",
            dosage = listOfNotNull(first?.dose, first?.doseUnit).joinToString(" ").ifBlank { "—" },
            route = first?.route ?: "—",
            frequency = first?.frequency ?: "—",
            scheduledTime = request.requestedAt?.take(10) ?: "—",
            status = when (request.status) {
                "COMPLETED" -> "ADMINISTERED"
                "IN_PROGRESS" -> "IN_PROGRESS"
                else -> "DUE"
            },
        )
    }

    val filteredMeds = when (selectedFilter) {
        "Due" -> medications.filter { it.status == "DUE" }
        "Administered" -> medications.filter { it.status == "ADMINISTERED" }
        else -> medications
    }

    Scaffold(
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
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
        ) {
            DemoDataChip(text = "Live from prescription requests · schedules not available")

            // Filter chips
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = Spacing.lg, vertical = Spacing.sm),
                horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
            ) {
                filters.forEach { filter ->
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
                state.isLoading -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center,
                    ) { CircularProgressIndicator() }
                }
                state.error != null -> {
                    ErrorState(message = state.error ?: "Failed to load", onRetry = { viewModel.load("PRESCRIPTION") })
                }
                filteredMeds.isEmpty() -> {
                    EmptyState(
                        title = "No medications due",
                        message = "Open prescription requests will appear here.",
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
                    }
                }
            }
        }

        // Admin confirmation dialog
        showAdminDialog?.let { med ->
            AlertDialog(
                onDismissRequest = { showAdminDialog = null },
                title = { Text("Confirm Administration") },
                text = {
                    Column {
                        Text("Mark ${med.medicationName} ${med.dosage} as administered to ${med.patientName}?")
                        Spacer(modifier = Modifier.height(Spacing.sm))
                        Text(
                            text = "Route: ${med.route} · Frequency: ${med.frequency}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                },
                confirmButton = {
                    TextButton(onClick = {
                        showAdminDialog = null
                        viewModel.onMedicationAdministered(med.id)
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
}

@Composable
private fun MedicationCard(
    medication: MedicationItem,
    onAdminister: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val isDue = medication.status == "DUE"
    val statusColor = if (isDue) MaterialTheme.colorScheme.error else Tertiary

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
                        text = medication.medicationName,
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
                            text = medication.status,
                            style = MaterialTheme.typography.labelSmall,
                            color = statusColor,
                            fontWeight = FontWeight.Bold,
                        )
                    }
                }
                Spacer(modifier = Modifier.height(Spacing.xs))
                Text(
                    text = "${medication.patientName} · ${medication.dosage} · ${medication.route}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(
                    text = "Requested: ${medication.scheduledTime} · ${medication.frequency}",
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
