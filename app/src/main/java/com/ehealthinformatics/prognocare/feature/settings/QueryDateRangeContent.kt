package com.ehealthinformatics.prognocare.feature.settings

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.ehealthinformatics.prognocare.data.config.AppConfig
import com.ehealthinformatics.prognocare.data.config.isValidIsoDate
import com.ehealthinformatics.prognocare.data.config.queryDateRange
import com.ehealthinformatics.prognocare.designsystem.theme.Spacing
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter

/**
 * Query date-range editor: start/end date pickers + Save.
 * When set, appointment queries use this window instead of hardcoded "today".
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QueryDateRangeContent(
    config: AppConfig,
    onSave: (start: String?, end: String?) -> Unit,
    onClear: () -> Unit,
    modifier: Modifier = Modifier,
    isSaving: Boolean = false,
    saveMessage: String? = null,
) {
    val saved = config.queryDateRange()
    var start by remember(config.queryDateStart) { mutableStateOf(config.queryDateStart) }
    var end by remember(config.queryDateEnd) { mutableStateOf(config.queryDateEnd) }
    var showStartPicker by remember { mutableStateOf(false) }
    var showEndPicker by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }

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
                text = "Query date range",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Text(
                text = "When set, appointment and dashboard queries use this window " +
                    "instead of only today's date. Leave empty to fall back to today.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(Spacing.md),
            ) {
                DatePickerField(
                    label = "Start date",
                    value = start,
                    onValueChange = {
                        start = it
                        error = null
                    },
                    onOpenPicker = { showStartPicker = true },
                    modifier = Modifier.weight(1f),
                )
                DatePickerField(
                    label = "End date",
                    value = end,
                    onValueChange = {
                        end = it
                        error = null
                    },
                    onOpenPicker = { showEndPicker = true },
                    modifier = Modifier.weight(1f),
                )
            }

            error?.let {
                Text(
                    text = it,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error,
                )
            }

            saveMessage?.let {
                Text(
                    text = it,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.primary,
                )
            }

            Text(
                text = "Active range: ${saved?.displayLabel() ?: "Not set (using today)"}",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(Spacing.md),
            ) {
                Button(
                    onClick = {
                        val s = start?.takeIf { it.isNotBlank() }
                        val e = end?.takeIf { it.isNotBlank() }
                        if (s != null && !isValidIsoDate(s)) {
                            error = "Start date must be a valid date"
                            return@Button
                        }
                        if (e != null && !isValidIsoDate(e)) {
                            error = "End date must be a valid date"
                            return@Button
                        }
                        if (s != null && e != null && s > e) {
                            error = "Start date must be on or before end date"
                            return@Button
                        }
                        error = null
                        onSave(s, e)
                    },
                    modifier = Modifier.weight(1f),
                    enabled = !isSaving,
                    shape = RoundedCornerShape(Spacing.md),
                ) {
                    Text(if (isSaving) "Saving…" else "Save range")
                }
                OutlinedButton(
                    onClick = {
                        start = null
                        end = null
                        error = null
                        onClear()
                    },
                    enabled = !isSaving,
                    shape = RoundedCornerShape(Spacing.md),
                ) {
                    Icon(
                        imageVector = Icons.Default.DeleteSweep,
                        contentDescription = null,
                        modifier = Modifier.padding(end = Spacing.xs),
                    )
                    Text("Clear")
                }
            }
        }
    }

    if (showStartPicker) {
        DateRangePickerDialog(
            title = "Start date",
            initialIso = start,
            onConfirm = { iso ->
                start = iso
                error = null
                showStartPicker = false
            },
            onDismiss = { showStartPicker = false },
        )
    }
    if (showEndPicker) {
        DateRangePickerDialog(
            title = "End date",
            initialIso = end,
            onConfirm = { iso ->
                end = iso
                error = null
                showEndPicker = false
            },
            onDismiss = { showEndPicker = false },
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DateRangePickerDialog(
    title: String,
    initialIso: String?,
    onConfirm: (String) -> Unit,
    onDismiss: () -> Unit,
) {
    val initialMillis = remember(initialIso) {
        initialIso
            ?.let {
                runCatching {
                    LocalDate.parse(it).atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli()
                }.getOrNull()
            }
            ?: System.currentTimeMillis()
    }
    val state = rememberDatePickerState(initialSelectedDateMillis = initialMillis)

    DatePickerDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(
                onClick = {
                    val millis = state.selectedDateMillis ?: return@TextButton
                    val iso = Instant.ofEpochMilli(millis)
                        .atZone(ZoneOffset.UTC)
                        .toLocalDate()
                        .format(DateTimeFormatter.ISO_LOCAL_DATE)
                    onConfirm(iso)
                },
            ) {
                Text("OK")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        },
    ) {
        Column {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(horizontal = Spacing.base, vertical = Spacing.sm),
            )
            DatePicker(state = state)
        }
    }
}

@Composable
private fun DatePickerField(
    label: String,
    value: String?,
    onValueChange: (String?) -> Unit,
    onOpenPicker: () -> Unit,
    modifier: Modifier = Modifier,
) {
    OutlinedTextField(
        value = value.orEmpty(),
        onValueChange = onValueChange,
        modifier = modifier.clickable { onOpenPicker() },
        label = { Text(label) },
        placeholder = { Text("yyyy-MM-dd") },
        singleLine = true,
        readOnly = true,
        enabled = true,
        trailingIcon = {
            IconButton(onClick = onOpenPicker) {
                Icon(
                    imageVector = Icons.Default.CalendarMonth,
                    contentDescription = "Pick $label",
                )
            }
        },
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = MaterialTheme.colorScheme.primary,
            unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant,
        ),
        shape = RoundedCornerShape(Spacing.md),
    )
}
