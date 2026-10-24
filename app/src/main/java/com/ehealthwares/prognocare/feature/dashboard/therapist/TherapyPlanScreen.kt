package com.ehealthwares.prognocare.feature.dashboard.therapist

import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Save
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FloatingActionButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.ehealthwares.prognocare.designsystem.theme.Spacing

data class TherapyPlanItem(
    val id: String,
    val goal: String,
    val frequency: String,
    val duration: String,
    val exercises: List<String>,
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TherapyPlanScreen(
    patientName: String,
    onBack: () -> Unit,
    onSave: () -> Unit,
) {
    val plans = remember {
        mutableStateListOf(
            TherapyPlanItem(
                id = "1",
                goal = "Improve shoulder mobility",
                frequency = "3x per week",
                duration = "6 weeks",
                exercises = listOf("Pendulum swings", "Shoulder rotations", "Resistance bands"),
            ),
            TherapyPlanItem(
                id = "2",
                goal = "Strengthen rotator cuff",
                frequency = "2x per week",
                duration = "8 weeks",
                exercises = listOf("External rotation", "Internal rotation", "Scapular squeezes"),
            ),
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Therapy Plan") },
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
            ExtendedFloatingActionButton(
                onClick = onSave,
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
                shape = RoundedCornerShape(Spacing.lg),
                elevation = FloatingActionButtonDefaults.elevation(defaultElevation = 6.dp),
            ) {
                Icon(Icons.Default.Save, contentDescription = null)
                Spacer(modifier = Modifier.width(Spacing.sm))
                Text("Save Plan", fontWeight = FontWeight.SemiBold)
            }
        },
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            verticalArrangement = Arrangement.spacedBy(Spacing.sm),
        ) {
            // ── Patient Header ───────────────────────────────
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = Spacing.lg),
                    shape = RoundedCornerShape(Spacing.base),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                ) {
                    Column(modifier = Modifier.padding(Spacing.base)) {
                        Text("Therapy Plan for", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text(patientName, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    }
                }
            }

            // ── Plan Items ───────────────────────────────────
            items(plans) { plan ->
                TherapyPlanCard(
                    plan = plan,
                    onDelete = { plans.remove(plan) },
                )
            }

            // ── Add Goal Button ──────────────────────────────
            item {
                OutlinedButton(
                    onClick = {
                        plans.add(
                            TherapyPlanItem(
                                id = "${plans.size + 1}",
                                goal = "New Goal",
                                frequency = "2x per week",
                                duration = "4 weeks",
                                exercises = emptyList(),
                            )
                        )
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = Spacing.lg),
                    shape = RoundedCornerShape(Spacing.sm),
                ) {
                    Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(Spacing.xs))
                    Text("Add Goal")
                }
            }

            // ── Bottom Spacer ────────────────────────────────
            item { Spacer(modifier = Modifier.height(100.dp)) }
        }
    }
}

@Composable
private fun TherapyPlanCard(
    plan: TherapyPlanItem,
    onDelete: () -> Unit,
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = Spacing.lg),
        shape = RoundedCornerShape(Spacing.base),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
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
                        text = plan.goal,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                }
                IconButton(onClick = onDelete, modifier = Modifier.size(32.dp)) {
                    Icon(
                        Icons.Default.Delete,
                        contentDescription = "Remove",
                        tint = MaterialTheme.colorScheme.error,
                        modifier = Modifier.size(16.dp),
                    )
                }
            }

            Spacer(modifier = Modifier.height(Spacing.sm))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(Spacing.md),
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("Frequency: ", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(plan.frequency, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Medium)
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("Duration: ", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(plan.duration, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Medium)
                }
            }

            if (plan.exercises.isNotEmpty()) {
                Spacer(modifier = Modifier.height(Spacing.sm))
                Text("Exercises:", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                plan.exercises.forEach { exercise ->
                    Text(
                        "• $exercise",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.padding(start = Spacing.sm, top = Spacing.xs),
                    )
                }
            }
        }
    }
}
