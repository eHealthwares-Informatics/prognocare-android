package com.ehealthinformatics.prognocare.feature.dashboard.finance

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
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.outlined.AccountBalance
import androidx.compose.material.icons.outlined.Description
import androidx.compose.material.icons.outlined.Payment
import androidx.compose.material.icons.outlined.Warning
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FloatingActionButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
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

@Composable
fun FinanceDashboardScreen(
    onNavigateToBills: () -> Unit,
    onNavigateToBillDetail: (String) -> Unit,
    onNavigateToPayments: () -> Unit,
    onNavigateToPatientSearch: () -> Unit,
    onNavigateToChat: () -> Unit,
    onNavigateToProfile: () -> Unit = {},
    viewModel: FinanceDashboardViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    Scaffold(
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = { /* new bill */ },
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
                Text("New Bill", fontWeight = FontWeight.SemiBold)
            }
        },
    ) { innerPadding ->
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
                                text = state.financeName,
                                style = MaterialTheme.typography.headlineMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary,
                            )
                            Spacer(modifier = Modifier.height(Spacing.xs))
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.CalendarMonth,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(16.dp),
                                )
                                Spacer(modifier = Modifier.width(Spacing.xs))
                                Text(
                                    text = state.todayDate,
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
                                if (state.overdueBills > 0) {
                                    Box(
                                        modifier = Modifier
                                            .align(Alignment.TopEnd)
                                            .size(20.dp)
                                            .clip(CircleShape)
                                            .background(AppThemeColors.current.notificationBadge),
                                        contentAlignment = Alignment.Center,
                                    ) {
                                        Text(
                                            text = "${state.overdueBills}",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onPrimary,
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
                                    text = state.financeName.take(1),
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
                            title = "Total Revenue",
                            value = state.totalRevenue,
                            icon = Icons.Outlined.AccountBalance,
                            iconTint = AppThemeColors.current.kpiGreen,
                            iconBg = AppThemeColors.current.kpiGreenLight,
                            onClick = { /* revenue */ },
                            modifier = Modifier.weight(1f),
                        )
                        DashboardKpiCard(
                            title = "Pending Bills",
                            value = "${state.pendingBills}",
                            icon = Icons.Outlined.Description,
                            iconTint = AppThemeColors.current.kpiOrange,
                            iconBg = AppThemeColors.current.kpiOrangeLight,
                            onClick = onNavigateToBills,
                            modifier = Modifier.weight(1f),
                        )
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
                    ) {
                        DashboardKpiCard(
                            title = "Completed",
                            value = "${state.completedPayments}",
                            icon = Icons.Default.CheckCircle,
                            iconTint = AppThemeColors.current.kpiBlue,
                            iconBg = AppThemeColors.current.kpiBlueLight,
                            onClick = onNavigateToPayments,
                            modifier = Modifier.weight(1f),
                        )
                        DashboardKpiCard(
                            title = "Overdue",
                            value = "${state.overdueBills}",
                            icon = Icons.Outlined.Warning,
                            iconTint = MaterialTheme.colorScheme.error,
                            iconBg = MaterialTheme.colorScheme.errorContainer,
                            onClick = { /* overdue */ },
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
                            icon = Icons.Outlined.Description,
                            label = "Create Bill",
                            iconTint = AppThemeColors.current.kpiBlue,
                            iconBg = AppThemeColors.current.kpiBlueLight,
                            onClick = { /* create bill */ },
                            modifier = Modifier.weight(1f),
                        )
                        DashboardQuickAction(
                            icon = Icons.Outlined.Payment,
                            label = "Record Payment",
                            iconTint = AppThemeColors.current.kpiGreen,
                            iconBg = AppThemeColors.current.kpiGreenLight,
                            onClick = onNavigateToPayments,
                            modifier = Modifier.weight(1f),
                        )
                        DashboardQuickAction(
                            icon = Icons.Default.Person,
                            label = "Patient Search",
                            iconTint = AppThemeColors.current.kpiPurple,
                            iconBg = AppThemeColors.current.kpiPurpleLight,
                            onClick = onNavigateToPatientSearch,
                            modifier = Modifier.weight(1f),
                        )
                        DashboardQuickAction(
                            icon = Icons.Outlined.Warning,
                            label = "Overdue Bills",
                            iconTint = MaterialTheme.colorScheme.error,
                            iconBg = MaterialTheme.colorScheme.errorContainer,
                            onClick = { /* overdue bills */ },
                            modifier = Modifier.weight(1f),
                        )
                    }
                }
            }

            // ── Recent Bills ──────────────────────────────────
            item {
                Column(modifier = Modifier.padding(horizontal = Spacing.lg)) {
                    SectionHeader(
                        title = "Recent Bills",
                        actionText = "View All",
                        onActionClick = onNavigateToBills,
                    )
                    Spacer(modifier = Modifier.height(Spacing.md))
                }
            }

            item {
                com.ehealthinformatics.prognocare.designsystem.components.DemoDataChip(
                    text = "Not available — billing module lives in rxsoft-backend, not EMR",
                )
            }

            items(state.recentBills) { bill ->
                BillCard(
                    bill = bill,
                    onClick = { onNavigateToBillDetail(bill.id) },
                    modifier = Modifier.padding(horizontal = Spacing.lg),
                )
            }

            // ── Recent Payments ───────────────────────────────
            item {
                Column(modifier = Modifier.padding(horizontal = Spacing.lg)) {
                    SectionHeader(
                        title = "Recent Payments",
                        actionText = "View All",
                        onActionClick = onNavigateToPayments,
                    )
                    Spacer(modifier = Modifier.height(Spacing.md))
                }
            }

            items(state.recentPayments) { payment ->
                PaymentCard(
                    payment = payment,
                    modifier = Modifier.padding(horizontal = Spacing.lg),
                )
            }

            item { Spacer(modifier = Modifier.height(Spacing.xxl)) }
        }
    }
}

@Composable
private fun BillCard(
    bill: Bill,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val (statusType, statusText) = when (bill.status) {
        BillStatus.DRAFT -> StatusType.Draft to "Draft"
        BillStatus.PENDING -> StatusType.Pending to "Pending"
        BillStatus.PARTIAL -> StatusType.InProgress to "Partial"
        BillStatus.PAID -> StatusType.Completed to "Paid"
        BillStatus.OVERDUE -> StatusType.Urgent to "Overdue"
        BillStatus.CANCELLED -> StatusType.Cancelled to "Cancelled"
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
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = bill.billNumber,
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.SemiBold,
                    )
                    Text(
                        text = bill.patientName,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold,
                    )
                    Text(
                        text = "Due: ${bill.dueDate}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = bill.totalDisplay,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                    StatusBadge(text = statusText, type = statusType)
                }
            }

            Spacer(modifier = Modifier.height(Spacing.sm))

            // Progress bar
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = "Paid: ${bill.paidDisplay}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(
                    text = "Balance: ${bill.balanceDisplay}",
                    style = MaterialTheme.typography.labelSmall,
                    color = if (bill.balanceAmount > 0) AppThemeColors.current.kpiOrange else AppThemeColors.current.kpiGreen,
                    fontWeight = FontWeight.SemiBold,
                )
            }
            Spacer(modifier = Modifier.height(Spacing.xs))
            LinearProgressIndicator(
                progress = { bill.paymentProgress },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(6.dp)
                    .clip(RoundedCornerShape(3.dp)),
                color = when {
                    bill.paymentProgress >= 1f -> AppThemeColors.current.kpiGreen
                    bill.paymentProgress > 0f -> AppThemeColors.current.kpiBlue
                    else -> AppThemeColors.current.kpiOrange
                },
                trackColor = MaterialTheme.colorScheme.outlineVariant,
            )
        }
    }
}

@Composable
private fun PaymentCard(
    payment: Payment,
    modifier: Modifier = Modifier,
) {
    Card(
        modifier = modifier.fillMaxWidth(),
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
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(AppThemeColors.current.kpiGreenLight),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = Icons.Outlined.Payment,
                    contentDescription = null,
                    tint = AppThemeColors.current.kpiGreen,
                    modifier = Modifier.size(22.dp),
                )
            }

            Spacer(modifier = Modifier.width(Spacing.md))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = payment.patientName,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                )
                Text(
                    text = "${payment.paymentMethod.name.replace("_", " ")} · ${payment.paymentDate}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(
                    text = payment.reference,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = payment.amountDisplay,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = AppThemeColors.current.kpiGreen,
                )
                StatusBadge(
                    text = payment.statusDisplay,
                    type = when (payment.status) {
                        PaymentStatus.COMPLETED -> StatusType.Completed
                        PaymentStatus.PENDING -> StatusType.Pending
                        PaymentStatus.FAILED -> StatusType.Urgent
                        PaymentStatus.REFUNDED -> StatusType.Cancelled
                    },
                )
            }
        }
    }
}
