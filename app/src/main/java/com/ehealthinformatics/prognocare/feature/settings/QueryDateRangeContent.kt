package com.ehealthinformatics.prognocare.feature.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.ehealthinformatics.prognocare.data.config.AppConfig
import com.ehealthinformatics.prognocare.data.config.QueryRangePeriod
import com.ehealthinformatics.prognocare.data.config.resolveQueryDateRange
import com.ehealthinformatics.prognocare.designsystem.theme.Spacing

/**
 * Relative query-window dropdown: None / Today / This week / This month.
 * Dates are resolved dynamically at query time from the saved period.
 * Default is [QueryRangePeriod.NONE] (no filter — dashboards use today).
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QueryDateRangeContent(
    config: AppConfig,
    onSave: (period: QueryRangePeriod) -> Unit,
    modifier: Modifier = Modifier,
    isSaving: Boolean = false,
    saveMessage: String? = null,
) {
    var expanded by remember { mutableStateOf(false) }
    var selected by remember(config.queryRange) { mutableStateOf(config.queryRange) }

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
                text = "Query range",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Text(
                text = "Appointment and dashboard queries use this window. " +
                    "Dates are calculated dynamically. Default: no range filter.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            ExposedDropdownMenuBox(
                expanded = expanded,
                onExpandedChange = { expanded = it },
            ) {
                OutlinedTextField(
                    value = selected.label,
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Range") },
                    trailingIcon = {
                        Icon(
                            imageVector = Icons.Default.ArrowDropDown,
                            contentDescription = null,
                        )
                    },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                        unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant,
                    ),
                    shape = RoundedCornerShape(Spacing.md),
                    modifier = Modifier
                        .fillMaxWidth()
                        .menuAnchor(),
                )
                ExposedDropdownMenu(
                    expanded = expanded,
                    onDismissRequest = { expanded = false },
                ) {
                    QueryRangePeriod.entries.forEach { period ->
                        DropdownMenuItem(
                            text = { Text(period.label) },
                            onClick = {
                                selected = period
                                expanded = false
                            },
                        )
                    }
                }
            }

            saveMessage?.let {
                Text(
                    text = it,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.primary,
                )
            }

            val active = config.resolveQueryDateRange()
            Text(
                text = "Active: ${config.queryRange.label}" +
                    (active?.let { " · ${it.displayLabel()}" } ?: " · no filter"),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(Spacing.md),
            ) {
                Button(
                    onClick = { onSave(selected) },
                    modifier = Modifier.weight(1f),
                    enabled = !isSaving,
                    shape = RoundedCornerShape(Spacing.md),
                ) {
                    Text(if (isSaving) "Saving…" else "Save")
                }
                OutlinedButton(
                    onClick = {
                        selected = QueryRangePeriod.NONE
                        onSave(QueryRangePeriod.NONE)
                    },
                    enabled = !isSaving,
                    shape = RoundedCornerShape(Spacing.md),
                ) {
                    Icon(
                        imageVector = Icons.Default.DeleteSweep,
                        contentDescription = null,
                        modifier = Modifier.padding(end = Spacing.xs),
                    )
                    Text("Reset")
                }
            }
        }
    }
}
