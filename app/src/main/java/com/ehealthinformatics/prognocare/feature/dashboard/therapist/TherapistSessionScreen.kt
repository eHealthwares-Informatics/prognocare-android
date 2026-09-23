package com.ehealthinformatics.prognocare.feature.dashboard.therapist

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
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ehealthinformatics.prognocare.designsystem.components.EmptyState
import com.ehealthinformatics.prognocare.designsystem.components.StatusBadge
import com.ehealthinformatics.prognocare.designsystem.components.StatusType
import com.ehealthinformatics.prognocare.designsystem.theme.AppThemeColors
import com.ehealthinformatics.prognocare.designsystem.theme.Spacing

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TherapistSessionScreen(
    onBack: () -> Unit,
    onPatientClick: (String) -> Unit,
    viewModel: TherapistDashboardViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    var selectedFilter by remember { mutableStateOf("All") }

    val filterOptions = listOf("All", "Scheduled", "In Progress", "Completed")

    val filteredSessions = state.upcomingSessions.filter { session ->
        when (selectedFilter) {
            "All" -> true
            "Scheduled" -> session.status == SessionStatus.SCHEDULED
            "In Progress" -> session.status == SessionStatus.IN_PROGRESS
            "Completed" -> session.status == SessionStatus.COMPLETED
            else -> true
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Sessions") },
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
                onClick = { /* new session */ },
                containerColor = MaterialTheme.colorScheme.primary,
            ) {
                Icon(Icons.Default.Add, contentDescription = "New Session")
            }
        },
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
        ) {
            // ── Filter Chips ────────────────────────────────
            androidx.compose.foundation.lazy.LazyRow(
                modifier = Modifier.padding(horizontal = Spacing.lg),
                horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
            ) {
                items(filterOptions.size) { index ->
                    FilterChip(
                        selected = selectedFilter == filterOptions[index],
                        onClick = { selectedFilter = filterOptions[index] },
                        label = {
                            Text(
                                text = filterOptions[index],
                                style = MaterialTheme.typography.labelMedium,
                            )
                        },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                            selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer,
                        ),
                    )
                }
            }

            Spacer(modifier = Modifier.height(Spacing.sm))

            // ── Sessions List ────────────────────────────────
            if (filteredSessions.isEmpty()) {
                EmptyState(
                    icon = Icons.Default.Add,
                    title = "No sessions found",
                    message = when (selectedFilter) {
                        "Scheduled" -> "No scheduled sessions at the moment"
                        "In Progress" -> "No sessions currently in progress"
                        else -> "No sessions match the current filter"
                    },
                )
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(Spacing.sm),
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(
                        horizontal = Spacing.lg,
                        vertical = Spacing.base,
                    ),
                ) {
                    items(filteredSessions) { session ->
                        SessionDetailCard(
                            session = session,
                            onClick = { onPatientClick(session.patientMrn) },
                            onComplete = { { /* session completion not backend-backed yet */ } },
                        )
                    }
                    item { Spacer(modifier = Modifier.height(Spacing.xxxl)) }
                }
            }
        }
    }
}

@Composable
private fun SessionDetailCard(
    session: TherapySession,
    onClick: () -> Unit,
    onComplete: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val (statusType, statusText) = when (session.status) {
        SessionStatus.SCHEDULED -> StatusType.Scheduled to "Scheduled"
        SessionStatus.IN_PROGRESS -> StatusType.InProgress to "In Progress"
        SessionStatus.COMPLETED -> StatusType.Completed to "Completed"
        SessionStatus.CANCELLED -> StatusType.Cancelled to "Cancelled"
        SessionStatus.NO_SHOW -> StatusType.Urgent to "No Show"
    }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
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
            // Time
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.width(56.dp),
            ) {
                Text(
                    text = session.scheduledTime.split(" ")[0],
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                )
                Text(
                    text = session.scheduledTime.split(" ").getOrElse(1) { "" },
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            Spacer(modifier = Modifier.width(Spacing.md))

            // Avatar
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(AppThemeColors.current.kpiBlueLight),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = session.patientName.take(1),
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold,
                )
            }

            Spacer(modifier = Modifier.width(Spacing.md))

            // Session info
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = session.patientName,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold,
                    )
                    if (session.isUrgent) {
                        Spacer(modifier = Modifier.width(Spacing.xs))
                        StatusBadge(text = "Urgent", type = StatusType.Urgent)
                    }
                }
                Text(
                    text = "${session.sessionType.name.replace("_", " ").lowercase().replaceFirstChar { it.uppercase() }} · ${session.duration}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(
                    text = session.location,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                if (session.notes != null) {
                    Text(
                        text = session.notes,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                    )
                }
            }

            // Status
            Column(horizontalAlignment = Alignment.End) {
                StatusBadge(text = statusText, type = statusType)
                if (session.status == SessionStatus.SCHEDULED) {
                    Spacer(modifier = Modifier.height(Spacing.xs))
                    Text(
                        text = "Start",
                        style = MaterialTheme.typography.labelMedium,
                        color = AppThemeColors.current.kpiGreen,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.clickable { /* start session */ },
                    )
                }
            }
        }
    }
}
