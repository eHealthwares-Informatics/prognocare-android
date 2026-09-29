package com.ehealthinformatics.prognocare.feature.dashboard.support

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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.SupportAgent
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material.icons.outlined.EventAvailable
import androidx.compose.material.icons.outlined.HowToReg
import androidx.compose.material.icons.outlined.Receipt
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FloatingActionButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ehealthinformatics.prognocare.designsystem.components.DashboardKpiCard
import com.ehealthinformatics.prognocare.designsystem.components.DashboardQuickAction
import com.ehealthinformatics.prognocare.designsystem.components.SectionHeader
import com.ehealthinformatics.prognocare.designsystem.components.StatusBadge
import com.ehealthinformatics.prognocare.designsystem.components.StatusType
import com.ehealthinformatics.prognocare.designsystem.theme.AppThemeColors
import com.ehealthinformatics.prognocare.designsystem.theme.Spacing

// ── Support Dashboard ────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SupportDashboardScreen(
    onNavigateToCheckIn: () -> Unit,
    onNavigateToRequests: () -> Unit,
    onNavigateToPatientLookup: () -> Unit,
    onNavigateToChat: () -> Unit,
    onNavigateToProfile: () -> Unit = {},
    viewModel: SupportDashboardViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    Scaffold(
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = onNavigateToCheckIn,
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
                shape = RoundedCornerShape(Spacing.lg),
                elevation = FloatingActionButtonDefaults.elevation(
                    defaultElevation = 6.dp,
                    pressedElevation = 8.dp,
                ),
            ) {
                Icon(Icons.Default.Add, contentDescription = null)
                Spacer(modifier = Modifier.width(Spacing.sm))
                Text("Check In Patient", fontWeight = FontWeight.SemiBold)
            }
        },
    ) { innerPadding ->
        PullToRefreshBox(
            isRefreshing = state.isLoading,
            onRefresh = { viewModel.retry() },
        ) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentPadding = PaddingValues(bottom = 100.dp),
            verticalArrangement = Arrangement.spacedBy(Spacing.base),
        ) {
            // ── Header ───────────────────────────────────────
            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = Spacing.lg)
                        .padding(top = Spacing.xl),
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "${state.greeting},",
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                            Text(
                                text = state.supportName,
                                style = MaterialTheme.typography.headlineMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary,
                            )
                            Spacer(modifier = Modifier.height(Spacing.xs))
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.SupportAgent,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(16.dp),
                                )
                                Spacer(modifier = Modifier.width(Spacing.xs))
                                Text(
                                    text = "Patient Services • ${state.todayDate}",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                        }

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box {
                                IconButton(onClick = { /* notifications */ }) {
                                    Icon(
                                        imageVector = Icons.Default.Notifications,
                                        contentDescription = "Notifications",
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.size(28.dp),
                                    )
                                }
                                if (state.patientsWaiting > 0) {
                                    Box(
                                        modifier = Modifier
                                            .align(Alignment.TopEnd)
                                            .size(20.dp)
                                            .clip(CircleShape)
                                            .background(AppThemeColors.current.notificationBadge),
                                        contentAlignment = Alignment.Center,
                                    ) {
                                        Text(
                                            text = "${state.patientsWaiting}",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = AppThemeColors.current.onNotificationBadge,
                                            fontWeight = FontWeight.Bold,
                                        )
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.width(Spacing.sm))

                            Box(
                                modifier = Modifier
                                    .size(48.dp)
                                    .clip(CircleShape)
                                    .background(MaterialTheme.colorScheme.primary)
                                    .clickable { onNavigateToProfile() },
                                contentAlignment = Alignment.Center,
                            ) {
                                Text(
                                    text = state.supportName.take(1),
                                    style = MaterialTheme.typography.titleLarge,
                                    color = MaterialTheme.colorScheme.onPrimary,
                                    fontWeight = FontWeight.Bold,
                                )
                            }
                        }
                    }
                }
            }

            // ── KPI Grid ─────────────────────────────────────
            item {
                Column(
                    modifier = Modifier.padding(horizontal = Spacing.lg),
                    verticalArrangement = Arrangement.spacedBy(Spacing.sm),
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
                    ) {
                        DashboardKpiCard(
                            title = "Waiting",
                            value = "${state.patientsWaiting}",
                            icon = Icons.Default.Timer,
                            iconTint = AppThemeColors.current.kpiOrange,
                            iconBg = AppThemeColors.current.kpiOrangeLight,
                            onClick = onNavigateToCheckIn,
                            modifier = Modifier.weight(1f),
                        )
                        DashboardKpiCard(
                            title = "Checked In",
                            value = "${state.checkedInToday}",
                            icon = Icons.Default.CheckCircle,
                            iconTint = AppThemeColors.current.kpiGreen,
                            iconBg = AppThemeColors.current.kpiGreenLight,
                            onClick = onNavigateToCheckIn,
                            modifier = Modifier.weight(1f),
                        )
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
                    ) {
                        DashboardKpiCard(
                            title = "Active Requests",
                            value = "${state.activeRequests}",
                            icon = Icons.Default.ErrorOutline,
                            iconTint = AppThemeColors.current.kpiPurple,
                            iconBg = AppThemeColors.current.kpiPurpleLight,
                            onClick = onNavigateToRequests,
                            modifier = Modifier.weight(1f),
                        )
                        DashboardKpiCard(
                            title = "Completed",
                            value = "${state.completedToday}",
                            icon = Icons.Default.TrendingUp,
                            iconTint = AppThemeColors.current.kpiBlue,
                            iconBg = AppThemeColors.current.kpiBlueLight,
                            onClick = { /* completed */ },
                            modifier = Modifier.weight(1f),
                        )
                    }
                }
            }

            // ── Quick Actions ─────────────────────────────────
            item {
                Column(modifier = Modifier.padding(horizontal = Spacing.lg)) {
                    SectionHeader(title = "Quick Actions")
                    Spacer(modifier = Modifier.height(Spacing.md))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
                    ) {
                        DashboardQuickAction(
                            icon = Icons.Outlined.HowToReg,
                            label = "Check In",
                            iconTint = AppThemeColors.current.kpiGreen,
                            iconBg = AppThemeColors.current.kpiGreenLight,
                            onClick = onNavigateToCheckIn,
                            modifier = Modifier.weight(1f),
                        )
                        DashboardQuickAction(
                            icon = Icons.Outlined.EventAvailable,
                            label = "Check Out",
                            iconTint = AppThemeColors.current.kpiBlue,
                            iconBg = AppThemeColors.current.kpiBlueLight,
                            onClick = onNavigateToCheckIn,
                            modifier = Modifier.weight(1f),
                        )
                        DashboardQuickAction(
                            icon = Icons.Outlined.Search,
                            label = "Patient Lookup",
                            iconTint = AppThemeColors.current.kpiPurple,
                            iconBg = AppThemeColors.current.kpiPurpleLight,
                            onClick = onNavigateToPatientLookup,
                            modifier = Modifier.weight(1f),
                        )
                        DashboardQuickAction(
                            icon = Icons.Outlined.Receipt,
                            label = "Requests",
                            iconTint = AppThemeColors.current.kpiOrange,
                            iconBg = AppThemeColors.current.kpiOrangeLight,
                            onClick = onNavigateToRequests,
                            modifier = Modifier.weight(1f),
                        )
                    }
                }
            }

            // ── Check-In Queue ────────────────────────────────
            item {
                Column(modifier = Modifier.padding(horizontal = Spacing.lg)) {
                    SectionHeader(
                        title = "Check-In Queue",
                        actionText = "View All",
                        onActionClick = onNavigateToCheckIn,
                    )
                    Spacer(modifier = Modifier.height(Spacing.md))
                }
            }

            items(state.checkInQueue.filter { it.status != CheckInStatus.CHECKED_OUT }) { checkIn ->
                SupportCheckInCard(
                    checkIn = checkIn,
                    onCheckIn = { viewModel.checkInPatient(checkIn.id) },
                    onCheckOut = { viewModel.checkOutPatient(checkIn.id) },
                    modifier = Modifier.padding(horizontal = Spacing.lg),
                )
            }

            // ── Recent Requests ───────────────────────────────
            item {
                Column(
                    modifier = Modifier.padding(horizontal = Spacing.lg),
                    verticalArrangement = Arrangement.spacedBy(Spacing.sm),
                ) {
                    SectionHeader(
                        title = "Recent Requests",
                        actionText = "View All",
                        onActionClick = onNavigateToRequests,
                    )
                    Spacer(modifier = Modifier.height(Spacing.xs))
                    com.ehealthinformatics.prognocare.designsystem.components.DemoDataChip(
                        text = "Not available — tickets module is not in the backend yet",
                    )
                    Spacer(modifier = Modifier.height(Spacing.sm))
                }
            }

            items(state.recentRequests) { request ->
                SupportRequestCard(
                    request = request,
                    onResolve = { viewModel.resolveRequest(request.id) },
                    modifier = Modifier.padding(horizontal = Spacing.lg),
                )
            }

            // Bottom spacer for FAB
            item { Spacer(modifier = Modifier.height(Spacing.lg)) }
        }
        }
    }
}

// ── Check-In Card ────────────────────────────────────────────

@Composable
private fun SupportCheckInCard(
    checkIn: SupportCheckIn,
    onCheckIn: () -> Unit,
    onCheckOut: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(Spacing.base),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface,
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
    ) {
        Column(
            modifier = Modifier.padding(Spacing.base),
        ) {
            // ── Top Row: Patient + Status ────────────────────
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
                            text = checkIn.patientName.take(1),
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                            fontWeight = FontWeight.Bold,
                        )
                    }
                    Spacer(modifier = Modifier.width(Spacing.md))
                    Column {
                        Text(
                            text = checkIn.patientName,
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurface,
                        )
                        Text(
                            text = checkIn.patientMrn,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
                SupportStatusBadge(status = checkIn.status)
            }

            Spacer(modifier = Modifier.height(Spacing.sm))

            // ── Details Row ──────────────────────────────────
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(Spacing.md),
            ) {
                DetailChip(
                    icon = Icons.Default.CalendarMonth,
                    text = checkIn.appointmentTime,
                )
                DetailChip(
                    icon = Icons.Default.Person,
                    text = checkIn.doctorName,
                )
            }

            if (checkIn.waitTime != null) {
                Spacer(modifier = Modifier.height(Spacing.xs))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Timer,
                        contentDescription = null,
                        tint = AppThemeColors.current.warning,
                        modifier = Modifier.size(14.dp),
                    )
                    Spacer(modifier = Modifier.width(Spacing.xs))
                    Text(
                        text = "Waiting ${checkIn.waitTime}",
                        style = MaterialTheme.typography.labelSmall,
                        color = AppThemeColors.current.warning,
                        fontWeight = FontWeight.Medium,
                    )
                }
            }

            Spacer(modifier = Modifier.height(Spacing.sm))

            // ── Appointment Type ─────────────────────────────
            Text(
                text = checkIn.appointmentType,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            Spacer(modifier = Modifier.height(Spacing.sm))

            // ── Action Buttons ───────────────────────────────
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
            ) {
                when (checkIn.status) {
                    CheckInStatus.WAITING -> {
                        OutlinedButton(
                            onClick = onCheckIn,
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(Spacing.sm),
                        ) {
                            Icon(
                                Icons.Outlined.HowToReg,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp),
                            )
                            Spacer(modifier = Modifier.width(Spacing.xs))
                            Text("Check In")
                        }
                    }
                    CheckInStatus.CHECKED_IN -> {
                        TextButton(
                            onClick = onCheckOut,
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(Spacing.sm),
                        ) {
                            Icon(
                                Icons.Default.CheckCircle,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp),
                                tint = MaterialTheme.colorScheme.primary,
                            )
                            Spacer(modifier = Modifier.width(Spacing.xs))
                            Text("Check Out")
                        }
                    }
                    CheckInStatus.IN_SESSION -> {
                        Text(
                            text = "In Session",
                            style = MaterialTheme.typography.bodySmall,
                            color = AppThemeColors.current.inProgress,
                            fontWeight = FontWeight.Medium,
                        )
                    }
                    else -> { /* No action for checked out */ }
                }
            }
        }
    }
}

// ── Request Card ─────────────────────────────────────────────

@Composable
private fun SupportRequestCard(
    request: SupportRequest,
    onResolve: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(Spacing.base),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface,
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
    ) {
        Column(
            modifier = Modifier.padding(Spacing.base),
        ) {
            // ── Header: Title + Status ──────────────────────
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

            // ── Description ──────────────────────────────────
            Text(
                text = request.description,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )

            Spacer(modifier = Modifier.height(Spacing.sm))

            // ── Footer: Category + Priority + Time ──────────
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

            // ── Assignee ────────────────────────────────────
            if (request.assignedTo != null) {
                Spacer(modifier = Modifier.height(Spacing.xs))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Person,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(12.dp),
                    )
                    Spacer(modifier = Modifier.width(Spacing.xxs))
                    Text(
                        text = "Assigned to ${request.assignedTo}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }

            // ── Action ──────────────────────────────────────
            if (request.status == RequestStatus.OPEN || request.status == RequestStatus.IN_PROGRESS) {
                Spacer(modifier = Modifier.height(Spacing.sm))
                TextButton(
                    onClick = onResolve,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Icon(
                        Icons.Default.CheckCircle,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                    )
                    Spacer(modifier = Modifier.width(Spacing.xs))
                    Text("Mark Resolved")
                }
            }
        }
    }
}

// ── Helper Badges ────────────────────────────────────────────

@Composable
private fun SupportStatusBadge(status: CheckInStatus) {
    val (text, type) = when (status) {
        CheckInStatus.WAITING -> "Waiting" to StatusType.Pending
        CheckInStatus.CHECKED_IN -> "Checked In" to StatusType.Active
        CheckInStatus.IN_SESSION -> "In Session" to StatusType.InProgress
        CheckInStatus.CHECKED_OUT -> "Completed" to StatusType.Completed
        CheckInStatus.NO_SHOW -> "No Show" to StatusType.Cancelled
    }
    StatusBadge(text = text, type = type)
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

@Composable
private fun DetailChip(icon: ImageVector, text: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(12.dp),
        )
        Spacer(modifier = Modifier.width(Spacing.xxs))
        Text(
            text = text,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}
