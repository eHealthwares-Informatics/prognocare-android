package com.ehealthinformatics.prognocare.feature.clinical

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ehealthinformatics.prognocare.data.remote.models.Encounter
import com.ehealthinformatics.prognocare.data.remote.models.ClinicalRequest
import com.ehealthinformatics.prognocare.data.remote.models.FormSubmission
import com.ehealthinformatics.prognocare.data.remote.models.Visit
import com.ehealthinformatics.prognocare.designsystem.components.EmptyState
import com.ehealthinformatics.prognocare.designsystem.components.ErrorState
import com.ehealthinformatics.prognocare.designsystem.components.StatusBadge
import com.ehealthinformatics.prognocare.designsystem.components.StatusType
import com.ehealthinformatics.prognocare.designsystem.theme.Spacing

private fun visitStatusType(status: String): StatusType = when (status.uppercase()) {
    "ONGOING" -> StatusType.InProgress
    "COMPLETED" -> StatusType.Completed
    "CANCELLED" -> StatusType.Cancelled
    else -> StatusType.Pending
}

/**
 * Visit detail: visit info + visit-scoped encounters / requests / documents.
 * Admit button converts the ongoing visit into an inpatient admission.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VisitDetailScreen(
    visitId: String,
    onBack: () -> Unit,
    viewModel: VisitDetailViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    var selectedTab by remember { mutableIntStateOf(0) }

    LaunchedEffect(visitId) { viewModel.bind(visitId) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Visit", fontWeight = FontWeight.Bold) },
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
        floatingActionButton = {
            if (state.visit?.isOngoing == true && state.admission == null) {
                ExtendedFloatingActionButton(
                    onClick = { viewModel.admitFromVisit() },
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary,
                    shape = RoundedCornerShape(Spacing.lg),
                ) {
                    Text("Admit patient", fontWeight = FontWeight.SemiBold)
                }
            }
        },
    ) { innerPadding ->
        when {
            state.isLoading && state.visit == null -> {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) { CircularProgressIndicator() }
            }
            state.error != null && state.visit == null -> {
                ErrorState(
                    message = state.error ?: "Failed to load",
                    onRetry = { viewModel.retry() },
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding),
                )
            }
            else -> {
                val visit = state.visit!!
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding),
                ) {
                    VisitHeader(
                        visit = visit,
                        admissionNumber = state.admission?.admissionNumber,
                        isBusy = state.isBusy,
                        onEnd = { viewModel.endVisit() },
                        onCancel = { viewModel.cancelVisit() },
                    )

                    TabRow(selectedTabIndex = selectedTab) {
                        listOf("Encounters", "Requests", "Documents").forEachIndexed { i, title ->
                            Tab(
                                selected = selectedTab == i,
                                onClick = { selectedTab = i },
                                text = { Text(title) },
                            )
                        }
                    }

                    when (selectedTab) {
                        0 -> VisitEncountersList(state.encounters)
                        1 -> VisitRequestsList(state.requests)
                        2 -> VisitDocumentsList(state.submissions)
                    }
                }
            }
        }
    }
}

@Composable
private fun VisitHeader(
    visit: Visit,
    admissionNumber: String?,
    isBusy: Boolean,
    onEnd: () -> Unit,
    onCancel: () -> Unit,
) {
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
                        text = visit.patientName.ifBlank { "Patient ${visit.patientId}" },
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                    )
                    Text(
                        text = listOfNotNull(
                            visit.visitNumber,
                            visit.typeDisplay,
                        ).joinToString(" · "),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                StatusBadge(
                    text = visit.status.lowercase().replaceFirstChar { it.uppercase() },
                    type = visitStatusType(visit.status),
                )
            }

            Spacer(modifier = Modifier.padding(top = Spacing.xs))
            HorizontalDivider()

            DetailRow("MRN", visit.patientId)
            DetailRow("Type", visit.typeDisplay)
            DetailRow("Provider", visit.providerName ?: "—")
            DetailRow("Started", visit.startDatetime?.take(16)?.replace('T', ' ') ?: "—")
            if (visit.stopDatetime != null) {
                DetailRow("Ended", visit.stopDatetime?.take(16)?.replace('T', ' ') ?: "—")
            }
            if (admissionNumber != null) {
                DetailRow("Admission", admissionNumber)
            }

            if (visit.isOngoing) {
                Spacer(modifier = Modifier.padding(top = Spacing.xs))
                Row(horizontalArrangement = Arrangement.spacedBy(Spacing.sm)) {
                    TextButton(onClick = onEnd, enabled = !isBusy) { Text("End visit") }
                    TextButton(onClick = onCancel, enabled = !isBusy) {
                        Text("Cancel", color = MaterialTheme.colorScheme.error)
                    }
                }
            }
        }
    }
}

@Composable
private fun DetailRow(label: String, value: String) {
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
private fun VisitEncountersList(encounters: List<Encounter>) {
    if (encounters.isEmpty()) {
        EmptyState(title = "No encounters", message = "Encounters for this visit will appear here.")
        return
    }
    LazyColumn(
        contentPadding = PaddingValues(Spacing.lg),
        verticalArrangement = Arrangement.spacedBy(Spacing.sm),
    ) {
        items(encounters, key = { it.id }) { encounter ->
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(Spacing.base),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            ) {
                Column(modifier = Modifier.padding(Spacing.base)) {
                    Text(
                        text = encounter.typeDisplay,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold,
                    )
                    Text(
                        text = listOfNotNull(
                            encounter.encounterDatetime?.take(16)?.replace('T', ' '),
                            encounter.providerName,
                            encounter.reason,
                        ).joinToString(" · "),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
    }
}

@Composable
private fun VisitRequestsList(requests: List<ClinicalRequest>) {
    if (requests.isEmpty()) {
        EmptyState(title = "No requests", message = "Clinical requests for this visit will appear here.")
        return
    }
    LazyColumn(
        contentPadding = PaddingValues(Spacing.lg),
        verticalArrangement = Arrangement.spacedBy(Spacing.sm),
    ) {
        items(requests, key = { it.id }) { request ->
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(Spacing.base),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            ) {
                Column(modifier = Modifier.padding(Spacing.base)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        Text(
                            text = request.typeDisplay,
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.SemiBold,
                        )
                        StatusBadge(
                            text = request.statusDisplay,
                            type = when (request.status.uppercase()) {
                                "COMPLETED" -> StatusType.Completed
                                "CANCELLED", "REJECTED" -> StatusType.Cancelled
                                "IN_PROGRESS" -> StatusType.InProgress
                                else -> StatusType.Pending
                            },
                        )
                    }
                    Text(
                        text = listOfNotNull(
                            request.requestNumber,
                            request.diagnosis,
                        ).joinToString(" · "),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
    }
}

@Composable
private fun VisitDocumentsList(submissions: List<FormSubmission>) {
    if (submissions.isEmpty()) {
        EmptyState(title = "No documents", message = "Documentation for this visit will appear here.")
        return
    }
    LazyColumn(
        contentPadding = PaddingValues(Spacing.lg),
        verticalArrangement = Arrangement.spacedBy(Spacing.sm),
    ) {
        items(submissions, key = { it.id }) { submission ->
            Card(
                modifier = Modifier.fillMaxWidth(),
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
    }
}
