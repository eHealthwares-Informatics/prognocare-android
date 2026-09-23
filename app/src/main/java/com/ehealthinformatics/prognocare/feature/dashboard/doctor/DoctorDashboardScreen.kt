package com.ehealthinformatics.prognocare.feature.dashboard.doctor

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
import androidx.compose.material.icons.filled.MedicalServices
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.outlined.EditNote
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.MedicalServices
import androidx.compose.material.icons.outlined.People
import androidx.compose.material.icons.outlined.Receipt
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FloatingActionButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ehealthinformatics.prognocare.designsystem.components.AppointmentRow
import com.ehealthinformatics.prognocare.designsystem.components.DashboardKpiCard
import com.ehealthinformatics.prognocare.designsystem.components.DashboardQuickAction
import com.ehealthinformatics.prognocare.designsystem.components.SectionHeader
import com.ehealthinformatics.prognocare.designsystem.components.StatusType
import com.ehealthinformatics.prognocare.designsystem.theme.AppThemeColors
import com.ehealthinformatics.prognocare.designsystem.theme.Spacing

@Composable
fun DoctorDashboardScreen(
    onNavigateToAppointments: () -> Unit,
    onNavigateToPatients: () -> Unit,
    onNavigateToPatientDetail: (String) -> Unit,
    onNavigateToChat: () -> Unit,
    onNavigateToProfile: () -> Unit = {},
    viewModel: DoctorDashboardViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    Scaffold(
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = { /* new encounter */ },
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
                Text("New Encounter", fontWeight = FontWeight.SemiBold)
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
                        Column {
                            Text(
                                text = "${state.greeting},",
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                            Text(
                                text = state.doctorName,
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
                            // Notification bell with badge
                            Box {
                                IconButton(onClick = { /* notifications */ }) {
                                    Icon(
                                        imageVector = Icons.Default.Notifications,
                                        contentDescription = "Notifications",
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.size(28.dp),
                                    )
                                }
                                if (state.urgentCount > 0) {
                                    Box(
                                        modifier = Modifier
                                            .align(Alignment.TopEnd)
                                            .size(20.dp)
                                            .clip(CircleShape)
                                            .background(AppThemeColors.current.notificationBadge),
                                        contentAlignment = Alignment.Center,
                                    ) {
                                        Text(
                                            text = "${state.urgentCount}",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = AppThemeColors.current.onNotificationBadge,
                                            fontWeight = FontWeight.Bold,
                                        )
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.width(Spacing.sm))

                            // Avatar
                            Box(
                                modifier = Modifier
                                    .size(48.dp)
                                    .clip(CircleShape)
                                    .background(MaterialTheme.colorScheme.primary)
                                    .clickable { onNavigateToProfile() },
                                contentAlignment = Alignment.Center,
                            ) {
                                Text(
                                    text = state.doctorName.take(1),
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
                            title = "Today's Appts",
                            value = "${state.todayAppointments}",
                            icon = Icons.Default.CalendarMonth,
                            iconTint = AppThemeColors.current.kpiBlue,
                            iconBg = AppThemeColors.current.kpiBlueLight,
                            onClick = onNavigateToAppointments,
                            modifier = Modifier.weight(1f),
                        )
                        DashboardKpiCard(
                            title = "Active",
                            value = "${state.activeEncounters}",
                            icon = Icons.Default.MedicalServices,
                            iconTint = AppThemeColors.current.kpiGreen,
                            iconBg = AppThemeColors.current.kpiGreenLight,
                            onClick = { /* active encounters */ },
                            modifier = Modifier.weight(1f),
                        )
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
                    ) {
                        DashboardKpiCard(
                            title = "Total Patients",
                            value = "${state.totalPatients}",
                            icon = Icons.Default.Person,
                            iconTint = AppThemeColors.current.kpiPurple,
                            iconBg = AppThemeColors.current.kpiPurpleLight,
                            onClick = onNavigateToPatients,
                            modifier = Modifier.weight(1f),
                        )
                        DashboardKpiCard(
                            title = "Completed",
                            value = "${state.completedToday}",
                            icon = Icons.Default.CheckCircle,
                            iconTint = AppThemeColors.current.kpiOrange,
                            iconBg = AppThemeColors.current.kpiOrangeLight,
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
                            icon = Icons.Outlined.EditNote,
                            label = "Clinical Note",
                            iconTint = AppThemeColors.current.kpiBlue,
                            iconBg = AppThemeColors.current.kpiBlueLight,
                            onClick = { /* new encounter */ },
                            modifier = Modifier.weight(1f),
                        )
                        DashboardQuickAction(
                            icon = Icons.Outlined.Receipt,
                            label = "New Request",
                            iconTint = AppThemeColors.current.kpiPurple,
                            iconBg = AppThemeColors.current.kpiPurpleLight,
                            onClick = { /* new request */ },
                            modifier = Modifier.weight(1f),
                        )
                        DashboardQuickAction(
                            icon = Icons.Outlined.People,
                            label = "View Patients",
                            iconTint = AppThemeColors.current.kpiGreen,
                            iconBg = AppThemeColors.current.kpiGreenLight,
                            onClick = onNavigateToPatients,
                            modifier = Modifier.weight(1f),
                        )
                        DashboardQuickAction(
                            icon = Icons.Outlined.CalendarMonth,
                            label = "Schedule",
                            iconTint = AppThemeColors.current.kpiOrange,
                            iconBg = AppThemeColors.current.kpiOrangeLight,
                            onClick = onNavigateToAppointments,
                            modifier = Modifier.weight(1f),
                        )
                    }
                }
            }

            // ── Upcoming Appointments ─────────────────────────
            item {
                Column(modifier = Modifier.padding(horizontal = Spacing.lg)) {
                    SectionHeader(
                        title = "Upcoming Appointments",
                        actionText = "View All",
                        onActionClick = onNavigateToAppointments,
                    )
                    Spacer(modifier = Modifier.height(Spacing.md))
                }
            }

            items(state.upcomingAppointments) { appointment ->
                AppointmentRow(
                    time = appointment.time.split(" ")[0],
                    timePeriod = appointment.time.split(" ").getOrElse(1) { "" },
                    patientName = appointment.patientName,
                    appointmentType = appointment.type,
                    statusText = when (appointment.status) {
                        "IN_PROGRESS" -> "In Progress"
                        "SCHEDULED" -> "Scheduled"
                        else -> appointment.status
                    },
                    statusType = when (appointment.status) {
                        "IN_PROGRESS" -> StatusType.InProgress
                        "SCHEDULED" -> StatusType.Scheduled
                        else -> StatusType.Pending
                    },
                    onClick = { onNavigateToPatientDetail(appointment.patientId) },
                    modifier = Modifier.padding(horizontal = Spacing.lg),
                )
            }

            // Bottom spacer for FAB
            item { Spacer(modifier = Modifier.height(Spacing.lg)) }
        }
    }
}
