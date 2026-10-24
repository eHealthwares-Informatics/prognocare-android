package com.ehealthwares.prognocare.feature.dashboard.patient

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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.outlined.Alarm
import androidx.compose.material.icons.outlined.MedicalServices
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.ehealthwares.prognocare.designsystem.components.EmptyState
import com.ehealthwares.prognocare.designsystem.components.StatusBadge
import com.ehealthwares.prognocare.designsystem.components.StatusType
import com.ehealthwares.prognocare.designsystem.theme.AppThemeColors
import com.ehealthwares.prognocare.designsystem.theme.Spacing

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PatientMedicationsScreen(
    onBack: () -> Unit,
) {
    var selectedTab by remember { mutableIntStateOf(0) }
    val tabs = listOf("Active", "Past")

    val activeMedications = remember {
        listOf(
            PatientMedication(
                "1", "Lisinopril", "10mg", "Once daily", "Oral",
                "Dr. Adebayo", "Jan 15, 2026",
                instructions = "Take in the morning with water. Avoid potassium supplements.",
                nextDose = "Tomorrow, 8:00 AM",
                refillDate = "Aug 21, 2026",
            ),
            PatientMedication(
                "2", "Metformin", "500mg", "Twice daily", "Oral",
                "Dr. Fatima", "Mar 1, 2026",
                instructions = "Take with meals to reduce GI side effects. Monitor blood sugar regularly.",
                nextDose = "Today, 6:00 PM",
            ),
            PatientMedication(
                "3", "Aspirin", "81mg", "Once daily", "Oral",
                "Dr. Adebayo", "Jan 15, 2026",
                instructions = "Low-dose for cardiac prophylaxis. Take with food.",
                nextDose = "Tomorrow, 8:00 AM",
            ),
            PatientMedication(
                "4", "Vitamin D3", "1000 IU", "Once daily", "Oral",
                "Dr. Adebayo", "Jun 1, 2026",
                instructions = "Take with food for better absorption. Supports bone health.",
                nextDose = "Tomorrow, 8:00 AM",
            ),
        )
    }

    val pastMedications = remember {
        listOf(
            PatientMedication(
                "5", "Amoxicillin", "500mg", "Three times daily", "Oral",
                "Dr. Adebayo", "May 1, 2026", "May 14, 2026",
                instructions = "Complete full course for bacterial infection.",
                isActive = false,
            ),
            PatientMedication(
                "6", "Omeprazole", "20mg", "Once daily", "Oral",
                "Dr. Fatima", "Feb 1, 2026", "Feb 28, 2026",
                instructions = "For acid reflux. Take 30 min before breakfast.",
                isActive = false,
            ),
        )
    }

    val displayedMedications = if (selectedTab == 0) activeMedications else pastMedications

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("My Medications") },
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
            // ── Tab Row ──────────────────────────────────────
            TabRow(
                selectedTabIndex = selectedTab,
                containerColor = MaterialTheme.colorScheme.surface,
                contentColor = MaterialTheme.colorScheme.primary,
            ) {
                tabs.forEachIndexed { index, title ->
                    Tab(
                        selected = selectedTab == index,
                        onClick = { selectedTab = index },
                        text = {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = title,
                                    fontWeight = if (selectedTab == index) FontWeight.SemiBold else FontWeight.Normal,
                                )
                                if (index == 0) {
                                    Spacer(modifier = Modifier.width(Spacing.xs))
                                    StatusBadge(
                                        text = "${activeMedications.size}",
                                        type = StatusType.Active,
                                    )
                                }
                            }
                        },
                    )
                }
            }

            if (displayedMedications.isEmpty()) {
                EmptyState(
                    icon = Icons.Outlined.MedicalServices,
                    title = if (selectedTab == 0) "No active medications" else "No past medications",
                    message = if (selectedTab == 0)
                        "Your current prescriptions will appear here"
                    else
                        "Discontinued medications will appear here",
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
                    items(displayedMedications) { medication ->
                        PatientMedicationDetailCard(medication = medication)
                    }
                    item { Spacer(modifier = Modifier.height(Spacing.xxxl)) }
                }
            }
        }
    }
}

@Composable
private fun PatientMedicationDetailCard(
    medication: PatientMedication,
    modifier: Modifier = Modifier,
) {
    Card(
        modifier = modifier.fillMaxWidth(),
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
                            .clip(RoundedCornerShape(Spacing.sm))
                    .background(AppThemeColors.current.kpiGreenLight),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = Icons.Outlined.MedicalServices,
                    contentDescription = null,
                    tint = AppThemeColors.current.kpiGreen,
                            modifier = Modifier.size(22.dp),
                        )
                    }
                    Spacer(modifier = Modifier.width(Spacing.md))
                    Column {
                        Text(
                            text = medication.name,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                        )
                        Text(
                            text = "${medication.dosage} · ${medication.frequency}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
                if (medication.isActive && medication.needsRefill) {
                    StatusBadge(text = "Refill due", type = StatusType.Urgent)
                } else if (!medication.isActive) {
                    StatusBadge(text = "Completed", type = StatusType.Completed)
                }
            }

            Spacer(modifier = Modifier.height(Spacing.md))

            // ── Details Grid ────────────────────────────────
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(Spacing.base),
            ) {
                DetailChip(
                    icon = Icons.Outlined.Schedule,
                    label = "Route",
                    value = medication.route,
                    modifier = Modifier.weight(1f),
                )
                DetailChip(
                    icon = Icons.Default.Info,
                    label = "Prescriber",
                    value = medication.prescriber,
                    modifier = Modifier.weight(1f),
                )
            }

            Spacer(modifier = Modifier.height(Spacing.sm))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(Spacing.base),
            ) {
                DetailChip(
                    icon = Icons.Outlined.Schedule,
                    label = "Started",
                    value = medication.startDate,
                    modifier = Modifier.weight(1f),
                )
                if (medication.endDate != null) {
                    DetailChip(
                        icon = Icons.Default.CheckCircle,
                        label = "Ended",
                        value = medication.endDate,
                        modifier = Modifier.weight(1f),
                    )
                } else if (medication.nextDose.isNotEmpty()) {
                    DetailChip(
                        icon = Icons.Outlined.Alarm,
                        label = "Next dose",
                        value = medication.nextDose,
                        modifier = Modifier.weight(1f),
                        valueColor = MaterialTheme.colorScheme.primary,
                    )
                }
            }

            // ── Refill Alert ────────────────────────────────
            if (medication.isActive && medication.needsRefill) {
                Spacer(modifier = Modifier.height(Spacing.md))
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(Spacing.sm),
                    colors = CardDefaults.cardColors(
                        containerColor = AppThemeColors.current.warningContainer.copy(alpha = 0.3f),
                    ),
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(Spacing.sm),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Icon(
                            imageVector = Icons.Default.Notifications,
                            contentDescription = null,
                            tint = AppThemeColors.current.onWarningContainer,
                            modifier = Modifier.size(18.dp),
                        )
                        Spacer(modifier = Modifier.width(Spacing.sm))
                        Text(
                            text = "Refill needed by ${medication.refillDate}",
                            style = MaterialTheme.typography.bodySmall,
                            color = AppThemeColors.current.onWarningContainer,
                            fontWeight = FontWeight.Medium,
                        )
                    }
                }
            }

            // ── Instructions ────────────────────────────────
            if (medication.instructions.isNotEmpty()) {
                Spacer(modifier = Modifier.height(Spacing.md))
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                Spacer(modifier = Modifier.height(Spacing.sm))
                Text(
                    text = "Instructions",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontWeight = FontWeight.Medium,
                )
                Spacer(modifier = Modifier.height(Spacing.xxs))
                Text(
                    text = medication.instructions,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun DetailChip(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    value: String,
    modifier: Modifier = Modifier,
    valueColor: Color = MaterialTheme.colorScheme.onSurface,
) {
    Row(
        modifier = modifier
            .clip(RoundedCornerShape(Spacing.sm))
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))
            .padding(horizontal = Spacing.sm, vertical = Spacing.xs),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(14.dp),
        )
        Spacer(modifier = Modifier.width(Spacing.xs))
        Column {
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                text = value,
                style = MaterialTheme.typography.bodySmall,
                color = valueColor,
                fontWeight = FontWeight.Medium,
            )
        }
    }
}
