package com.ehealthinformatics.prognocare.feature.clinical

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.AlertDialog
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
import androidx.compose.material3.TextButton
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
import com.ehealthinformatics.prognocare.data.remote.models.Admission
import com.ehealthinformatics.prognocare.data.remote.models.FormSubmission
import com.ehealthinformatics.prognocare.designsystem.components.EmptyState
import com.ehealthinformatics.prognocare.designsystem.components.ErrorState
import com.ehealthinformatics.prognocare.designsystem.components.StatusBadge
import com.ehealthinformatics.prognocare.designsystem.components.StatusType
import com.ehealthinformatics.prognocare.designsystem.theme.Spacing

private fun admissionStatusType(status: String): StatusType = when (status.uppercase()) {
    "ADMITTED" -> StatusType.InProgress
    "DISCHARGED" -> StatusType.Completed
    "TRANSFERRED" -> StatusType.Scheduled
    else -> StatusType.Pending
}

/**
 * Admission detail: admission info + visit-scoped documents + discharge.
 * Documents are limited to the admission's linked visit.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdmissionDetailScreen(
    admissionId: String,
    onBack: () -> Unit,
    viewModel: AdmissionDetailViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    var showDischargeDialog by remember { mutableStateOf(false) }
    var dischargeSummary by remember { mutableStateOf("") }

    LaunchedEffect(admissionId) { viewModel.bind(admissionId) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Admission", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = { viewModel.retry() }) {
                        Icon(Icons.Default.Refresh, contentDescription = "Refresh")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                ),
            )
        },
    ) { innerPadding ->
        when {
            state.isLoading && state.admission == null -> {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) { CircularProgressIndicator() }
            }
            state.error != null && state.admission == null -> {
                ErrorState(
                    message = state.error ?: "Failed to load",
                    onRetry = { viewModel.retry() },
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding),
                )
            }
            else -> {
                val admission = state.admission!!
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding),
                    contentPadding = PaddingValues(bottom = 100.dp),
                    verticalArrangement = Arrangement.spacedBy(Spacing.md),
                ) {
                    item {
                        AdmissionHeader(admission = admission)
                    }
                    item {
                        Column(modifier = Modifier.padding(horizontal = Spacing.lg)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Text(
                                    text = "Documents (${state.submissions.size})",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.SemiBold,
                                )
                                if (admission.isActive) {
                                    TextButton(
                                        onClick = { showDischargeDialog = true },
                                        enabled = !state.isBusy,
                                    ) {
                                        Text("Discharge", color = MaterialTheme.colorScheme.error)
                                    }
                                }
                            }
                        }
                    }
                    if (state.submissions.isEmpty()) {
                        item {
                            EmptyState(
                                title = "No documents",
                                message = "Documentation for this admission's visit will appear here.",
                            )
                        }
                    } else {
                        items(state.submissions, key = { it.id }) { submission ->
                            DocumentCard(submission = submission)
                        }
                    }
                }
            }
        }
    }

    if (showDischargeDialog) {
        AlertDialog(
            onDismissRequest = { showDischargeDialog = false },
            title = { Text("Discharge patient") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(Spacing.sm)) {
                    Text("Discharge ${state.admission?.patientName ?: "this patient"}?")
                    OutlinedTextField(
                        value = dischargeSummary,
                        onValueChange = { dischargeSummary = it },
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text("Discharge summary (optional)") },
                        minLines = 2,
                    )
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        showDischargeDialog = false
                        viewModel.discharge(
                            dischargeType = "DISCHARGED_HOME",
                            summary = dischargeSummary.takeIf { it.isNotBlank() },
                        )
                        dischargeSummary = ""
                    },
                ) {
                    Text("Discharge", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDischargeDialog = false }) {
                    Text("Cancel")
                }
            },
        )
    }
}

@Composable
private fun AdmissionHeader(admission: Admission) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(Spacing.lg),
        shape = RoundedCornerShape(Spacing.base),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
    ) {
        Column(modifier = Modifier.padding(Spacing.base)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = admission.patientName.ifBlank { "Patient ${admission.patientId}" },
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                    )
                    Text(
                        text = listOfNotNull(
                            admission.admissionNumber,
                            admission.typeDisplay,
                        ).joinToString(" · "),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                StatusBadge(
                    text = admission.statusDisplay,
                    type = admissionStatusType(admission.status),
                )
            }

            Spacer(modifier = Modifier.padding(top = Spacing.xs))
            HorizontalDivider()

            AdmissionDetailRow("MRN", admission.patientId)
            AdmissionDetailRow("Type", admission.typeDisplay)
            AdmissionDetailRow("Diagnosis", admission.diagnosis ?: "—")
            AdmissionDetailRow("Admitted", admission.admissionDatetime?.take(16)?.replace('T', ' ') ?: "—")
            AdmissionDetailRow("Referring provider", admission.referringProviderName ?: "—")
            if (admission.dischargeDatetime != null) {
                AdmissionDetailRow("Discharged", admission.dischargeDatetime?.take(16)?.replace('T', ' ') ?: "—")
                AdmissionDetailRow("Discharge type", admission.dischargeType ?: "—")
            }
        }
    }
}

@Composable
private fun AdmissionDetailRow(label: String, value: String) {
    Row(modifier = Modifier.padding(vertical = 2.dp)) {
        Text(
            text = "$label: ",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodySmall,
            fontWeight = FontWeight.Medium,
        )
    }
}

@Composable
private fun DocumentCard(submission: FormSubmission) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = Spacing.lg),
        shape = RoundedCornerShape(Spacing.base),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
    ) {
        Column(modifier = Modifier.padding(Spacing.base)) {
            Text(
                text = submission.formName,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
            )
            Text(
                text = listOfNotNull(
                    submission.submittedAt?.take(16)?.replace('T', ' '),
                    submission.status,
                ).joinToString(" · "),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}
