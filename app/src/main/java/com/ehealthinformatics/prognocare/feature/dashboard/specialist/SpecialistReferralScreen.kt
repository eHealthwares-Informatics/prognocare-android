package com.ehealthinformatics.prognocare.feature.dashboard.specialist

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ehealthinformatics.prognocare.data.remote.models.Referral
import com.ehealthinformatics.prognocare.designsystem.components.EmptyState
import com.ehealthinformatics.prognocare.designsystem.components.ErrorState
import com.ehealthinformatics.prognocare.designsystem.components.StatusBadge
import com.ehealthinformatics.prognocare.designsystem.components.StatusType
import com.ehealthinformatics.prognocare.designsystem.theme.AppThemeColors
import com.ehealthinformatics.prognocare.designsystem.theme.Spacing
import android.widget.Toast

private val STATUS_FILTERS = listOf("All", "PENDING", "ACCEPTED", "DECLINED", "COMPLETED")

@Composable
private fun statusType(status: String): StatusType = when (status) {
    "PENDING" -> StatusType.Pending
    "ACCEPTED" -> StatusType.Active
    "DECLINED" -> StatusType.Cancelled
    "COMPLETED" -> StatusType.Completed
    else -> StatusType.Pending
}

private fun statusLabel(status: String): String =
    status.replace("_", " ").lowercase().replaceFirstChar { it.uppercase() }

@Composable
private fun priorityColor(priority: String): Color = when (priority) {
    "URGENT" -> MaterialTheme.colorScheme.error
    else -> MaterialTheme.colorScheme.primary
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SpecialistReferralScreen(
    onBack: () -> Unit,
    onPatientClick: (String) -> Unit,
    viewModel: SpecialistDashboardViewModel = hiltViewModel(),
) {
    val referrals by viewModel.referrals.collectAsStateWithLifecycle()
    val referralsLoading by viewModel.referralsLoading.collectAsStateWithLifecycle()
    val referralsError by viewModel.referralsError.collectAsStateWithLifecycle()
    val direction by viewModel.direction.collectAsStateWithLifecycle()
    val statusFilter by viewModel.statusFilter.collectAsStateWithLifecycle()
    val createState by viewModel.createState.collectAsStateWithLifecycle()

    val snackbarHostState = remember { SnackbarHostState() }
    val context = LocalContext.current
    var showCreate by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        viewModel.events.collect { event ->
            when (event) {
                is ReferralUiEvent.Success -> {
                    snackbarHostState.showSnackbar(event.message)
                    showCreate = false
                }
                is ReferralUiEvent.Error -> Toast.makeText(context, event.message, Toast.LENGTH_LONG).show()
            }
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text("Referrals") },
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
        floatingActionButton = {
            FloatingActionButton(
                onClick = {
                    viewModel.loadCreateScope()
                    showCreate = true
                },
                containerColor = MaterialTheme.colorScheme.primary,
            ) {
                Icon(Icons.Default.Add, contentDescription = "New Referral")
            }
        },
    ) { innerPadding ->
        PullToRefreshBox(
            isRefreshing = referralsLoading,
            onRefresh = { viewModel.refresh() },
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                // ── Direction tabs ──────────────────────────────
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = Spacing.lg, vertical = Spacing.xs),
                    horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
                ) {
                    ReferralDirection.entries.forEach { dir ->
                        FilterChip(
                            selected = direction == dir,
                            onClick = { viewModel.setDirection(dir) },
                            label = { Text(dir.label) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                                selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer,
                            ),
                        )
                    }
                }

                // ── Status filter chips ─────────────────────────
                androidx.compose.foundation.lazy.LazyRow(
                    modifier = Modifier.padding(horizontal = Spacing.lg),
                    horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
                ) {
                    items(STATUS_FILTERS.size) { index ->
                        val option = STATUS_FILTERS[index]
                        FilterChip(
                            selected = statusFilter == (option.takeIf { it != "All" }),
                            onClick = {
                                viewModel.setStatusFilter(option.takeIf { it != "All" })
                            },
                            label = {
                                Text(
                                    text = option.takeIf { it != "All" }
                                        ?.let { statusLabel(it) } ?: "All",
                                    style = MaterialTheme.typography.labelMedium,
                                )
                            },
                        )
                    }
                }

                Spacer(modifier = Modifier.height(Spacing.sm))

                when {
                    referralsError != null && referrals.isEmpty() -> {
                        ErrorState(
                            message = referralsError ?: "Failed to load",
                            onRetry = { viewModel.refresh() },
                        )
                    }
                    !referralsLoading && referrals.isEmpty() -> {
                        EmptyState(
                            icon = Icons.Default.Add,
                            title = if (direction == ReferralDirection.INCOMING) {
                                "No incoming referrals"
                            } else {
                                "No outgoing referrals"
                            },
                            message = "Referrals will appear here as they are created.",
                        )
                    }
                    else -> {
                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            verticalArrangement = Arrangement.spacedBy(Spacing.sm),
                            contentPadding = androidx.compose.foundation.layout.PaddingValues(
                                horizontal = Spacing.lg,
                                vertical = Spacing.base,
                            ),
                        ) {
                            items(referrals, key = { it.id }) { referral ->
                                ReferralCard(
                                    referral = referral,
                                    isIncoming = direction == ReferralDirection.INCOMING,
                                    onClick = { onPatientClick(referral.patientId) },
                                    onAccept = { viewModel.decide(referral.id, "ACCEPTED") },
                                    onDecline = { viewModel.decide(referral.id, "DECLINED") },
                                    onComplete = { viewModel.complete(referral.id) },
                                )
                            }
                            item { Spacer(modifier = Modifier.height(Spacing.xxxl)) }
                        }
                    }
                }
            }
        }
    }

    if (showCreate) {
        CreateReferralDialog(
            state = createState,
            viewModel = viewModel,
            onDismiss = { showCreate = false },
        )
    }
}

@Composable
private fun ReferralCard(
    referral: Referral,
    isIncoming: Boolean,
    onClick: () -> Unit,
    onAccept: () -> Unit,
    onDecline: () -> Unit,
    onComplete: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val partyLabel = if (isIncoming) {
        "From ${referral.referringProviderName ?: "—"}"
    } else {
        "To ${referral.specialistProviderName ?: "—"}"
    }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(Spacing.base),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(Spacing.base),
        ) {
            // ── Header ──────────────────────────────────────
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primaryContainer),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            text = (referral.patientName ?: "P").take(1),
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                            fontWeight = FontWeight.Bold,
                        )
                    }
                    Spacer(modifier = Modifier.width(Spacing.md))
                    Column {
                        Text(
                            text = referral.patientName ?: "Patient ${referral.patientId}",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.SemiBold,
                        )
                        Text(
                            text = "$partyLabel · ${referral.referralNumber ?: ""}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
                StatusBadge(text = statusLabel(referral.status), type = statusType(referral.status))
            }

            Spacer(modifier = Modifier.height(Spacing.md))

            // ── Specialty & priority ────────────────────────
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(
                    text = referral.specialty ?: "General",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium,
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .clip(CircleShape)
                            .background(priorityColor(referral.priority)),
                    )
                    Spacer(modifier = Modifier.width(Spacing.xs))
                    Text(
                        text = referral.priorityDisplay,
                        style = MaterialTheme.typography.bodyMedium,
                        color = priorityColor(referral.priority),
                        fontWeight = FontWeight.Medium,
                    )
                }
            }

            Spacer(modifier = Modifier.height(Spacing.sm))

            // ── Reason ──────────────────────────────────────
            Text(
                text = referral.reason,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 2,
            )

            referral.notes?.let { notes ->
                Spacer(modifier = Modifier.height(Spacing.xs))
                Text(
                    text = notes,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            Spacer(modifier = Modifier.height(Spacing.sm))

            Text(
                text = "Created: ${referral.createdAt?.take(10) ?: "—"}",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            // ── Actions ─────────────────────────────────────
            if (isIncoming && referral.status == "PENDING") {
                Spacer(modifier = Modifier.height(Spacing.md))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
                ) {
                    OutlinedButton(
                        onClick = onDecline,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(Spacing.sm),
                    ) {
                        Text("Decline")
                    }
                    Button(
                        onClick = onAccept,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(Spacing.sm),
                    ) {
                        Text("Accept Referral")
                    }
                }
            } else if (isIncoming && referral.status == "ACCEPTED") {
                Spacer(modifier = Modifier.height(Spacing.md))
                Button(
                    onClick = onComplete,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(Spacing.sm),
                ) {
                    Text("Mark Completed")
                }
            }
        }
    }
}

@Composable
private fun CreateReferralDialog(
    state: CreateReferralState,
    viewModel: SpecialistDashboardViewModel,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("New Referral") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(Spacing.sm)) {
                if (state.isLoadingScope) {
                    Box(
                        modifier = Modifier.fillMaxWidth().padding(Spacing.lg),
                        contentAlignment = Alignment.Center,
                    ) { CircularProgressIndicator() }
                } else {
                    Text(
                        "Encounter (patient)",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    if (state.selectedEncounter == null) {
                        LazyColumn(modifier = Modifier.fillMaxWidth().height(160.dp)) {
                            items(state.encounters, key = { it.id }) { encounter ->
                                Text(
                                    text = "Patient ${encounter.patientId} · ${encounter.typeDisplay}",
                                    style = MaterialTheme.typography.bodySmall,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable {
                                            viewModel.updateCreate {
                                                it.copy(selectedEncounter = encounter)
                                            }
                                        }
                                        .padding(vertical = Spacing.xs),
                                )
                            }
                        }
                    } else {
                        Text(
                            "Patient ${state.selectedEncounter!!.patientId} · ${state.selectedEncounter!!.typeDisplay}",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.SemiBold,
                        )
                    }
                    Text(
                        "Specialist",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    if (state.selectedSpecialist == null) {
                        LazyColumn(modifier = Modifier.fillMaxWidth().height(160.dp)) {
                            items(state.specialists, key = { it.id }) { specialist ->
                                Text(
                                    text = specialist.displayWithDepartment,
                                    style = MaterialTheme.typography.bodySmall,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable {
                                            viewModel.updateCreate {
                                                it.copy(selectedSpecialist = specialist)
                                            }
                                        }
                                        .padding(vertical = Spacing.xs),
                                )
                            }
                        }
                    } else {
                        Text(
                            state.selectedSpecialist!!.displayWithDepartment,
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.SemiBold,
                        )
                    }
                    OutlinedTextField(
                        value = state.reason,
                        onValueChange = { v -> viewModel.updateCreate { it.copy(reason = v) } },
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text("Reason (min 3 characters)") },
                        minLines = 2,
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(Spacing.sm)) {
                        listOf("ROUTINE", "URGENT").forEach { priority ->
                            FilterChip(
                                selected = state.priority == priority,
                                onClick = { viewModel.updateCreate { it.copy(priority = priority) } },
                                label = { Text(priority.lowercase().replaceFirstChar { c -> c.uppercase() }) },
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = { viewModel.createReferral() }, enabled = !state.isSaving) {
                if (state.isSaving) {
                    CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                } else {
                    Text("Send referral")
                }
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        },
    )
}
