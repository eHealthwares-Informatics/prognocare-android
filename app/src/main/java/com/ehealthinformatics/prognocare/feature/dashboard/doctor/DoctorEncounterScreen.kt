package com.ehealthinformatics.prognocare.feature.dashboard.doctor

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.MedicalServices
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.outlined.Bloodtype
import androidx.compose.material.icons.outlined.Description
import androidx.compose.material.icons.outlined.MonitorHeart
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FloatingActionButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
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
import com.ehealthinformatics.prognocare.designsystem.components.StatusBadge
import com.ehealthinformatics.prognocare.designsystem.components.StatusType
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ehealthinformatics.prognocare.designsystem.theme.AppThemeColors
import com.ehealthinformatics.prognocare.designsystem.theme.Spacing

data class EncounterData(
    val patientName: String,
    val patientMrn: String,
    val encounterType: String,
    val chiefComplaint: String,
    val vitals: Map<String, String>,
    val notes: String,
    val diagnosis: String,
    val status: String,
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DoctorEncounterScreen(
    encounterId: String,
    onBack: () -> Unit,
    onAddNote: () -> Unit,
    onAddDiagnosis: () -> Unit,
    onComplete: () -> Unit,
    viewModel: DoctorEncounterViewModel = hiltViewModel(key = "encounter-$encounterId"),
) {
    var currentStep by remember { mutableStateOf(0) }
    val steps = listOf("Vitals", "Notes", "Diagnosis", "Prescription")

    val state by viewModel.state.collectAsStateWithLifecycle()
    androidx.compose.runtime.LaunchedEffect(encounterId) {
        viewModel.bind(encounterId)
    }

    val encounter = EncounterData(
        patientName = state.patient?.displayName ?: "Unknown patient",
        patientMrn = state.patient?.patientId ?: "—",
        encounterType = state.encounter?.typeDisplay ?: "Encounter",
        chiefComplaint = state.encounter?.reason ?: "No chief complaint recorded",
        vitals = emptyMap(),
        notes = state.encounter?.notes ?: "",
        diagnosis = state.encounter?.reason ?: "",
        status = state.encounter?.let { "IN_PROGRESS" } ?: "LOADING",
    )

    if (state.isLoading) {
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            androidx.compose.material3.CircularProgressIndicator()
        }
        return
    }
    if (state.encounter == null) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(Spacing.xxxl),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Text(
                text = state.error ?: "Encounter not found",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.error,
            )
            Spacer(modifier = Modifier.height(Spacing.md))
            androidx.compose.material3.TextButton(onClick = { viewModel.load() }) {
                Text("Retry")
            }
            androidx.compose.material3.TextButton(onClick = onBack) {
                Text("Go back")
            }
        }
        return
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Clinical Encounter") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    TextButton(onClick = onComplete) {
                        Icon(Icons.Default.Check, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        Spacer(modifier = Modifier.width(Spacing.xs))
                        Text("Complete", color = MaterialTheme.colorScheme.primary)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                ),
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = {
                    when (currentStep) {
                        1 -> onAddNote()
                        2 -> onAddDiagnosis()
                        3 -> { /* navigate to prescription */ }
                    }
                },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
                shape = RoundedCornerShape(Spacing.lg),
                elevation = FloatingActionButtonDefaults.elevation(defaultElevation = 6.dp),
            ) {
                Icon(Icons.Default.Add, contentDescription = null)
                Spacer(modifier = Modifier.width(Spacing.sm))
                Text(
                    when (currentStep) {
                        1 -> "Add Note"
                        2 -> "Add Diagnosis"
                        3 -> "Add Prescription"
                        else -> "Continue"
                    },
                    fontWeight = FontWeight.SemiBold,
                )
            }
        },
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            verticalArrangement = Arrangement.spacedBy(Spacing.base),
        ) {
            // ── Patient Header ───────────────────────────────
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = Spacing.lg),
                    shape = RoundedCornerShape(Spacing.base),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                ) {
                    Row(
                        modifier = Modifier.padding(Spacing.base),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Box(
                            modifier = Modifier
                                .size(52.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.primaryContainer),
                            contentAlignment = Alignment.Center,
                        ) {
                            Text(
                                text = encounter.patientName.take(1),
                                style = MaterialTheme.typography.titleLarge,
                                color = MaterialTheme.colorScheme.onPrimaryContainer,
                                fontWeight = FontWeight.Bold,
                            )
                        }
                        Spacer(modifier = Modifier.width(Spacing.md))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(encounter.patientName, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                            Text(encounter.patientMrn, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text(encounter.encounterType, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.primary)
                        }
                        StatusBadge(text = "Active", type = StatusType.InProgress)
                    }
                }
            }

            // ── Step Progress ────────────────────────────────
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = Spacing.lg),
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    steps.forEachIndexed { index, step ->
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.weight(1f),
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(32.dp)
                                    .clip(CircleShape)
                                    .background(
                                        if (index <= currentStep) MaterialTheme.colorScheme.primary
                                        else MaterialTheme.colorScheme.surfaceVariant
                                    ),
                                contentAlignment = Alignment.Center,
                            ) {
                                Text(
                                    text = "${index + 1}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = if (index <= currentStep) MaterialTheme.colorScheme.onPrimary
                                    else MaterialTheme.colorScheme.onSurfaceVariant,
                                    fontWeight = FontWeight.Bold,
                                )
                            }
                            Spacer(modifier = Modifier.height(Spacing.xs))
                            Text(
                                step,
                                style = MaterialTheme.typography.labelSmall,
                                color = if (index == currentStep) MaterialTheme.colorScheme.primary
                                else MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                }
            }

            // ── Chief Complaint ──────────────────────────────
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = Spacing.lg),
                    shape = RoundedCornerShape(Spacing.base),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                ) {
                    Column(modifier = Modifier.padding(Spacing.base)) {
                        Text("Chief Complaint", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.height(Spacing.xs))
                        Text(encounter.chiefComplaint, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }

            // ── Vitals ───────────────────────────────────────
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = Spacing.lg),
                    shape = RoundedCornerShape(Spacing.base),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                ) {
                    Column(modifier = Modifier.padding(Spacing.base)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text("Vitals", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                            IconButton(onClick = { /* edit vitals */ }, modifier = Modifier.size(32.dp)) {
                                Icon(Icons.Default.Edit, contentDescription = "Edit", modifier = Modifier.size(16.dp))
                            }
                        }
                        Spacer(modifier = Modifier.height(Spacing.sm))
                        encounter.vitals.forEach { (key, value) ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = Spacing.xs),
                                horizontalArrangement = Arrangement.SpaceBetween,
                            ) {
                                Text(key, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Text(value, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Medium)
                            }
                        }
                    }
                }
            }

            // ── Clinical Notes ───────────────────────────────
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = Spacing.lg),
                    shape = RoundedCornerShape(Spacing.base),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                ) {
                    Column(modifier = Modifier.padding(Spacing.base)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text("Clinical Notes", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                            IconButton(onClick = onAddNote, modifier = Modifier.size(32.dp)) {
                                Icon(Icons.Default.Add, contentDescription = "Add Note", modifier = Modifier.size(16.dp))
                            }
                        }
                        Spacer(modifier = Modifier.height(Spacing.xs))
                        if (encounter.notes.isNotEmpty()) {
                            Text(encounter.notes, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        } else {
                            Text("No notes added yet", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
            }

            // ── Diagnosis ────────────────────────────────────
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = Spacing.lg),
                    shape = RoundedCornerShape(Spacing.base),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                ) {
                    Column(modifier = Modifier.padding(Spacing.base)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text("Diagnosis", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                            IconButton(onClick = onAddDiagnosis, modifier = Modifier.size(32.dp)) {
                                Icon(Icons.Default.Add, contentDescription = "Add Diagnosis", modifier = Modifier.size(16.dp))
                            }
                        }
                        Spacer(modifier = Modifier.height(Spacing.xs))
                        if (encounter.diagnosis.isNotEmpty()) {
                            Text(encounter.diagnosis, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
                        } else {
                            Text("No diagnosis added yet", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
            }

            // ── Bottom Spacer ────────────────────────────────
            item { Spacer(modifier = Modifier.height(100.dp)) }
        }
    }
}
