package com.ehealthwares.prognocare.feature.dashboard.support

import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ehealthwares.prognocare.designsystem.components.StatusBadge
import com.ehealthwares.prognocare.designsystem.components.StatusType
import com.ehealthwares.prognocare.designsystem.theme.AppThemeColors
import com.ehealthwares.prognocare.designsystem.theme.Spacing

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SupportRequestListScreen(
    onBack: () -> Unit,
    viewModel: SupportDashboardViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    var selectedFilter by remember { mutableStateOf("All") }
    val filters = listOf("All", "Open", "In Progress", "Urgent", "Resolved")

    val filteredRequests = state.recentRequests.filter { request ->
        when (selectedFilter) {
            "All" -> true
            "Open" -> request.status == RequestStatus.OPEN
            "In Progress" -> request.status == RequestStatus.IN_PROGRESS
            "Urgent" -> request.priority == RequestPriority.URGENT || request.priority == RequestPriority.HIGH
            "Resolved" -> request.status == RequestStatus.RESOLVED || request.status == RequestStatus.CLOSED
            else -> true
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Support Requests") },
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
                .padding(innerPadding),
        ) {
            // ── Filter Chips ─────────────────────────────────
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = Spacing.lg, vertical = Spacing.sm),
                horizontalArrangement = Arrangement.spacedBy(Spacing.xs),
            ) {
                filters.forEach { filter ->
                    FilterChip(
                        selected = selectedFilter == filter,
                        onClick = { selectedFilter = filter },
                        label = {
                            Text(
                                text = filter,
                                style = MaterialTheme.typography.labelSmall,
                            )
                        },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                            selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer,
                        ),
                    )
                }
            }

            // ── Requests List ────────────────────────────────
            LazyColumn(
                contentPadding = PaddingValues(horizontal = Spacing.lg, vertical = Spacing.sm),
                verticalArrangement = Arrangement.spacedBy(Spacing.sm),
            ) {
                items(filteredRequests) { request ->
                    SupportRequestListItem(
                        request = request,
                        onResolve = { viewModel.resolveRequest(request.id) },
                    )
                }
            }
        }
    }
}

@Composable
private fun SupportRequestListItem(
    request: SupportRequest,
    onResolve: () -> Unit,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(Spacing.base),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface,
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
    ) {
        Column(
            modifier = Modifier.padding(Spacing.base),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top,
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = request.title,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                    if (request.patientName != null) {
                        Text(
                            text = request.patientName,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
                SupportRequestStatusBadge(status = request.status)
            }

            Spacer(modifier = Modifier.height(Spacing.xs))

            Text(
                text = request.description,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )

            Spacer(modifier = Modifier.height(Spacing.sm))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(Spacing.xs)) {
                    SupportCategoryBadge(category = request.category)
                    SupportPriorityBadge(priority = request.priority)
                }
                Text(
                    text = request.createdAt,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            if (request.assignedTo != null) {
                Spacer(modifier = Modifier.height(Spacing.xs))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Person, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(12.dp))
                    Spacer(modifier = Modifier.width(Spacing.xxs))
                    Text("Assigned to ${request.assignedTo}", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }

            if (request.status == RequestStatus.OPEN || request.status == RequestStatus.IN_PROGRESS) {
                Spacer(modifier = Modifier.height(Spacing.sm))
                TextButton(
                    onClick = onResolve,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Icon(Icons.Default.CheckCircle, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(Spacing.xs))
                    Text("Mark Resolved")
                }
            }
        }
    }
}

@Composable
private fun SupportRequestStatusBadge(status: RequestStatus) {
    val (text, type) = when (status) {
        RequestStatus.OPEN -> "Open" to StatusType.Pending
        RequestStatus.IN_PROGRESS -> "In Progress" to StatusType.InProgress
        RequestStatus.RESOLVED -> "Resolved" to StatusType.Completed
        RequestStatus.ESCALATED -> "Escalated" to StatusType.Urgent
        RequestStatus.CLOSED -> "Closed" to StatusType.Cancelled
    }
    StatusBadge(text = text, type = type)
}

@Composable
private fun SupportCategoryBadge(category: RequestCategory) {
    val color = when (category) {
        RequestCategory.PREAUTHORIZATION -> AppThemeColors.current.kpiPurple
        RequestCategory.INSURANCE -> AppThemeColors.current.kpiBlue
        RequestCategory.RECORDS -> AppThemeColors.current.kpiGreen
        RequestCategory.APPOINTMENT -> AppThemeColors.current.kpiOrange
        RequestCategory.BILLING -> AppThemeColors.current.critical
        RequestCategory.COMPLAINT -> AppThemeColors.current.warning
        RequestCategory.OTHER -> MaterialTheme.colorScheme.onSurfaceVariant
    }
    Text(
        text = category.displayName,
        style = MaterialTheme.typography.labelSmall,
        color = color,
        fontWeight = FontWeight.Medium,
    )
}

@Composable
private fun SupportPriorityBadge(priority: RequestPriority) {
    val color = when (priority) {
        RequestPriority.URGENT -> AppThemeColors.current.critical
        RequestPriority.HIGH -> AppThemeColors.current.warning
        RequestPriority.NORMAL -> MaterialTheme.colorScheme.onSurfaceVariant
        RequestPriority.LOW -> MaterialTheme.colorScheme.outline
    }
    Text(
        text = "• ${priority.displayName}",
        style = MaterialTheme.typography.labelSmall,
        color = color,
        fontWeight = FontWeight.Medium,
    )
}
