package com.ehealthwares.prognocare.feature.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.BugReport
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.ehealthwares.prognocare.designsystem.theme.Spacing

/**
 * Diagnostics card — exposes a manual Sentry test trigger so crash/error
 * reporting can be verified from Settings (previously a debug-only FAB that
 * floated over the whole app).
 */
@Composable
fun DiagnosticsContent(
    onSendTestEvent: () -> Unit,
    modifier: Modifier = Modifier,
    pendingCount: Int = 0,
    cachedPatientCount: Int = 0,
    isOnline: Boolean = true,
    isSyncing: Boolean = false,
    onSync: () -> Unit = {},
) {
    var sent by remember { mutableStateOf(false) }

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(Spacing.md),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(Spacing.base),
            verticalArrangement = Arrangement.spacedBy(Spacing.md),
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(
                    imageVector = Icons.Outlined.BugReport,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(20.dp),
                )
                Text(
                    text = "Diagnostics",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                )
            }

            // ── Offline mode ─────────────────────────────────────
            Text(
                text = "Offline mode",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Text(
                text = (if (isOnline) "Online" else "Offline") +
                    " · $pendingCount pending write${if (pendingCount == 1) "" else "s"}" +
                    " · $cachedPatientCount patient${if (cachedPatientCount == 1) "" else "s"} cached",
                style = MaterialTheme.typography.bodySmall,
                color = if (isOnline) {
                    MaterialTheme.colorScheme.onSurfaceVariant
                } else {
                    MaterialTheme.colorScheme.error
                },
            )
            Text(
                text = "Patients and appointments/documents created offline are stored on " +
                    "this device and synced automatically when the connection returns.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            OutlinedButton(
                onClick = onSync,
                enabled = isOnline && !isSyncing,
                shape = RoundedCornerShape(Spacing.md),
            ) {
                Text(if (isSyncing) "Syncing…" else "Sync now")
            }

            HorizontalDivider()

            Text(
                text = "Send a test event to Sentry to verify that error " +
                    "reporting is working on this device.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Button(
                onClick = {
                    onSendTestEvent()
                    sent = true
                },
                shape = RoundedCornerShape(Spacing.md),
            ) {
                Text("Send test event to Sentry")
            }
            if (sent) {
                Text(
                    text = "Test event sent.",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary,
                )
            }
        }
    }
}
