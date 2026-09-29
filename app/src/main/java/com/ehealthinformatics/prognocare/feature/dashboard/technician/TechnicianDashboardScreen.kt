package com.ehealthinformatics.prognocare.feature.dashboard.technician

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
import androidx.compose.material.icons.filled.Biotech
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.MedicalServices
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Science
import androidx.compose.material.icons.filled.Upload
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.outlined.Bloodtype
import androidx.compose.material.icons.outlined.CameraAlt
import androidx.compose.material.icons.outlined.EventNote
import androidx.compose.material.icons.outlined.UploadFile
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FloatingActionButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
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

// ── Technician Dashboard ─────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TechnicianDashboardScreen(
    onNavigateToOrders: () -> Unit,
    onNavigateToResults: () -> Unit,
    onNavigateToChat: () -> Unit,
    onNavigateToProfile: () -> Unit = {},
    viewModel: TechnicianDashboardViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    Scaffold(
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = { /* upload result */ },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
                shape = RoundedCornerShape(Spacing.lg),
                elevation = FloatingActionButtonDefaults.elevation(
                    defaultElevation = 6.dp,
                    pressedElevation = 8.dp,
                ),
            ) {
                Icon(Icons.Default.Upload, contentDescription = null)
                Spacer(modifier = Modifier.width(Spacing.sm))
                Text("Upload Result", fontWeight = FontWeight.SemiBold)
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
                                text = state.technicianName,
                                style = MaterialTheme.typography.headlineMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary,
                            )
                            Spacer(modifier = Modifier.height(Spacing.xs))
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.Science,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(16.dp),
                                )
                                Spacer(modifier = Modifier.width(Spacing.xs))
                                Text(
                                    text = "${state.department} • ${state.todayDate}",
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
                                if (state.urgentOrders > 0) {
                                    Box(
                                        modifier = Modifier
                                            .align(Alignment.TopEnd)
                                            .size(20.dp)
                                            .clip(CircleShape)
                                            .background(AppThemeColors.current.notificationBadge),
                                        contentAlignment = Alignment.Center,
                                    ) {
                                        Text(
                                            text = "${state.urgentOrders}",
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
                                    text = state.technicianName.take(1),
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
                            title = "Pending",
                            value = "${state.pendingOrders}",
                            icon = Icons.Default.ErrorOutline,
                            iconTint = AppThemeColors.current.kpiOrange,
                            iconBg = AppThemeColors.current.kpiOrangeLight,
                            onClick = onNavigateToOrders,
                            modifier = Modifier.weight(1f),
                        )
                        DashboardKpiCard(
                            title = "In Progress",
                            value = "${state.inProgress}",
                            icon = Icons.Default.Biotech,
                            iconTint = AppThemeColors.current.kpiBlue,
                            iconBg = AppThemeColors.current.kpiBlueLight,
                            onClick = onNavigateToOrders,
                            modifier = Modifier.weight(1f),
                        )
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
                    ) {
                        DashboardKpiCard(
                            title = "Completed",
                            value = "${state.completedToday}",
                            icon = Icons.Default.CheckCircle,
                            iconTint = AppThemeColors.current.kpiGreen,
                            iconBg = AppThemeColors.current.kpiGreenLight,
                            onClick = onNavigateToResults,
                            modifier = Modifier.weight(1f),
                        )
                        DashboardKpiCard(
                            title = "Urgent",
                            value = "${state.urgentOrders}",
                            icon = Icons.Default.Warning,
                            iconTint = AppThemeColors.current.kpiPurple,
                            iconBg = AppThemeColors.current.kpiPurpleLight,
                            onClick = onNavigateToOrders,
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
                            icon = Icons.Outlined.Bloodtype,
                            label = "Blood Work",
                            iconTint = AppThemeColors.current.kpiGreen,
                            iconBg = AppThemeColors.current.kpiGreenLight,
                            onClick = onNavigateToOrders,
                            modifier = Modifier.weight(1f),
                        )
                        DashboardQuickAction(
                            icon = Icons.Outlined.CameraAlt,
                            label = "Imaging",
                            iconTint = AppThemeColors.current.kpiBlue,
                            iconBg = AppThemeColors.current.kpiBlueLight,
                            onClick = onNavigateToOrders,
                            modifier = Modifier.weight(1f),
                        )
                        DashboardQuickAction(
                            icon = Icons.Outlined.UploadFile,
                            label = "Upload",
                            iconTint = AppThemeColors.current.kpiPurple,
                            iconBg = AppThemeColors.current.kpiPurpleLight,
                            onClick = { /* upload result */ },
                            modifier = Modifier.weight(1f),
                        )
                        DashboardQuickAction(
                            icon = Icons.Outlined.EventNote,
                            label = "Orders",
                            iconTint = AppThemeColors.current.kpiOrange,
                            iconBg = AppThemeColors.current.kpiOrangeLight,
                            onClick = onNavigateToOrders,
                            modifier = Modifier.weight(1f),
                        )
                    }
                }
            }

            // ── Pending Orders Queue ──────────────────────────
            item {
                Column(modifier = Modifier.padding(horizontal = Spacing.lg)) {
                    SectionHeader(
                        title = "Orders Queue",
                        actionText = "View All",
                        onActionClick = onNavigateToOrders,
                    )
                    Spacer(modifier = Modifier.height(Spacing.md))
                }
            }

            items(state.pendingOrdersList) { order ->
                TechnicianOrderCard(
                    order = order,
                    onStartProcessing = {
                        viewModel.updateOrderStatus(order.id, OrderStatus.IN_PROGRESS)
                    },
                    onMarkSampleCollected = {
                        viewModel.updateOrderStatus(order.id, OrderStatus.SAMPLE_COLLECTED)
                    },
                    onComplete = {
                        viewModel.updateOrderStatus(order.id, OrderStatus.COMPLETED)
                    },
                    modifier = Modifier.padding(horizontal = Spacing.lg),
                )
            }

            // ── Recent Results ────────────────────────────────
            item {
                Column(
                    modifier = Modifier.padding(horizontal = Spacing.lg),
                    verticalArrangement = Arrangement.spacedBy(Spacing.sm),
                ) {
                    SectionHeader(
                        title = "Recent Results",
                        actionText = "View All",
                        onActionClick = onNavigateToResults,
                    )
                    Spacer(modifier = Modifier.height(Spacing.sm))
                }
            }

            items(state.recentResults) { result ->
                TechnicianResultCard(
                    result = result,
                    modifier = Modifier.padding(horizontal = Spacing.lg),
                )
            }

            // Bottom spacer for FAB
            item { Spacer(modifier = Modifier.height(Spacing.lg)) }
        }
        }
    }
}

// ── Order Card ───────────────────────────────────────────────

@Composable
private fun TechnicianOrderCard(
    order: TechnicianOrder,
    onStartProcessing: () -> Unit,
    onMarkSampleCollected: () -> Unit,
    onComplete: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val isStat = order.priority == OrderPriority.STAT

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(Spacing.base),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface,
        ),
        elevation = CardDefaults.cardElevation(
            defaultElevation = if (isStat) 4.dp else 1.dp,
        ),
    ) {
        Column(
            modifier = Modifier.padding(Spacing.base),
        ) {
            // ── Header: Test + Priority + Status ─────────────
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top,
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = order.testName,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Text(
                        text = order.patientName,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Text(
                        text = order.patientMrn,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Column(horizontalAlignment = Alignment.End) {
                    TechnicianPriorityBadge(priority = order.priority)
                    Spacer(modifier = Modifier.height(Spacing.xs))
                    TechnicianOrderStatusBadge(status = order.status)
                }
            }

            Spacer(modifier = Modifier.height(Spacing.sm))

            // ── Details Row ──────────────────────────────────
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(Spacing.md),
            ) {
                DetailChip(
                    icon = Icons.Default.Person,
                    text = order.orderedBy,
                )
                DetailChip(
                    icon = Icons.Default.MedicalServices,
                    text = order.orderType.displayName,
                )
            }

            if (order.dueTime != null) {
                Spacer(modifier = Modifier.height(Spacing.xs))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.ErrorOutline,
                        contentDescription = null,
                        tint = if (isStat) AppThemeColors.current.critical else AppThemeColors.current.warning,
                        modifier = Modifier.size(14.dp),
                    )
                    Spacer(modifier = Modifier.width(Spacing.xs))
                    Text(
                        text = "Due: ${order.dueTime}",
                        style = MaterialTheme.typography.labelSmall,
                        color = if (isStat) AppThemeColors.current.critical else AppThemeColors.current.warning,
                        fontWeight = FontWeight.Medium,
                    )
                }
            }

            if (order.notes != null) {
                Spacer(modifier = Modifier.height(Spacing.xs))
                Text(
                    text = order.notes,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontStyle = androidx.compose.ui.text.font.FontStyle.Italic,
                )
            }

            // ── Progress bar for in-progress orders ──────────
            if (order.status == OrderStatus.IN_PROGRESS || order.status == OrderStatus.PROCESSING) {
                Spacer(modifier = Modifier.height(Spacing.sm))
                val progress = if (order.status == OrderStatus.PROCESSING) 0.7f else 0.3f
                LinearProgressIndicator(
                    progress = { progress },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(4.dp)
                        .clip(RoundedCornerShape(2.dp)),
                    color = MaterialTheme.colorScheme.primary,
                    trackColor = MaterialTheme.colorScheme.surfaceVariant,
                )
            }

            Spacer(modifier = Modifier.height(Spacing.sm))

            // ── Action Buttons ───────────────────────────────
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
            ) {
                when (order.status) {
                    OrderStatus.RECEIVED -> {
                        OutlinedButton(
                            onClick = onStartProcessing,
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(Spacing.sm),
                        ) {
                            Icon(
                                Icons.Default.Biotech,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp),
                            )
                            Spacer(modifier = Modifier.width(Spacing.xs))
                            Text("Start Processing")
                        }
                    }
                    OrderStatus.IN_PROGRESS -> {
                        OutlinedButton(
                            onClick = onMarkSampleCollected,
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(Spacing.sm),
                        ) {
                            Icon(
                                Icons.Outlined.Bloodtype,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp),
                            )
                            Spacer(modifier = Modifier.width(Spacing.xs))
                            Text("Sample Collected")
                        }
                    }
                    OrderStatus.SAMPLE_COLLECTED -> {
                        TextButton(
                            onClick = onComplete,
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
                            Text("Complete & Upload")
                        }
                    }
                    OrderStatus.PROCESSING -> {
                        TextButton(
                            onClick = onComplete,
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
                            Text("Complete")
                        }
                    }
                    else -> { /* No action for completed/cancelled */ }
                }
            }
        }
    }
}

// ── Result Card ──────────────────────────────────────────────

@Composable
private fun TechnicianResultCard(
    result: TechnicianResult,
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
            // ── Header ──────────────────────────────────────
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = result.testName,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Text(
                        text = result.patientName,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                if (result.isAbnormal) {
                    StatusBadge(text = "Abnormal", type = StatusType.Urgent)
                } else {
                    StatusBadge(text = "Normal", type = StatusType.Completed)
                }
            }

            Spacer(modifier = Modifier.height(Spacing.xs))

            // ── Result Summary ───────────────────────────────
            Text(
                text = result.resultSummary,
                style = MaterialTheme.typography.bodySmall,
                color = if (result.isAbnormal) AppThemeColors.current.critical else MaterialTheme.colorScheme.onSurfaceVariant,
                fontWeight = if (result.isAbnormal) FontWeight.Medium else FontWeight.Normal,
                maxLines = 3,
                overflow = TextOverflow.Ellipsis,
            )

            Spacer(modifier = Modifier.height(Spacing.sm))

            // ── Footer ───────────────────────────────────────
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = "Uploaded ${result.uploadedAt}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                if (result.reviewedBy != null) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Default.CheckCircle,
                            contentDescription = null,
                            tint = AppThemeColors.current.success,
                            modifier = Modifier.size(12.dp),
                        )
                        Spacer(modifier = Modifier.width(Spacing.xxs))
                        Text(
                            text = result.reviewedBy,
                            style = MaterialTheme.typography.labelSmall,
                            color = AppThemeColors.current.success,
                        )
                    }
                } else {
                    Text(
                        text = "Awaiting review",
                        style = MaterialTheme.typography.labelSmall,
                        color = AppThemeColors.current.warning,
                    )
                }
            }
        }
    }
}

// ── Helper Badges ────────────────────────────────────────────

@Composable
private fun TechnicianOrderStatusBadge(status: OrderStatus) {
    val (text, type) = when (status) {
        OrderStatus.RECEIVED -> "Received" to StatusType.Pending
        OrderStatus.IN_PROGRESS -> "Processing" to StatusType.InProgress
        OrderStatus.SAMPLE_COLLECTED -> "Sample Ready" to StatusType.Active
        OrderStatus.PROCESSING -> "Processing" to StatusType.InProgress
        OrderStatus.COMPLETED -> "Completed" to StatusType.Completed
        OrderStatus.CANCELLED -> "Cancelled" to StatusType.Cancelled
    }
    StatusBadge(text = text, type = type)
}

@Composable
private fun TechnicianPriorityBadge(priority: OrderPriority) {
    val (text, color) = when (priority) {
        OrderPriority.STAT -> "STAT" to AppThemeColors.current.critical
        OrderPriority.URGENT -> "Urgent" to AppThemeColors.current.warning
        OrderPriority.ROUTINE -> "Routine" to MaterialTheme.colorScheme.onSurfaceVariant
    }
    Text(
        text = text,
        style = MaterialTheme.typography.labelSmall,
        color = color,
        fontWeight = FontWeight.Bold,
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
