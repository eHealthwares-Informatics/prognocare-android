package com.ehealthinformatics.prognocare.feature.forms

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ehealthinformatics.prognocare.designsystem.theme.Spacing

private fun String.titleCase(): String =
    replace("_", " ").lowercase().replaceFirstChar { it.uppercase() }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DynamicFormScreen(
    formId: String,
    patientId: String,
    visitId: String? = null,
    encounterId: String? = null,
    onBack: () -> Unit,
    onComplete: (() -> Unit)? = null,
    viewModel: FormViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    LaunchedEffect(formId) {
        viewModel.loadForm(formId)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    val title = (state as? FormUiState.Ready)?.formDefinition?.name ?: "Documentation"
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
            )
        },
    ) { innerPadding ->
        when (val s = state) {
            is FormUiState.Loading -> LoadingBox(innerPadding.calculateTopPadding())
            is FormUiState.Empty -> EmptyBox(innerPadding.calculateTopPadding())
            is FormUiState.Error -> ErrorBox(innerPadding.calculateTopPadding(), s.message, viewModel::loadForm, formId)
            is FormUiState.Submitted -> SubmittedBox(
                innerPadding.calculateTopPadding(),
                s.submission.submissionNumber,
                onBack,
            )
            is FormUiState.Ready -> FormContent(
                topPadding = innerPadding.calculateTopPadding(),
                s = s,
                onDraft = { viewModel.submit(patientId, visitId, encounterId) },
                onValueChange = viewModel::updateField,
                onAddRow = viewModel::addTableRow,
                onRemoveRow = viewModel::removeTableRow,
                onRowValue = viewModel::updateTableRow,
            )
        }
    }
}

@Composable
private fun FormContent(
    topPadding: Dp,
    s: FormUiState.Ready,
    onDraft: () -> Unit,
    onValueChange: (String, Any?) -> Unit,
    onAddRow: (FormField) -> Unit,
    onRemoveRow: (FormField, Int) -> Unit,
    onRowValue: (FormField, Int, String, Any?) -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(top = topPadding)
            .verticalScroll(rememberScrollState())
            .padding(Spacing.xl),
    ) {
        s.errors.takeIf { it.isNotEmpty() }?.let { errors ->
            Text(
                text = errors.joinToString("\n"),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.error,
            )
            Spacer(Modifier.height(Spacing.md))
        }

        FieldList(
            fields = s.schema.fields,
            data = s.data,
            enabled = !s.submitting,
            onValueChange = onValueChange,
            onAddRow = onAddRow,
            onRemoveRow = onRemoveRow,
            onRowValue = onRowValue,
        )

        Spacer(Modifier.height(Spacing.xl))

        Button(
            onClick = onDraft,
            modifier = Modifier.fillMaxWidth().height(52.dp),
            enabled = !s.submitting,
            shape = RoundedCornerShape(Spacing.md),
        ) {
            if (s.submitting) {
                CircularProgressIndicator(modifier = Modifier.width(22.dp).height(22.dp), color = MaterialTheme.colorScheme.onPrimary)
            } else {
                Text("Submit", fontWeight = FontWeight.SemiBold)
            }
        }
        Spacer(Modifier.height(Spacing.xl))
    }
}

@Composable
private fun FieldList(
    fields: List<FormField>,
    data: FormData,
    enabled: Boolean,
    onValueChange: (String, Any?) -> Unit,
    onAddRow: (FormField) -> Unit,
    onRemoveRow: (FormField, Int) -> Unit,
    onRowValue: (FormField, Int, String, Any?) -> Unit,
) {
    // Column arrangement: consecutive simple fields (text/number/select/etc.)
    // are chunked into two-per-row rows; structural fields (sections, tabs,
    // columns, tables, textareas) always get the full width.
    var index = 0
    while (index < fields.size) {
        val field = fields[index]
        when (field.type) {
            FormFieldType.SECTION -> {
                HorizontalDivider(modifier = Modifier.padding(vertical = Spacing.md))
                Text(
                    text = field.label,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.primary,
                )
                Spacer(Modifier.height(Spacing.sm))
                index += 1
            }
            FormFieldType.TAB -> {
                TabFieldList(
                    field = field,
                    data = data,
                    enabled = enabled,
                    onValueChange = onValueChange,
                    onAddRow = onAddRow,
                    onRemoveRow = onRemoveRow,
                    onRowValue = onRowValue,
                )
                index += 1
            }
            FormFieldType.COL -> {
                Row(horizontalArrangement = Arrangement.spacedBy(Spacing.md)) {
                    field.fields.forEach { child ->
                        Column(Modifier.weight(1f)) {
                            SingleField(
                                field = child,
                                value = data[child.key],
                                enabled = enabled,
                                onValueChange = { onValueChange(child.key, it) },
                            )
                        }
                    }
                }
                Spacer(Modifier.height(Spacing.md))
                index += 1
            }
            FormFieldType.TABLE, FormFieldType.TEXTAREA -> {
                TableOrTextarea(
                    field = field,
                    data = data,
                    enabled = enabled,
                    onValueChange = onValueChange,
                    onAddRow = onAddRow,
                    onRemoveRow = onRemoveRow,
                    onRowValue = onRowValue,
                )
                index += 1
            }
            else -> {
                // Pair consecutive simple fields into a 2-column row.
                val isSimple: (FormField) -> Boolean = {
                    it.type != FormFieldType.SECTION &&
                        it.type != FormFieldType.TAB &&
                        it.type != FormFieldType.COL &&
                        it.type != FormFieldType.TABLE &&
                        it.type != FormFieldType.TEXTAREA
                }
                val pair = buildList {
                    add(field)
                    var next = index + 1
                    while (next < fields.size && size < 2 && isSimple(fields[next])) {
                        add(fields[next])
                        next += 1
                    }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(Spacing.md)) {
                    pair.forEach { f ->
                        Column(Modifier.weight(1f)) {
                            TableOrTextarea(
                                field = f,
                                data = data,
                                enabled = enabled,
                                onValueChange = onValueChange,
                                onAddRow = onAddRow,
                                onRemoveRow = onRemoveRow,
                                onRowValue = onRowValue,
                            )
                        }
                    }
                    // Keep widths stable when a row has only one field.
                    if (pair.size == 1) {
                        androidx.compose.foundation.layout.Box(Modifier.weight(1f)) {}
                    }
                }
                Spacer(Modifier.height(Spacing.md))
                index += pair.size
            }
        }
    }
}

@Composable
private fun TableOrTextarea(
    field: FormField,
    data: FormData,
    enabled: Boolean,
    onValueChange: (String, Any?) -> Unit,
    onAddRow: (FormField) -> Unit,
    onRemoveRow: (FormField, Int) -> Unit,
    onRowValue: (FormField, Int, String, Any?) -> Unit,
) {
    when (field.type) {
        FormFieldType.TABLE -> TableField(
            field = field,
            rows = data[field.key] as? List<*> ?: emptyList<Any>(),
            enabled = enabled,
            onAddRow = { onAddRow(field) },
            onRemoveRow = { idx -> onRemoveRow(field, idx) },
            onCellValue = { r, c, v -> onRowValue(field, r, c, v) },
        )
        else -> {
            SingleField(
                field = field,
                value = data[field.key],
                enabled = enabled,
                onValueChange = { onValueChange(field.key, it) },
            )
            Spacer(Modifier.height(Spacing.md))
        }
    }
}

@Composable
private fun TabFieldList(
    field: FormField,
    data: FormData,
    enabled: Boolean,
    onValueChange: (String, Any?) -> Unit,
    onAddRow: (FormField) -> Unit,
    onRemoveRow: (FormField, Int) -> Unit,
    onRowValue: (FormField, Int, String, Any?) -> Unit,
) {
    val children = field.fields
    if (children.isEmpty()) return
    var selected by rememberSaveable { mutableStateOf(children.first().key) }
    val selectedIndex = children.indexOfFirst { it.key == selected }.coerceAtLeast(0)

    TabRow(selectedTabIndex = selectedIndex) {
        children.forEach { child ->
            androidx.compose.material3.Tab(
                selected = child.key == selected,
                onClick = { selected = child.key },
                text = { Text(child.label, maxLines = 1) },
            )
        }
    }
    Spacer(Modifier.height(Spacing.md))
    val active = children.getOrNull(selectedIndex)
    if (active != null) {
        FieldList(
            fields = active.fields,
            data = data,
            enabled = enabled,
            onValueChange = onValueChange,
            onAddRow = onAddRow,
            onRemoveRow = onRemoveRow,
            onRowValue = onRowValue,
        )
    }
}

@Composable
private fun SingleField(
    field: FormField,
    value: Any?,
    enabled: Boolean,
    onValueChange: (Any?) -> Unit,
) {
    when (field.type) {
        FormFieldType.TEXT, FormFieldType.DATE, FormFieldType.DATETIME -> {
            OutlinedTextField(
                value = value?.toString() ?: "",
                onValueChange = { onValueChange(it.ifEmpty { null }) },
                modifier = Modifier.fillMaxWidth(),
                label = { Text(field.label) },
                placeholder = field.placeholder?.let { { Text(it) } },
                singleLine = true,
                enabled = enabled,
                shape = RoundedCornerShape(Spacing.md),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                    unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant,
                ),
            )
        }
        FormFieldType.TEXTAREA -> {
            OutlinedTextField(
                value = value?.toString() ?: "",
                onValueChange = { onValueChange(it.ifEmpty { null }) },
                modifier = Modifier.fillMaxWidth().heightIn(min = 96.dp),
                label = { Text(field.label) },
                placeholder = field.placeholder?.let { { Text(it) } },
                minLines = field.rows.coerceAtLeast(2),
                enabled = enabled,
                shape = RoundedCornerShape(Spacing.md),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                    unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant,
                ),
            )
        }
        FormFieldType.NUMBER -> {
            OutlinedTextField(
                value = value?.toString() ?: "",
                onValueChange = { raw ->
                    val trimmed = raw.trim()
                    onValueChange(if (trimmed.isEmpty()) null else trimmed.toLongOrNull() ?: trimmed.toDoubleOrNull())
                },
                modifier = Modifier.fillMaxWidth(),
                label = { Text(field.label) },
                singleLine = true,
                enabled = enabled,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                shape = RoundedCornerShape(Spacing.md),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                    unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant,
                ),
            )
        }
        FormFieldType.SELECT -> {
            Text(
                text = field.label,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            if (field.options.isEmpty()) {
                OutlinedTextField(
                    value = value?.toString() ?: "",
                    onValueChange = { onValueChange(it.ifEmpty { null }) },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text(field.label) },
                    enabled = enabled,
                    shape = RoundedCornerShape(Spacing.md),
                )
            } else {
                Row(horizontalArrangement = Arrangement.spacedBy(Spacing.sm)) {
                    field.options.forEach { option ->
                        val selected = value?.toString() == option
                        FilterChip(
                            selected = selected,
                            onClick = { onValueChange(if (!selected) option else null) },
                            label = { Text(option.titleCase()) },
                            enabled = enabled,
                        )
                    }
                }
            }
        }
        FormFieldType.RADIO -> {
            Text(
                text = field.label,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Column {
                field.options.forEach { option ->
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        RadioButton(
                            selected = value?.toString() == option,
                            onClick = { onValueChange(option) },
                            enabled = enabled,
                        )
                        Spacer(Modifier.width(Spacing.xs))
                        Text(option.titleCase(), style = MaterialTheme.typography.bodyMedium)
                    }
                }
            }
        }
        FormFieldType.CHECKBOX -> {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Checkbox(
                    checked = value == true,
                    onCheckedChange = { onValueChange(it) },
                    enabled = enabled,
                )
                Spacer(Modifier.width(Spacing.xs))
                Text(field.label, style = MaterialTheme.typography.bodyMedium)
            }
        }
        FormFieldType.CHECKBOX_GROUP -> {
            Text(
                text = field.label,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Column {
                val current = (value as? List<*>)?.map { it.toString() } ?: emptyList()
                field.options.forEach { option ->
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Checkbox(
                            checked = option in current,
                            onCheckedChange = { checked ->
                                val next = if (checked) current + option else current - option
                                onValueChange(next)
                            },
                            enabled = enabled,
                        )
                        Spacer(Modifier.width(Spacing.xs))
                        Text(option.titleCase(), style = MaterialTheme.typography.bodyMedium)
                    }
                }
            }
        }
        else -> Unit
    }
}

@Composable
private fun TableField(
    field: FormField,
    rows: List<*>,
    enabled: Boolean,
    onAddRow: () -> Unit,
    onRemoveRow: (Int) -> Unit,
    onCellValue: (Int, String, Any?) -> Unit,
) {
    Column {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = field.label,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            OutlinedButton(
                onClick = onAddRow,
                enabled = enabled,
            ) {
                Icon(
                    Icons.Filled.Add,
                    contentDescription = null,
                    modifier = Modifier.width(16.dp).height(16.dp),
                )
                Spacer(Modifier.width(Spacing.xs))
                Text("Add row")
            }
        }
        Spacer(Modifier.height(Spacing.sm))

        rows.forEachIndexed { rowIndex, rawRow ->
            val rowMap = (rawRow as? Map<*, *>)?.mapKeys { it.key.toString() } ?: emptyMap()
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
                verticalAlignment = Alignment.Top,
            ) {
                field.columns.forEach { column ->
                    Column(Modifier.weight(1f)) {
                        Text(
                            text = column.label.titleCase(),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        when (column.type) {
                            FormFieldType.NUMBER -> OutlinedTextField(
                                value = rowMap[column.key]?.toString() ?: "",
                                onValueChange = { raw ->
                                    val t = raw.trim()
                                    onCellValue(rowIndex, column.key, if (t.isEmpty()) null else t.toLongOrNull() ?: t.toDoubleOrNull())
                                },
                                singleLine = true,
                                enabled = enabled,
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                shape = RoundedCornerShape(Spacing.sm),
                            )
                            FormFieldType.CHECKBOX -> Checkbox(
                                checked = rowMap[column.key] == true,
                                onCheckedChange = { onCellValue(rowIndex, column.key, it) },
                                enabled = enabled,
                            )
                            else -> OutlinedTextField(
                                value = rowMap[column.key]?.toString() ?: "",
                                onValueChange = { onCellValue(rowIndex, column.key, it.ifEmpty { null }) },
                                singleLine = true,
                                enabled = enabled,
                                shape = RoundedCornerShape(Spacing.sm),
                            )
                        }
                    }
                }
                IconButton(onClick = { onRemoveRow(rowIndex) }, enabled = enabled) {
                    Icon(
                        Icons.Filled.Delete,
                        contentDescription = "Remove row",
                        tint = MaterialTheme.colorScheme.error,
                    )
                }
            }
            Spacer(Modifier.height(Spacing.sm))
        }

        if (rows.isEmpty()) {
            Text(
                text = "No rows yet.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun LoadingBox(topPadding: Dp) {
    Column(
        modifier = Modifier.fillMaxSize().padding(top = topPadding),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        CircularProgressIndicator()
    }
}

@Composable
private fun EmptyBox(topPadding: Dp) {
    Column(
        modifier = Modifier.fillMaxSize().padding(top = topPadding),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text("Select a form to begin", style = MaterialTheme.typography.bodyLarge)
    }
}

@Composable
private fun ErrorBox(topPadding: Dp, message: String?, reload: (String) -> Unit, formId: String) {
    Column(
        modifier = Modifier.fillMaxSize().padding(top = topPadding),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text(
            text = message ?: "Could not load form",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.error,
        )
        Spacer(Modifier.height(Spacing.md))
        Button(onClick = { reload(formId) }) { Text("Retry") }
    }
}

@Composable
private fun SubmittedBox(topPadding: Dp, submissionNumber: String?, onBack: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize().padding(top = topPadding).padding(Spacing.xl),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text(
            text = "Form submitted",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
        )
        Spacer(Modifier.height(Spacing.sm))
        Text(
            text = "Submission ${submissionNumber ?: ""}".trim(),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(Spacing.xl))
        Button(onClick = onBack) { Text("Done") }
    }
}
