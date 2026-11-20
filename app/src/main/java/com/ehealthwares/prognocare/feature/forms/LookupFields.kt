package com.ehealthwares.prognocare.feature.forms

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.ehealthwares.prognocare.designsystem.theme.Spacing
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * Patient search picker for the `patient` / `auth-patient` field types. Stores
 * `{ id, patientId, patientName }` — the same shape as the web `PatientOption`.
 */
@Composable
fun PatientLookupField(
    field: FormField,
    value: Any?,
    enabled: Boolean,
    lookup: FormLookupRepository,
    onValueChange: (Any?) -> Unit,
) {
    SearchPickerField(
        label = field.label,
        required = field.required,
        placeholder = field.placeholder ?: "Search by name or MRN",
        selectedLabel = selectedDisplay(value, fallbackKey = "patientName", idKey = "patientId"),
        enabled = enabled,
        search = { query ->
            lookup.searchPatients(query).map { patient ->
                PickerOption(
                    key = patient.id,
                    label = listOf(patient.patientId, patient.displayName)
                        .filter { it.isNotBlank() }
                        .joinToString(" · "),
                    value = mapOf<String, Any?>(
                        "id" to patient.patientId.ifBlank { patient.id },
                        "patientId" to patient.patientId,
                        "patientName" to patient.displayName,
                    ),
                )
            }
        },
        onValueChange = onValueChange,
    )
}

/** Staff search picker for the `practitioner` field type. */
@Composable
fun PractitionerLookupField(
    field: FormField,
    value: Any?,
    enabled: Boolean,
    lookup: FormLookupRepository,
    onValueChange: (Any?) -> Unit,
) {
    SearchPickerField(
        label = field.label,
        required = field.required,
        placeholder = field.placeholder ?: "Search practitioner",
        selectedLabel = selectedDisplay(value, fallbackKey = "staffName", idKey = "staffId"),
        enabled = enabled,
        search = { query ->
            lookup.searchStaff(query).map { staff ->
                PickerOption(
                    key = staff.id,
                    label = staff.displayWithDepartment.ifBlank { staff.staffNumber },
                    value = mapOf<String, Any?>(
                        "id" to staff.id,
                        "staffId" to staff.staffNumber,
                        "staffName" to staff.displayName,
                        "roleType" to staff.roleType,
                    ),
                )
            }
        },
        onValueChange = onValueChange,
    )
}

/** Department picker for the `department` field type. */
@Composable
fun DepartmentLookupField(
    field: FormField,
    value: Any?,
    enabled: Boolean,
    lookup: FormLookupRepository,
    onValueChange: (Any?) -> Unit,
) {
    var departments by remember { mutableStateOf<List<com.ehealthwares.prognocare.data.remote.models.Department>>(emptyList()) }

    LaunchedEffect(Unit) {
        departments = runCatching { lookup.departments() }.getOrDefault(emptyList())
    }

    SearchPickerField(
        label = field.label,
        required = field.required,
        placeholder = field.placeholder ?: "Select department",
        selectedLabel = selectedDisplay(value, fallbackKey = "name", idKey = "code"),
        enabled = enabled,
        search = { query ->
            departments
                .filter {
                    query.isBlank() ||
                        it.name.contains(query, ignoreCase = true) ||
                        it.code.contains(query, ignoreCase = true)
                }
                .map { department ->
                    PickerOption(
                        key = department.id,
                        label = department.displayName,
                        value = department.toFieldValue(),
                    )
                }
        },
        onValueChange = onValueChange,
    )
}

private data class PickerOption(
    val key: String,
    val label: String,
    val value: Map<String, Any?>,
)

/** LOINC/DICOM picker backed by the concepts service (free text allowed). */
@Composable
fun ConceptField(
    field: FormField,
    value: Any?,
    enabled: Boolean,
    lookup: FormLookupRepository,
    concept: String,
    onValueChange: (Any?) -> Unit,
) {
    SearchPickerField(
        label = field.label,
        required = field.required,
        placeholder = field.placeholder ?: "Search $concept or type a value",
        selectedLabel = selectedDisplay(value, fallbackKey = "name", idKey = "code"),
        enabled = enabled,
        search = { query ->
            lookup.concepts(concept, query).map { row ->
                PickerOption(
                    key = row.id,
                    label = listOfNotNull(row.name, row.code?.let { "($it)" }).joinToString(" "),
                    value = mapOf<String, Any?>(
                        "id" to row.id,
                        "code" to row.code,
                        "name" to row.name,
                        "source" to "catalog",
                    ),
                )
            }
        },
        // Free text is stored as a concept-shaped value (mirrors the web).
        freeText = { text ->
            mapOf<String, Any?>(
                "id" to "free-text-$text",
                "code" to null,
                "name" to text,
                "source" to "free-text",
            )
        },
        onValueChange = onValueChange,
    )
}

/**
 * Master-catalogue picker for the item field types. [kind] is one of
 * `STOCK_ITEM`, `GENERIC_PRODUCT`, `GENERIC_DRUG`.
 */
@Composable
fun MasterItemField(
    field: FormField,
    value: Any?,
    enabled: Boolean,
    lookup: FormLookupRepository,
    kind: String,
    onValueChange: (Any?) -> Unit,
) {
    SearchPickerField(
        label = field.label,
        required = field.required,
        placeholder = field.placeholder ?: "Search catalogue",
        selectedLabel = selectedDisplay(value, fallbackKey = "label", idKey = "code"),
        enabled = enabled,
        search = { query ->
            lookup.catalogue(kind, query).map { row ->
                val label = listOfNotNull(row.name, row.code?.let { "($it)" }).joinToString(" ")
                PickerOption(
                    key = row.id,
                    label = label,
                    value = mapOf<String, Any?>(
                        "id" to row.id,
                        "label" to row.name,
                        "code" to row.code,
                        "kind" to kind,
                        "referenceCode" to "$kind:${row.code ?: row.id}",
                    ),
                )
            }
        },
        onValueChange = onValueChange,
    )
}

@Composable
private fun SearchPickerField(
    label: String,
    required: Boolean,
    placeholder: String,
    selectedLabel: String?,
    enabled: Boolean,
    search: suspend (String) -> List<PickerOption>,
    onValueChange: (Any?) -> Unit,
    freeText: ((String) -> Map<String, Any?>)? = null,
) {
    var query by remember { mutableStateOf("") }
    var results by remember { mutableStateOf<List<PickerOption>>(emptyList()) }
    var loading by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(query) {
        if (selectedLabel != null) return@LaunchedEffect
        loading = true
        error = null
        delay(250)
        runCatching { search(query) }
            .onSuccess { results = it.take(10) }
            .onFailure { error = it.message; results = emptyList() }
        loading = false
    }

    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = label + if (required) " *" else "",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(Spacing.xs))

        if (selectedLabel != null) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(Spacing.md),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                ),
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(Spacing.sm),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = selectedLabel,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier.weight(1f),
                    )
                    TextButton(onClick = { onValueChange(null) }, enabled = enabled) {
                        Text("Change")
                    }
                }
            }
            return@Column
        }

        OutlinedTextField(
            value = query,
            onValueChange = { query = it },
            modifier = Modifier.fillMaxWidth(),
            placeholder = { Text(placeholder) },
            singleLine = true,
            enabled = enabled,
            trailingIcon = {
                if (loading) {
                    CircularProgressIndicator(
                        modifier = Modifier.width(18.dp).height(18.dp),
                        strokeWidth = 2.dp,
                    )
                }
            },
            shape = RoundedCornerShape(Spacing.md),
        )

        error?.let {
            Text(
                text = it,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.error,
            )
        }

        if (results.isNotEmpty()) {
            LazyColumn(modifier = Modifier.fillMaxWidth().heightIn(max = 168.dp)) {
                items(results) { option ->
                    TextButton(
                        onClick = {
                            onValueChange(option.value)
                            query = ""
                            results = emptyList()
                        },
                        enabled = enabled,
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Text(option.label, modifier = Modifier.fillMaxWidth())
                    }
                }
            }
        }

        if (freeText != null && query.isNotBlank()) {
            TextButton(
                onClick = {
                    onValueChange(freeText(query.trim()))
                    query = ""
                    results = emptyList()
                },
                enabled = enabled,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text("Use \"${query.trim()}\"", modifier = Modifier.fillMaxWidth())
            }
        }
    }
}

/** Reads the display label out of a stored lookup value map. */
private fun selectedDisplay(value: Any?, fallbackKey: String, idKey: String): String? {
    val map = value as? Map<*, *> ?: return null
    val name = map[fallbackKey]?.toString().orEmpty()
    val id = map[idKey]?.toString().orEmpty()
    val text = listOf(id, name).filter { it.isNotBlank() }.joinToString(" · ")
    return text.ifBlank { null }
}

/**
 * Immediate upload control for the `file` field type. Files upload as soon as
 * they are picked; the stored value is `{ url, name, … }` (or a list when
 * `multiple`).
 */
@Composable
fun FileField(
    field: FormField,
    value: Any?,
    enabled: Boolean,
    lookup: FormLookupRepository,
    onValueChange: (Any?) -> Unit,
) {
    val scope = rememberCoroutineScope()
    var uploading by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }

    val files: List<Map<*, *>> = when (val v = value) {
        is List<*> -> v.filterIsInstance<Map<*, *>>()
        is Map<*, *> -> listOf(v)
        else -> emptyList()
    }

    val picker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument(),
    ) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        scope.launch {
            uploading = true
            error = null
            runCatching { lookup.upload(uri) }
                .onSuccess { uploaded ->
                    val next = if (field.multiple) {
                        files.map { it.toMutableMap() } + uploaded.toFieldValue()
                    } else {
                        listOf(uploaded.toFieldValue())
                    }
                    onValueChange(if (field.multiple) next else next.first())
                }
                .onFailure { error = it.message ?: "Upload failed" }
            uploading = false
        }
    }

    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = field.label + if (field.required) " *" else "",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(Spacing.xs))

        files.forEach { file ->
            Row(
                modifier = Modifier.fillMaxWidth().padding(vertical = Spacing.xxs),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = file["name"]?.toString() ?: file["url"]?.toString().orEmpty(),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.weight(1f),
                )
                TextButton(
                    onClick = {
                        val remaining = files.filter { it != file }.map { it.toMutableMap() }
                        onValueChange(
                            if (field.multiple) remaining.ifEmpty { null }
                            else null,
                        )
                    },
                    enabled = enabled,
                ) {
                    Icon(Icons.Filled.Close, contentDescription = "Remove file")
                }
            }
        }

        if (uploading) {
            LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
        }

        Button(
            onClick = {
                val mimes = field.accept
                    ?.split(',')
                    ?.map { it.trim() }
                    ?.filter { it.isNotEmpty() }
                    ?.toTypedArray()
                    ?: arrayOf("*/*")
                picker.launch(mimes)
            },
            enabled = enabled && !uploading,
            shape = RoundedCornerShape(Spacing.md),
        ) {
            Icon(Icons.Filled.Add, contentDescription = null)
            Spacer(Modifier.width(Spacing.xs))
            Text(if (uploading) "Uploading…" else "Attach file")
        }

        error?.let {
            Text(
                text = it,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.error,
            )
        }
    }
}

/**
 * Placeholder for field types whose catalogue lives in a service the mobile app
 * doesn't talk to yet (`item`, `loinc-test`, `dicom-test`, …). Rendered as a
 * clear notice instead of a misleading text box.
 */
@Composable
fun UnavailableField(field: FormField, reason: String, modifier: Modifier = Modifier) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(Spacing.md),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant,
        ),
    ) {
        Column(modifier = Modifier.padding(Spacing.sm)) {
            Text(
                text = field.label + if (field.required) " *" else "",
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.SemiBold,
            )
            Text(
                text = reason,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}
