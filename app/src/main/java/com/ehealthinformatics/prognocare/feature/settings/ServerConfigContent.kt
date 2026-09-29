package com.ehealthinformatics.prognocare.feature.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Error
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.ehealthinformatics.prognocare.data.config.AppConfig
import com.ehealthinformatics.prognocare.data.config.ConnectionCheck
import com.ehealthinformatics.prognocare.designsystem.theme.Spacing

/**
 * Shared server-config editor used by the hidden 7-tap panel on the login
 * screen and the full SettingsScreen. Saves the three URL/channel fields.
 */
@Composable
fun ServerConfigContent(
    config: AppConfig,
    onSave: (emrBaseUrl: String, conversationBaseUrl: String, webChannelCode: String) -> Unit,
    onReset: () -> Unit,
    modifier: Modifier = Modifier,
    isVerifying: Boolean = false,
    saveResult: SaveResult? = null,
) {
    var emrUrl by rememberSaveable { mutableStateOf(config.emrBaseUrl) }
    var conversationUrl by rememberSaveable { mutableStateOf(config.conversationBaseUrl) }
    var channelCode by rememberSaveable { mutableStateOf(config.webChannelCode) }

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
            Text(
                text = "Server configuration",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Text(
                text = "Point the app at your EMR and Conversation Engine endpoints.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            OutlinedTextField(
                value = emrUrl,
                onValueChange = { emrUrl = it },
                modifier = Modifier.fillMaxWidth(),
                label = { Text("EMR base URL") },
                singleLine = true,
                shape = RoundedCornerShape(Spacing.md),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                    unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant,
                ),
            )

            OutlinedTextField(
                value = conversationUrl,
                onValueChange = { conversationUrl = it },
                modifier = Modifier.fillMaxWidth(),
                label = { Text("Conversation engine base URL") },
                singleLine = true,
                shape = RoundedCornerShape(Spacing.md),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                    unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant,
                ),
            )

            OutlinedTextField(
                value = channelCode,
                onValueChange = { channelCode = it },
                modifier = Modifier.fillMaxWidth(),
                label = { Text("Messaging channel code") },
                singleLine = true,
                shape = RoundedCornerShape(Spacing.md),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                    unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant,
                ),
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(Spacing.md),
            ) {
                Button(
                    onClick = { onSave(emrUrl, conversationUrl, channelCode) },
                    modifier = Modifier.weight(1f),
                    enabled = !isVerifying,
                    shape = RoundedCornerShape(Spacing.md),
                ) {
                    if (isVerifying) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(16.dp),
                            color = Color.White,
                            strokeWidth = 2.dp,
                        )
                        Spacer(modifier = Modifier.size(Spacing.sm))
                        Text("Checking…")
                    } else {
                        Text("Save")
                    }
                }
                TextButton(
                    onClick = onReset,
                    modifier = Modifier.weight(0.6f),
                    enabled = !isVerifying,
                ) {
                    Text("Reset")
                }
            }

            saveResult?.let { result ->
                ConnectionCheckList(checks = result.checks)
                Text(
                    text = if (result.saved) {
                        "Saved. Clients are using the new endpoints."
                    } else {
                        "Not saved. Fix the unreachable endpoints and try again."
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = if (result.saved) MaterialTheme.colorScheme.primary
                    else MaterialTheme.colorScheme.error,
                )
            }

            Spacer(modifier = Modifier.height(Spacing.xs))
            Text(
                text = "Saved URLs apply immediately to the app's network clients.",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun ConnectionCheckList(checks: List<ConnectionCheck>) {
    Column(verticalArrangement = Arrangement.spacedBy(Spacing.xs)) {
        checks.forEach { check ->
            Row(verticalAlignment = Alignment.CenterVertically) {
                when (check) {
                    is ConnectionCheck.Success -> {
                        Icon(
                            imageVector = Icons.Filled.CheckCircle,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(18.dp),
                        )
                    }
                    is ConnectionCheck.Failure -> {
                        Icon(
                            imageVector = Icons.Filled.Error,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.error,
                            modifier = Modifier.size(18.dp),
                        )
                    }
                }
                Spacer(modifier = Modifier.size(Spacing.sm))
                Column {
                    Text(
                        text = check.label,
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                    if (check is ConnectionCheck.Failure) {
                        Text(
                            text = check.detail,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
        }
    }
}