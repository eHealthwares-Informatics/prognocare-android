package com.ehealthwares.prognocare.feature.dashboard.nurse

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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Assessment
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.MedicalServices
import androidx.compose.material.icons.outlined.Bloodtype
import androidx.compose.material.icons.outlined.EventAvailable
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.ehealthwares.prognocare.designsystem.components.StatusBadge
import com.ehealthwares.prognocare.designsystem.components.StatusType
import com.ehealthwares.prognocare.designsystem.theme.Spacing
import com.ehealthwares.prognocare.designsystem.theme.Tertiary
import com.ehealthwares.prognocare.designsystem.theme.Warning

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NurseTaskListScreen(
    onBack: () -> Unit,
) {
    var selectedFilter by remember { mutableStateOf("All") }
    val filters = listOf("All", "Pending", "In Progress", "Completed")

    val sampleTasks = listOf(
        NurseTask("1", "Chidi Okonkwo", "PT-001", NurseTaskType.VITALS,
            "Record pre-consultation vitals", TaskPriority.HIGH, "09:00 AM", TaskStatus.PENDING),
        NurseTask("2", "Amina Bello", "PT-002", NurseTaskType.MEDICATION,
            "Administer Metformin 500mg", TaskPriority.NORMAL, "09:30 AM", TaskStatus.PENDING),
        NurseTask("3", "Emeka Nwosu", "PT-003", NurseTaskType.CHECK_IN,
            "Check in for follow-up appointment", TaskPriority.NORMAL, "10:00 AM", TaskStatus.PENDING),
        NurseTask("4", "Fatima Yusuf", "PT-004", NurseTaskType.MEDICATION,
            "Administer Lisinopril 10mg", TaskPriority.HIGH, "10:30 AM", TaskStatus.IN_PROGRESS),
        NurseTask("5", "Tunde Adeyemi", "PT-005", NurseTaskType.VITALS,
            "Record post-procedure vitals", TaskPriority.URGENT, "11:00 AM", TaskStatus.PENDING),
        NurseTask("6", "Ngozi Okafor", "PT-006", NurseTaskType.ASSESSMENT,
            "Pain assessment and documentation", TaskPriority.NORMAL, "11:30 AM", TaskStatus.PENDING),
        NurseTask("7", "Ibrahim Mohammed", "PT-007", NurseTaskType.SPECIMEN,
            "Collect blood sample for lab", TaskPriority.HIGH, "02:00 PM", TaskStatus.PENDING),
        NurseTask("8", "Grace Obi", "PT-008", NurseTaskType.DOCUMENTATION,
            "Complete discharge documentation", TaskPriority.NORMAL, "08:30 AM", TaskStatus.COMPLETED),
    )

    val filteredTasks = remember(selectedFilter) {
        when (selectedFilter) {
            "Pending" -> sampleTasks.filter { it.status == TaskStatus.PENDING }
            "In Progress" -> sampleTasks.filter { it.status == TaskStatus.IN_PROGRESS }
            "Completed" -> sampleTasks.filter { it.status == TaskStatus.COMPLETED }
            else -> sampleTasks
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Tasks",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.SemiBold,
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface),
            )
        },
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
        ) {
            // Filter chips
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = Spacing.lg, vertical = Spacing.sm),
                horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
            ) {
                filters.forEach { filter ->
                    FilterChip(
                        selected = selectedFilter == filter,
                        onClick = { selectedFilter = filter },
                        label = { Text(filter) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                        ),
                    )
                }
            }

            LazyColumn(
                contentPadding = PaddingValues(horizontal = Spacing.lg),
                verticalArrangement = Arrangement.spacedBy(Spacing.sm),
            ) {
                items(filteredTasks) { task ->
                    TaskCard(task = task)
                }
            }
        }
    }
}

@Composable
private fun TaskCard(
    task: NurseTask,
    modifier: Modifier = Modifier,
) {
    val priorityColor = when (task.priority) {
        TaskPriority.URGENT -> MaterialTheme.colorScheme.error
        TaskPriority.HIGH -> Warning
        TaskPriority.NORMAL -> MaterialTheme.colorScheme.primary
        TaskPriority.LOW -> MaterialTheme.colorScheme.onSurfaceVariant
    }

    val taskIcon: ImageVector = when (task.taskType) {
        NurseTaskType.VITALS -> Icons.Outlined.Bloodtype
        NurseTaskType.MEDICATION -> Icons.Default.MedicalServices
        NurseTaskType.CHECK_IN, NurseTaskType.CHECK_OUT -> Icons.Outlined.EventAvailable
        NurseTaskType.ASSESSMENT, NurseTaskType.DOCUMENTATION -> Icons.Default.Assessment
        NurseTaskType.SPECIMEN -> Icons.Outlined.Bloodtype
    }

    val isCompleted = task.status == TaskStatus.COMPLETED

    Card(
        modifier = modifier
            .fillMaxWidth()
            .then(if (!isCompleted) Modifier.clickable { } else Modifier),
        shape = RoundedCornerShape(Spacing.base),
        colors = CardDefaults.cardColors(
            containerColor = if (isCompleted) {
                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
            } else {
                MaterialTheme.colorScheme.surface
            },
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = if (isCompleted) 0.dp else 1.dp),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(Spacing.base),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            // Task type icon
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(RoundedCornerShape(Spacing.sm))
                    .background(
                        if (isCompleted) Tertiary.copy(alpha = 0.1f)
                        else priorityColor.copy(alpha = 0.1f)
                    ),
                contentAlignment = Alignment.Center,
            ) {
                if (isCompleted) {
                    Icon(
                        imageVector = Icons.Default.CheckCircle,
                        contentDescription = null,
                        tint = Tertiary,
                        modifier = Modifier.size(22.dp),
                    )
                } else {
                    Icon(
                        imageVector = taskIcon,
                        contentDescription = null,
                        tint = priorityColor,
                        modifier = Modifier.size(22.dp),
                    )
                }
            }

            Spacer(modifier = Modifier.width(Spacing.md))

            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = task.patientName,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Medium,
                        color = if (isCompleted) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurface,
                    )
                    Spacer(modifier = Modifier.width(Spacing.sm))
                    if (!isCompleted) {
                        StatusBadge(
                            text = task.priority.name,
                            type = when (task.priority) {
                                TaskPriority.URGENT -> StatusType.Urgent
                                TaskPriority.HIGH -> StatusType.Active
                                TaskPriority.NORMAL -> StatusType.Scheduled
                                TaskPriority.LOW -> StatusType.Draft
                            },
                        )
                    }
                }
                Spacer(modifier = Modifier.height(Spacing.xxs))
                Text(
                    text = task.description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Spacer(modifier = Modifier.height(Spacing.xxs))
                Text(
                    text = "${task.scheduledTime} · ${task.taskType.name.replace("_", " ").lowercase().replaceFirstChar { it.uppercase() }}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            if (!isCompleted) {
                IconButton(onClick = { /* complete */ }) {
                    Icon(
                        imageVector = Icons.Default.Check,
                        contentDescription = "Complete",
                        tint = Tertiary,
                        modifier = Modifier.size(28.dp),
                    )
                }
            }
        }
    }
}
