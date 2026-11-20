package com.ehealthwares.prognocare.feature.forms

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
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
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
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
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
import com.ehealthwares.prognocare.designsystem.theme.Spacing
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset

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
    lookupViewModel: FormLookupViewModel = hiltViewModel(),
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
            is FormUiState.SubmittedOffline -> OfflineSubmittedBox(
                innerPadding.calculateTopPadding(),
                onBack,
            )
            is FormUiState.Ready -> FormContent(
                topPadding = innerPadding.calculateTopPadding(),
                s = s,
                lookup = lookupViewModel.repository,
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
    lookup: FormLookupRepository,
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
            lookup = lookup,
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
    lookup: FormLookupRepository,
    onValueChange: (String, Any?) -> Unit,
    onAddRow: (FormField) -> Unit,
    onRemoveRow: (FormField, Int) -> Unit,
    onRowValue: (FormField, Int, String, Any?) -> Unit,
) {
    // Column arrangement: consecutive simple fields (text/number/select/etc.)
    // are chunked into two-per-row rows; structural and lookup fields always
    // get the full width.
    val fullWidthTypes = setOf(
        FormFieldType.TABLE, FormFieldType.TEXTAREA, FormFieldType.ANNOTATE,
        FormFieldType.PATIENT, FormFieldType.AUTH_PATIENT, FormFieldType.PRACTITIONER,
        FormFieldType.DEPARTMENT, FormFieldType.FILE,
        FormFieldType.ITEM, FormFieldType.INVENTORY_ITEM,
        FormFieldType.GENERIC_PRODUCT, FormFieldType.GENERIC_DRUG,
        FormFieldType.LOINC_TEST, FormFieldType.DICOM_TEST,
    )
    val hiddenTypes = setOf(
        FormFieldType.QR_CODE, FormFieldType.PRINT, FormFieldType.SEND_FORM,
    )

    var index = 0
    while (index < fields.size) {
        val field = fields[index]
        when {
            field.type in hiddenTypes -> {
                // Action widgets with no mobile equivalent (print / send / QR).
                index += 1
            }
            field.type == FormFieldType.SECTION -> {
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
            field.type == FormFieldType.TAB -> {
                TabFieldList(
                    field = field,
                    data = data,
                    enabled = enabled,
                    lookup = lookup,
                    onValueChange = onValueChange,
                    onAddRow = onAddRow,
                    onRemoveRow = onRemoveRow,
                    onRowValue = onRowValue,
                )
                index += 1
            }
            field.type == FormFieldType.COL -> {
                Row(horizontalArrangement = Arrangement.spacedBy(Spacing.md)) {
                    field.fields.forEach { child ->
                        Column(Modifier.weight(1f)) {
                            SingleField(
                                field = child,
                                value = data[child.key],
                                enabled = enabled,
                                lookup = lookup,
                                onValueChange = { onValueChange(child.key, it) },
                            )
                        }
                    }
                }
                Spacer(Modifier.height(Spacing.md))
                index += 1
            }
            field.type == FormFieldType.HTML || field.type == FormFieldType.HIDDEN -> {
                SingleField(
                    field = field,
                    value = data[field.key],
                    enabled = enabled,
                    lookup = lookup,
                    onValueChange = { onValueChange(field.key, it) },
                )
                index += 1
            }
            field.type in fullWidthTypes -> {
                TableOrTextarea(
                    field = field,
                    data = data,
                    enabled = enabled,
                    lookup = lookup,
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
                    it.type !in fullWidthTypes &&
                        it.type !in hiddenTypes &&
                        it.type != FormFieldType.SECTION &&
                        it.type != FormFieldType.TAB &&
                        it.type != FormFieldType.COL &&
                        it.type != FormFieldType.HTML &&
                        it.type != FormFieldType.HIDDEN
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
                                lookup = lookup,
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
    lookup: FormLookupRepository,
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
                lookup = lookup,
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
    lookup: FormLookupRepository,
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
            lookup = lookup,
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
    lookup: FormLookupRepository,
    onValueChange: (Any?) -> Unit,
) {
    when (field.type) {
        FormFieldType.TEXT -> {
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
        FormFieldType.DATE, FormFieldType.DATETIME -> {
            DateField(
                field = field,
                value = value?.toString().orEmpty(),
                withTime = field.type == FormFieldType.DATETIME,
                enabled = enabled,
                onValueChange = { onValueChange(it.ifEmpty { null }) },
            )
        }
        FormFieldType.ANNOTATE -> {
            AnnotateField(
                field = field,
                value = value,
                enabled = enabled,
                onValueChange = onValueChange,
            )
        }
        FormFieldType.HTML -> {
            field.html?.takeIf { it.isNotBlank() }?.let { html ->
                Text(
                    text = html.replace(Regex("<[^>]*>"), "").trim(),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }
        FormFieldType.HIDDEN -> Unit
        FormFieldType.PATIENT, FormFieldType.AUTH_PATIENT -> {
            PatientLookupField(
                field = field,
                value = value,
                enabled = enabled,
                lookup = lookup,
                onValueChange = onValueChange,
            )
        }
        FormFieldType.PRACTITIONER -> {
            PractitionerLookupField(
                field = field,
                value = value,
                enabled = enabled,
                lookup = lookup,
                onValueChange = onValueChange,
            )
        }
        FormFieldType.DEPARTMENT -> {
            DepartmentLookupField(
                field = field,
                value = value,
                enabled = enabled,
                lookup = lookup,
                onValueChange = onValueChange,
            )
        }
        FormFieldType.FILE -> {
            FileField(
                field = field,
                value = value,
                enabled = enabled,
                lookup = lookup,
                onValueChange = onValueChange,
            )
        }
        FormFieldType.ITEM, FormFieldType.INVENTORY_ITEM -> {
            MasterItemField(
                field = field,
                value = value,
                enabled = enabled,
                lookup = lookup,
                kind = "STOCK_ITEM",
                onValueChange = onValueChange,
            )
        }
        FormFieldType.GENERIC_PRODUCT -> {
            MasterItemField(
                field = field,
                value = value,
                enabled = enabled,
                lookup = lookup,
                kind = "GENERIC_PRODUCT",
                onValueChange = onValueChange,
            )
        }
        FormFieldType.GENERIC_DRUG -> {
            MasterItemField(
                field = field,
                value = value,
                enabled = enabled,
                lookup = lookup,
                kind = "GENERIC_DRUG",
                onValueChange = onValueChange,
            )
        }
        FormFieldType.LOINC_TEST -> {
            ConceptField(
                field = field,
                value = value,
                enabled = enabled,
                lookup = lookup,
                concept = "LOINC",
                onValueChange = onValueChange,
            )
        }
        FormFieldType.DICOM_TEST -> {
            ConceptField(
                field = field,
                value = value,
                enabled = enabled,
                lookup = lookup,
                concept = "DICOM",
                onValueChange = onValueChange,
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
            SelectField(
                field = field,
                value = value?.toString(),
                enabled = enabled,
                onValueChange = onValueChange,
            )
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SelectField(
    field: FormField,
    value: String?,
    enabled: Boolean,
    onValueChange: (Any?) -> Unit,
) {
    var expanded by remember { mutableStateOf(false) }

    if (field.options.isEmpty()) {
        // No option list — fall back to free text.
        OutlinedTextField(
            value = value.orEmpty(),
            onValueChange = { onValueChange(it.ifEmpty { null }) },
            modifier = Modifier.fillMaxWidth(),
            label = { Text(field.label) },
            enabled = enabled,
            shape = RoundedCornerShape(Spacing.md),
        )
        return
    }

    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { if (enabled) expanded = it },
    ) {
        OutlinedTextField(
            value = value?.titleCase().orEmpty(),
            onValueChange = {},
            readOnly = true,
            singleLine = true,
            enabled = enabled,
            label = { Text(field.label) },
            placeholder = field.placeholder?.let { { Text(it) } },
            trailingIcon = {
                ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded)
            },
            shape = RoundedCornerShape(Spacing.md),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = MaterialTheme.colorScheme.primary,
                unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant,
            ),
            modifier = Modifier
                .fillMaxWidth()
                .menuAnchor(),
        )
        ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            DropdownMenuItem(
                text = { Text("—") },
                onClick = {
                    onValueChange(null)
                    expanded = false
                },
            )
            field.options.forEach { option ->
                DropdownMenuItem(
                    text = { Text(option.titleCase()) },
                    onClick = {
                        onValueChange(option)
                        expanded = false
                    },
                )
            }
        }
    }
}

/**
 * Date (and optional time) picker field. Writes `yyyy-MM-dd` for DATE and
 * `yyyy-MM-ddTHH:mm` for DATETIME — the formats the backend/validator expect.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DateField(
    field: FormField,
    value: String,
    withTime: Boolean,
    enabled: Boolean,
    onValueChange: (String) -> Unit,
) {
    var showDate by remember { mutableStateOf(false) }
    var showTime by remember { mutableStateOf(false) }
    var pendingDate by remember { mutableStateOf(value.substringBefore("T").takeIf { it.length == 10 }) }

    val datePickerState = rememberDatePickerState(
        initialSelectedDateMillis = value.substringBefore("T").toEpochMillisOrNull(),
    )

    OutlinedTextField(
        value = value.replace('T', ' '),
        onValueChange = {},
        readOnly = true,
        singleLine = true,
        enabled = enabled,
        label = { Text(field.label) },
        placeholder = { Text(if (withTime) "yyyy-MM-dd HH:mm" else "yyyy-MM-dd") },
        trailingIcon = {
            IconButton(onClick = { if (enabled) showDate = true }) {
                Icon(Icons.Filled.CalendarMonth, contentDescription = "Pick date")
            }
        },
        shape = RoundedCornerShape(Spacing.md),
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = MaterialTheme.colorScheme.primary,
            unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant,
        ),
        modifier = Modifier.fillMaxWidth(),
    )

    if (showDate) {
        DatePickerDialog(
            onDismissRequest = { showDate = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        val date = datePickerState.selectedDateMillis?.toLocalDateString()
                        if (date == null) {
                            showDate = false
                        } else if (withTime) {
                            pendingDate = date
                            showDate = false
                            showTime = true
                        } else {
                            onValueChange(date)
                            showDate = false
                        }
                    },
                ) { Text("OK") }
            },
            dismissButton = {
                TextButton(onClick = { showDate = false }) { Text("Cancel") }
            },
        ) {
            DatePicker(state = datePickerState)
        }
    }

    if (showTime) {
        val timePickerState = rememberTimePickerState(
            initialHour = value.substringAfter("T", "09:00").substringBefore(":").toIntOrNull() ?: 9,
            initialMinute = value.substringAfter("T", "09:00").substringAfter(":", "00").take(2).toIntOrNull() ?: 0,
            is24Hour = true,
        )
        AlertDialog(
            onDismissRequest = { showTime = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        val hh = timePickerState.hour.toString().padStart(2, '0')
                        val mm = timePickerState.minute.toString().padStart(2, '0')
                        onValueChange("${pendingDate.orEmpty()}T$hh:$mm")
                        showTime = false
                    },
                ) { Text("OK") }
            },
            dismissButton = {
                TextButton(onClick = { showTime = false }) { Text("Cancel") }
            },
            text = { TimePicker(state = timePickerState) },
        )
    }
}

private fun String.toEpochMillisOrNull(): Long? = runCatching {
    LocalDate.parse(this).atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli()
}.getOrNull()

private fun Long.toLocalDateString(): String =
    Instant.ofEpochMilli(this).atZone(ZoneOffset.UTC).toLocalDate().toString()

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
private fun OfflineSubmittedBox(topPadding: Dp, onBack: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize().padding(top = topPadding).padding(Spacing.xl),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text(
            text = "Saved offline",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
        )
        Spacer(Modifier.height(Spacing.sm))
        Text(
            text = "This document is stored on this device and will be submitted " +
                "automatically when the connection returns.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
        )
        Spacer(Modifier.height(Spacing.xl))
        Button(onClick = onBack) { Text("Done") }
    }
}

@Composable
private fun SubmittedBox(topPadding: Dp, submissionNumber: String?, onBack: () -> Unit) {    Column(
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
