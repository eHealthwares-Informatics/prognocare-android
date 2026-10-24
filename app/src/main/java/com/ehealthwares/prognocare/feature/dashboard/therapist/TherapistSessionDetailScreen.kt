package com.ehealthwares.prognocare.feature.dashboard.therapist

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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FloatingActionButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.ehealthwares.prognocare.designsystem.components.StatusBadge
import com.ehealthwares.prognocare.designsystem.components.StatusType
import com.ehealthwares.prognocare.designsystem.theme.AppThemeColors
import com.ehealthwares.prognocare.designsystem.theme.Spacing

data class SessionDetail(
    val id: String,
    val patientName: String,
    val patientMrn: String,
    val sessionType: String,
    val duration: String,
    val objectives: List<String>,
    val exercises: List<String>,
    val notes: String,
    val progress: Float,
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TherapistSessionDetailScreen(
    sessionId: String,
    onBack: () -> Unit,
    onComplete: () -> Unit,
) {
    var sessionNotes by remember { mutableStateOf("") }

    val session = SessionDetail(
        id = sessionId,
        patientName = "Adaeze Nwankwo",
        patientMrn = "MRN-2024-1001",
        sessionType = "Physical Therapy",
        duration = "45 minutes",
        objectives = listOf(
            "Reduce shoulder pain from 7/10 to 4/10",
            "Improve range of motion by 20%",
            "Strengthen rotator cuff muscles",
        ),
        exercises = listOf(
            "Pendulum swings - 3 sets x 10 reps",
            "Shoulder rotations - 2 sets x 15 reps",
            "Resistance band exercises - 3 sets x 12 reps",
            "Wall push-ups - 2 sets x 10 reps",
        ),
        notes = "Patient responded well to manual therapy. Pain reduced during session.",
        progress = 0.6f,
    )

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Session Details") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    TextButton(onClick = onComplete) {
                        Icon(Icons.Default.Check, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        Spacer(modifier = Modifier.width(Spacing.xs))
                        Text("Complete", color = MaterialTheme.colorScheme.primary)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                ),
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = { /* save notes */ },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
                shape = RoundedCornerShape(Spacing.lg),
                elevation = FloatingActionButtonDefaults.elevation(defaultElevation = 6.dp),
            ) {
                Icon(Icons.Default.Add, contentDescription = null)
                Spacer(modifier = Modifier.width(Spacing.sm))
                Text("Save Notes", fontWeight = FontWeight.SemiBold)
            }
        },
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            verticalArrangement = Arrangement.spacedBy(Spacing.base),
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
                    Row(
                        modifier = Modifier.padding(Spacing.base),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Box(
                            modifier = Modifier
                                .size(52.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.primaryContainer),
                            contentAlignment = Alignment.Center,
                        ) {
                            Text(
                                text = session.patientName.take(1),
                                style = MaterialTheme.typography.titleLarge,
                                color = MaterialTheme.colorScheme.onPrimaryContainer,
                                fontWeight = FontWeight.Bold,
                            )
                        }
                        Spacer(modifier = Modifier.width(Spacing.md))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(session.patientName, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                            Text(session.patientMrn, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        StatusBadge(text = "In Session", type = StatusType.InProgress)
                    }
                }
            }

            // ── Session Progress ─────────────────────────────
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = Spacing.lg),
                    shape = RoundedCornerShape(Spacing.base),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                ) {
                    Column(modifier = Modifier.padding(Spacing.base)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                        ) {
                            Text("Session Progress", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                            Text("${(session.progress * 100).toInt()}%", style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                        }
                        Spacer(modifier = Modifier.height(Spacing.sm))
                        LinearProgressIndicator(
                            progress = { session.progress },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(8.dp)
                                .clip(RoundedCornerShape(4.dp)),
                            color = MaterialTheme.colorScheme.primary,
                            trackColor = MaterialTheme.colorScheme.surfaceVariant,
                        )
                    }
                }
            }

            // ── Objectives ───────────────────────────────────
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = Spacing.lg),
                    shape = RoundedCornerShape(Spacing.base),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                ) {
                    Column(modifier = Modifier.padding(Spacing.base)) {
                        Text("Objectives", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.height(Spacing.sm))
                        session.objectives.forEach { objective ->
                            Row(
                                modifier = Modifier.padding(vertical = Spacing.xs),
                                verticalAlignment = Alignment.Top,
                            ) {
                                Icon(
                                    Icons.Default.Check,
                                    contentDescription = null,
                                    tint = AppThemeColors.current.success,
                                    modifier = Modifier.size(16.dp),
                                )
                                Spacer(modifier = Modifier.width(Spacing.xs))
                                Text(objective, style = MaterialTheme.typography.bodySmall)
                            }
                        }
                    }
                }
            }

            // ── Exercises ────────────────────────────────────
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = Spacing.lg),
                    shape = RoundedCornerShape(Spacing.base),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                ) {
                    Column(modifier = Modifier.padding(Spacing.base)) {
                        Text("Exercises", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.height(Spacing.sm))
                        session.exercises.forEachIndexed { index, exercise ->
                            Row(
                                modifier = Modifier.padding(vertical = Spacing.xs),
                                verticalAlignment = Alignment.Top,
                            ) {
                                Text(
                                    "${index + 1}.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.primary,
                                    fontWeight = FontWeight.Bold,
                                )
                                Spacer(modifier = Modifier.width(Spacing.xs))
                                Text(exercise, style = MaterialTheme.typography.bodySmall)
                            }
                        }
                    }
                }
            }

            // ── Session Notes ────────────────────────────────
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = Spacing.lg),
                    shape = RoundedCornerShape(Spacing.base),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                ) {
                    Column(modifier = Modifier.padding(Spacing.base)) {
                        Text("Session Notes", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.height(Spacing.xs))
                        Text(session.notes, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }

            // ── Bottom Spacer ────────────────────────────────
            item { Spacer(modifier = Modifier.height(100.dp)) }
        }
    }
}
