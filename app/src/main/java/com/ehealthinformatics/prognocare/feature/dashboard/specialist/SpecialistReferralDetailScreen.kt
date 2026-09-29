package com.ehealthinformatics.prognocare.feature.dashboard.specialist

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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ehealthinformatics.prognocare.designsystem.components.StatusBadge
import com.ehealthinformatics.prognocare.designsystem.components.StatusType
import com.ehealthinformatics.prognocare.designsystem.theme.Spacing

private fun detailStatusType(status: String): StatusType = when (status) {
    "PENDING" -> StatusType.Pending
    "ACCEPTED" -> StatusType.Active
    "DECLINED" -> StatusType.Cancelled
    "COMPLETED" -> StatusType.Completed
    else -> StatusType.Pending
}

@Composable
private fun InfoRow(label: String, value: String) {
    Column {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(text = value, style = MaterialTheme.typography.bodyMedium)
    }
    Spacer(modifier = Modifier.height(Spacing.sm))
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SpecialistReferralDetailScreen(
    referralId: String,
    onBack: () -> Unit,
    viewModel: SpecialistDashboardViewModel = hiltViewModel(),
) {
    val referral by viewModel.referralDetail.collectAsStateWithLifecycle()
    val isLoading by viewModel.detailLoading.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    var decisionReason by remember { mutableStateOf("") }

    LaunchedEffect(referralId) {
        viewModel.loadReferral(referralId)
    }

    LaunchedEffect(Unit) {
        viewModel.events.collect { event ->
            if (event is ReferralUiEvent.Success) {
                snackbarHostState.showSnackbar(event.message)
                viewModel.loadReferral(referralId)
            }
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text("Referral Detail") },
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
            isLoading -> Box(
                modifier = Modifier.fillMaxSize().padding(innerPadding),
                contentAlignment = Alignment.Center,
            ) { CircularProgressIndicator() }
            referral == null -> Box(
                modifier = Modifier.fillMaxSize().padding(innerPadding),
                contentAlignment = Alignment.Center,
            ) { Text("Referral not found") }
            else -> {
                val r = referral!!
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding)
                        .verticalScroll(rememberScrollState())
                        .padding(vertical = Spacing.base),
                    verticalArrangement = Arrangement.spacedBy(Spacing.md),
                ) {
                    // ── Patient header ─────────────────────────
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = Spacing.lg),
                        shape = RoundedCornerShape(Spacing.base),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(Spacing.base),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(52.dp)
                                    .clip(CircleShape)
                                    .background(MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.1f)),
                                contentAlignment = Alignment.Center,
                            ) {
                                Text(
                                    text = (r.patientName ?: "P").take(1),
                                    style = MaterialTheme.typography.titleLarge,
                                    fontWeight = FontWeight.Bold,
                                )
                            }
                            Spacer(modifier = Modifier.width(Spacing.md))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    r.patientName ?: "Patient ${r.patientId}",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                )
                                Text(
                                    "${r.referralNumber ?: ""} · ${r.patientId}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                            StatusBadge(text = r.statusDisplay, type = detailStatusType(r.status))
                        }
                    }

                    // ── Referral info ──────────────────────────
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = Spacing.lg),
                        shape = RoundedCornerShape(Spacing.base),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    ) {
                        Column(modifier = Modifier.padding(Spacing.base)) {
                            InfoRow("Referred by", r.referringProviderName ?: "—")
                            InfoRow("Addressed to", r.specialistProviderName ?: "—")
                            InfoRow("Specialty", r.specialty ?: "General")
                            InfoRow("Priority", r.priorityDisplay)
                            InfoRow("Created", r.createdAt?.take(10) ?: "—")
                            InfoRow("Status", r.statusDisplay)
                            if (r.decisionAt != null) {
                                InfoRow("Decided", r.decisionAt!!.take(10) + (r.decisionReason?.let { " — $it" } ?: ""))
                            }
                            if (r.completedAt != null) {
                                InfoRow("Completed", r.completedAt!!.take(10))
                            }
                        }
                    }

                    // ── Reason ─────────────────────────────────
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = Spacing.lg),
                        shape = RoundedCornerShape(Spacing.base),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    ) {
                        Column(modifier = Modifier.padding(Spacing.base)) {
                            Text("Reason for Referral", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                            Spacer(modifier = Modifier.height(Spacing.xs))
                            Text(r.reason, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            if (!r.notes.isNullOrBlank()) {
                                Spacer(modifier = Modifier.height(Spacing.sm))
                                Text("Notes", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                                Spacer(modifier = Modifier.height(Spacing.xs))
                                Text(r.notes!!, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    }

                    // ── Actions ────────────────────────────────
                    if (r.status == "PENDING" || r.status == "ACCEPTED") {
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = Spacing.lg),
                            shape = RoundedCornerShape(Spacing.base),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        ) {
                            Column(modifier = Modifier.padding(Spacing.base)) {
                                if (r.status == "PENDING") {
                                    OutlinedTextField(
                                        value = decisionReason,
                                        onValueChange = { decisionReason = it },
                                        modifier = Modifier.fillMaxWidth(),
                                        label = { Text("Decision reason (optional)") },
                                        minLines = 2,
                                    )
                                    Spacer(modifier = Modifier.height(Spacing.sm))
                                    Row(horizontalArrangement = Arrangement.spacedBy(Spacing.sm)) {
                                        OutlinedButton(
                                            onClick = {
                                                viewModel.decide(r.id, "DECLINED", decisionReason)
                                                decisionReason = ""
                                            },
                                            modifier = Modifier.weight(1f),
                                        ) { Text("Decline") }
                                        Button(
                                            onClick = {
                                                viewModel.decide(r.id, "ACCEPTED", decisionReason)
                                                decisionReason = ""
                                            },
                                            modifier = Modifier.weight(1f),
                                        ) { Text("Accept") }
                                    }
                                } else {
                                    Button(
                                        onClick = { viewModel.complete(r.id) },
                                        modifier = Modifier.fillMaxWidth(),
                                    ) { Text("Mark Completed") }
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(Spacing.lg))
                }
            }
        }
    }
}
