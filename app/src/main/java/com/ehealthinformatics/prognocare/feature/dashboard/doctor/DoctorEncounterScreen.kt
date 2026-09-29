package com.ehealthinformatics.prognocare.feature.dashboard.doctor

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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.outlined.Description
import androidx.compose.material.icons.outlined.MonitorHeart
import androidx.compose.material.icons.outlined.ReceiptLong
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ehealthinformatics.prognocare.designsystem.components.ErrorState
import com.ehealthinformatics.prognocare.designsystem.components.StatusBadge
import com.ehealthinformatics.prognocare.designsystem.components.StatusType
import com.ehealthinformatics.prognocare.designsystem.theme.AppThemeColors
import com.ehealthinformatics.prognocare.designsystem.theme.Spacing
import kotlinx.coroutines.delay
import java.time.Duration
import java.time.OffsetDateTime

private fun statusTypeOf(status: String): StatusType = when (status.uppercase()) {
    "ACTIVE" -> StatusType.InProgress
    "COMPLETED" -> StatusType.Completed
    "CANCELLED" -> StatusType.Cancelled
    else -> StatusType.Pending
}

private fun requestStatusType(status: String): StatusType = when (status.uppercase()) {
    "REQUESTED" -> StatusType.Pending
    "IN_PROGRESS" -> StatusType.InProgress
    "COMPLETED", "FULFILLED" -> StatusType.Completed
    "CANCELLED" -> StatusType.Cancelled
    else -> StatusType.Pending
}

private fun formatDateTime(iso: String?): String = iso
    ?.take(16)
    ?.replace('T', ' ')
    .orEmpty()
    .ifBlank { "—" }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DoctorEncounterScreen(
    encounterId: String,
    onBack: () -> Unit,
    onOpenFormPicker: ((patientId: String, visitId: String?, encounterId: String?) -> Unit)? = null,
    onEditSubmission: ((patientId: String, visitId: String?, encounterId: String?, submissionId: String) -> Unit)? = null,
    viewModel: DoctorEncounterViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    LaunchedEffect(encounterId) { viewModel.bind(encounterId) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Clinical Encounter") },
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
        when {
            state.isLoading -> Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
            ) { CircularProgressIndicator() }

            state.encounter == null -> Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
            ) {
                ErrorState(
                    message = state.error ?: "Encounter not found",
                    onRetry = { viewModel.load() },
                    modifier = Modifier.align(Alignment.Center),
                )
            }

            else -> {
                val encounter = state.encounter!!
                val isActive = encounter.isActive
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding),
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(bottom = 32.dp),
                    verticalArrangement = Arrangement.spacedBy(Spacing.base),
                ) {
                    // ── Patient header + status + timer ─────────
                    item {
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = Spacing.lg),
                            shape = RoundedCornerShape(Spacing.base),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        ) {
                            Column(modifier = Modifier.padding(Spacing.base)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Box(
                                        modifier = Modifier
                                            .size(52.dp)
                                            .clip(CircleShape)
                                            .background(MaterialTheme.colorScheme.primaryContainer),
                                        contentAlignment = Alignment.Center,
                                    ) {
                                        Text(
                                            text = state.patientName.take(1).ifBlank { "?" },
                                            style = MaterialTheme.typography.titleLarge,
                                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                                            fontWeight = FontWeight.Bold,
                                        )
                                    }
                                    Spacer(modifier = Modifier.width(Spacing.md))
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            state.patientName,
                                            style = MaterialTheme.typography.titleMedium,
                                            fontWeight = FontWeight.Bold,
                                        )
                                        Text(
                                            state.patient?.patientId ?: encounter.patientId,
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        )
                                        Text(
                                            encounter.typeDisplay,
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.primary,
                                        )
                                    }
                                    StatusBadge(
                                        text = encounter.statusDisplay,
                                        type = statusTypeOf(encounter.status),
                                    )
                                }

                                Spacer(modifier = Modifier.height(Spacing.md))

                                // Times
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                ) {
                                    Column {
                                        Text("Created", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                        Text(formatDateTime(encounter.createdAt), style = MaterialTheme.typography.bodySmall)
                                    }
                                    Column {
                                        Text("Start", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                        Text(formatDateTime(encounter.encounterDatetime), style = MaterialTheme.typography.bodySmall)
                                    }
                                    Column(horizontalAlignment = Alignment.End) {
                                        Text("End", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                        Text(
                                            if (isActive) "—" else formatDateTime(encounter.endedAt),
                                            style = MaterialTheme.typography.bodySmall,
                                        )
                                    }
                                }

                                // Live timer while active; auto-ends at 8h cap.
                                if (isActive) {
                                    Spacer(modifier = Modifier.height(Spacing.md))
                                    val start = encounter.encounterDatetime
                                    var elapsed by androidx.compose.runtime.remember(encounter.id) {
                                        androidx.compose.runtime.mutableStateOf<Duration?>(null)
                                    }
                                    LaunchedEffect(encounter.id, start) {
                                        val startTime = runCatching { OffsetDateTime.parse(start) }.getOrNull()
                                        while (true) {
                                            elapsed = startTime?.let { Duration.between(it, OffsetDateTime.now()) }
                                            if (elapsed != null && elapsed!!.toHours() >= 8) {
                                                viewModel.endEncounter()
                                                break
                                            }
                                            delay(1000)
                                        }
                                    }
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(
                                            Icons.Default.Timer,
                                            contentDescription = null,
                                            tint = AppThemeColors.current.kpiBlue,
                                            modifier = Modifier.size(18.dp),
                                        )
                                        Spacer(modifier = Modifier.width(Spacing.xs))
                                        Text(
                                            text = elapsed?.let {
                                                String.format(
                                                    "%02d:%02d:%02d",
                                                    it.toHours(),
                                                    it.toMinutesPart(),
                                                    it.toSecondsPart(),
                                                )
                                            } ?: "—",
                                            style = MaterialTheme.typography.titleMedium,
                                            fontWeight = FontWeight.Bold,
                                        )
                                    }
                                    LinearProgressIndicator(
                                        progress = {
                                            val hours = (elapsed?.toMillis() ?: 0L) / 3_600_000.0
                                            (hours / 8.0).toFloat().coerceIn(0f, 1f)
                                        },
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(top = Spacing.xs),
                                    )
                                }
                            }
                        }
                    }

                    // ── Vitals (latest VITALS submission for the visit) ─
                    state.vitals?.let { vitals ->
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
                                        verticalAlignment = Alignment.CenterVertically,
                                    ) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Icon(
                                                Icons.Outlined.MonitorHeart,
                                                contentDescription = null,
                                                tint = AppThemeColors.current.kpiGreen,
                                                modifier = Modifier.size(18.dp),
                                            )
                                            Spacer(modifier = Modifier.width(Spacing.xs))
                                            Text("Vitals", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                                        }
                                        Text(
                                            text = listOfNotNull(
                                                vitals.recordedAt?.take(16)?.replace('T', ' '),
                                                vitals.recordedBy,
                                            ).joinToString(" · "),
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        )
                                    }
                                    Spacer(modifier = Modifier.height(Spacing.xs))
                                    vitals.rows().forEachIndexed { index, row ->
                                        if (index > 0) {
                                            HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant)
                                        }
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(vertical = Spacing.xs),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                        ) {
                                            Text(
                                                row.label,
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            )
                                            Text(
                                                row.value,
                                                style = MaterialTheme.typography.bodySmall,
                                                fontWeight = FontWeight.Medium,
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // ── Reason / notes ──────────────────────
                    if (!encounter.reason.isNullOrBlank() || !encounter.notes.isNullOrBlank()) {
                        item {
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = Spacing.lg),
                                shape = RoundedCornerShape(Spacing.base),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                            ) {
                                Column(modifier = Modifier.padding(Spacing.base)) {
                                    encounter.reason?.takeIf { it.isNotBlank() }?.let {
                                        Text("Reason", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                                        Spacer(modifier = Modifier.height(Spacing.xs))
                                        Text(it, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    }
                                    encounter.notes?.takeIf { it.isNotBlank() }?.let {
                                        if (!encounter.reason.isNullOrBlank()) Spacer(modifier = Modifier.height(Spacing.sm))
                                        Text("Notes", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                                        Spacer(modifier = Modifier.height(Spacing.xs))
                                        Text(it, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    }
                                }
                            }
                        }
                    }

                    // ── Documentation (encounter + visit) ───────
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
                                    verticalAlignment = Alignment.CenterVertically,
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(
                                            Icons.Outlined.Description,
                                            contentDescription = null,
                                            tint = AppThemeColors.current.kpiBlue,
                                            modifier = Modifier.size(18.dp),
                                        )
                                        Spacer(modifier = Modifier.width(Spacing.xs))
                                        Text("Documentation (${state.submissions.size})", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                                    }
                                    Row {
                                        IconButton(onClick = { viewModel.load() }, modifier = Modifier.size(32.dp)) {
                                            Icon(Icons.Default.Refresh, contentDescription = "Refresh", modifier = Modifier.size(16.dp))
                                        }
                                        IconButton(
                                            onClick = {
                                                state.patient?.let { p ->
                                                    onOpenFormPicker?.invoke(p.id, encounter.visitId, encounter.id)
                                                }
                                            },
                                            modifier = Modifier.size(32.dp),
                                        ) {
                                            Icon(Icons.Default.Add, contentDescription = "Add documentation", modifier = Modifier.size(16.dp))
                                        }
                                    }
                                }
                                if (state.submissions.isEmpty()) {
                                    Text(
                                        "No documentation for this encounter or visit yet",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    )
                                } else {
                                    Spacer(modifier = Modifier.height(Spacing.xs))
                                    state.submissions.forEach { submission ->
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .clickable {
                                                    state.patient?.let { p ->
                                                        onEditSubmission?.invoke(p.id, encounter.visitId, encounter.id, submission.id)
                                                    }
                                                }
                                                .padding(vertical = Spacing.xs),
                                            verticalAlignment = Alignment.CenterVertically,
                                        ) {
                                            Column(modifier = Modifier.weight(1f)) {
                                                Text(
                                                    submission.formName.ifBlank { "Form entry" },
                                                    style = MaterialTheme.typography.bodyMedium,
                                                    fontWeight = FontWeight.Medium,
                                                )
                                                Text(
                                                    listOfNotNull(
                                                        submission.status.lowercase().replaceFirstChar { c -> c.uppercase() },
                                                        submission.submittedAt?.take(10),
                                                    ).joinToString(" · "),
                                                    style = MaterialTheme.typography.bodySmall,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                )
                                                if (submission.schemaOutdated) {
                                                    Spacer(modifier = Modifier.height(Spacing.xs))
                                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                                        Icon(
                                                            Icons.Default.Warning,
                                                            contentDescription = null,
                                                            tint = MaterialTheme.colorScheme.error,
                                                            modifier = Modifier.size(14.dp),
                                                        )
                                                        Spacer(modifier = Modifier.width(Spacing.xs))
                                                        Text(
                                                            text = "Older schema (v${submission.formVersion}" +
                                                                (submission.schemaCurrentVersion?.let { " → v$it" } ?: "") +
                                                                ") — values may not match current fields",
                                                            style = MaterialTheme.typography.labelSmall,
                                                            color = MaterialTheme.colorScheme.error,
                                                        )
                                                    }
                                                }
                                            }
                                            Icon(
                                                Icons.Default.Edit,
                                                contentDescription = "Edit",
                                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                                modifier = Modifier.size(16.dp),
                                            )
                                        }
                                        HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant)
                                    }
                                }
                            }
                        }
                    }

                    // ── Requests (encounter + visit) ────────────
                    item {
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = Spacing.lg),
                            shape = RoundedCornerShape(Spacing.base),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        ) {
                            Column(modifier = Modifier.padding(Spacing.base)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        Icons.Outlined.ReceiptLong,
                                        contentDescription = null,
                                        tint = AppThemeColors.current.kpiPurple,
                                        modifier = Modifier.size(18.dp),
                                    )
                                    Spacer(modifier = Modifier.width(Spacing.xs))
                                    Text("Requests (${state.requests.size})", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                                }
                                if (state.requests.isEmpty()) {
                                    Spacer(modifier = Modifier.height(Spacing.xs))
                                    Text(
                                        "No requests for this encounter or visit yet",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    )
                                } else {
                                    Spacer(modifier = Modifier.height(Spacing.xs))
                                    state.requests.forEach { request ->
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(vertical = Spacing.xs),
                                            verticalAlignment = Alignment.CenterVertically,
                                        ) {
                                            Column(modifier = Modifier.weight(1f)) {
                                                Text(
                                                    listOfNotNull(
                                                        request.requestNumber,
                                                        request.requestType
                                                            .lowercase()
                                                            .replace('_', ' ')
                                                            .replaceFirstChar { c -> c.uppercase() },
                                                    ).joinToString(" · "),
                                                    style = MaterialTheme.typography.bodyMedium,
                                                    fontWeight = FontWeight.Medium,
                                                )
                                                Text(
                                                    request.diagnosis ?: "—",
                                                    style = MaterialTheme.typography.bodySmall,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                    maxLines = 1,
                                                )
                                            }
                                            StatusBadge(
                                                text = request.status.lowercase().replaceFirstChar { c -> c.uppercase() },
                                                type = requestStatusType(request.status),
                                            )
                                        }
                                        HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant)
                                    }
                                }
                            }
                        }
                    }

                    // ── End encounter ───────────────────────────
                    item {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = Spacing.lg),
                            verticalArrangement = Arrangement.spacedBy(Spacing.sm),
                        ) {
                            if (isActive) {
                                Button(
                                    onClick = { viewModel.endEncounter() },
                                    modifier = Modifier.fillMaxWidth(),
                                    enabled = !state.isBusy,
                                ) {
                                    if (state.isBusy) {
                                        CircularProgressIndicator(
                                            modifier = Modifier.size(18.dp),
                                            strokeWidth = 2.dp,
                                            color = MaterialTheme.colorScheme.onPrimary,
                                        )
                                        Spacer(modifier = Modifier.width(Spacing.sm))
                                    }
                                    Text("End Encounter")
                                }
                            } else {
                                Text(
                                    text = "This encounter ended at ${formatDateTime(encounter.endedAt)}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                            state.error?.let {
                                Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                            }
                        }
                    }
                }
            }
        }
    }
}
