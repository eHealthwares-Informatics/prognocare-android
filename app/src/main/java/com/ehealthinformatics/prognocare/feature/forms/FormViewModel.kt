package com.ehealthinformatics.prognocare.feature.forms

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ehealthinformatics.prognocare.data.remote.RetrofitClient
import com.ehealthinformatics.prognocare.data.remote.models.CreateFormSubmissionDto
import com.ehealthinformatics.prognocare.data.remote.models.FormDefinition
import com.ehealthinformatics.prognocare.data.remote.models.FormSubmission
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import javax.inject.Inject

sealed class FormUiState {
    data object Loading : FormUiState()
    data class Ready(
        val formDefinition: FormDefinition,
        val schema: FormSchemaData,
        val data: FormData,
        val errors: List<String> = emptyList(),
        val submitting: Boolean = false,
    ) : FormUiState()
    data object Empty : FormUiState()
    data class Submitted(val submission: FormSubmission) : FormUiState()
    data class Error(val message: String) : FormUiState()
}

@HiltViewModel
class FormViewModel @Inject constructor(
    private val retrofitClient: RetrofitClient,
) : ViewModel() {

    private val _available = MutableStateFlow<List<FormDefinition>>(emptyList())
    val available: StateFlow<List<FormDefinition>> = _available.asStateFlow()

    private val _loadingAvailable = MutableStateFlow(false)
    val loadingAvailable: StateFlow<Boolean> = _loadingAvailable.asStateFlow()

    private val _loadError = MutableStateFlow<String?>(null)
    val loadError: StateFlow<String?> = _loadError.asStateFlow()

    private val _state = MutableStateFlow<FormUiState>(FormUiState.Empty)
    val state: StateFlow<FormUiState> = _state.asStateFlow()

    private val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
        isLenient = true
    }

    fun loadAvailable() {
        viewModelScope.launch {
            _loadError.value = null
            _loadingAvailable.value = true
            try {
                runCatching { retrofitClient.apis.value.formApi.availableForms() }
                    .onSuccess { resp ->
                        if (resp.isSuccessful && resp.body() != null) {
                            _available.value = resp.body()!!.data
                        } else {
                            _loadError.value = "Could not load forms (${resp.code()})"
                        }
                    }
                    .onFailure { e ->
                        _loadError.value = e.message ?: "Could not load forms"
                    }
            } finally {
                _loadingAvailable.value = false
            }
        }
    }

    /** Loads a form definition by id and initializes a fresh value bag. */
    fun loadForm(formId: String) {
        viewModelScope.launch {
            _state.value = FormUiState.Loading
            runCatching { retrofitClient.apis.value.formApi.getDefinitionById(formId) }
                .onSuccess { resp ->
                    val body = resp.body()
                    if (resp.isSuccessful && body != null) {
                        val schema = FormSchemaParser.parse(body.schemaJson)
                        _state.value = FormUiState.Ready(
                            formDefinition = body,
                            schema = schema,
                            data = buildInitial(schema),
                        )
                    } else {
                        _state.value = FormUiState.Error(
                            "Could not load form (${resp.code()})",
                        )
                    }
                }
                .onFailure { e ->
                    _state.value = FormUiState.Error(e.message ?: "Could not load form")
                }
        }
    }

    fun updateField(key: String, value: Any?) {
        val s = _state.value as? FormUiState.Ready ?: return
        _state.value = s.copy(data = s.data + (key to value))
    }

    fun updateTableRow(field: FormField, rowIndex: Int, columnKey: String, value: Any?) {
        val s = _state.value as? FormUiState.Ready ?: return
        val rows: MutableList<MutableMap<String, Any?>> =
            ((s.data[field.key] as? List<*>) ?: emptyList<Any>()).map {
                (it as? Map<*, *>)?.entries?.associate { e -> e.key.toString() to e.value }?.toMutableMap()
                    ?: mutableMapOf()
            }.toMutableList()
        if (rowIndex >= rows.size) return
        rows[rowIndex][columnKey] = value
        _state.value = s.copy(data = s.data + (field.key to rows))
    }

    fun addTableRow(field: FormField) {
        val s = _state.value as? FormUiState.Ready ?: return
        val rows = (s.data[field.key] as? List<*>)?.toMutableList() ?: mutableListOf()
        _state.value = s.copy(data = s.data + (field.key to (rows + mutableMapOf<String, Any?>())))
    }

    fun removeTableRow(field: FormField, rowIndex: Int) {
        val s = _state.value as? FormUiState.Ready ?: return
        val rows = (s.data[field.key] as? List<*>)?.toMutableList() ?: return
        if (rowIndex < 0 || rowIndex >= rows.size) return
        rows.removeAt(rowIndex)
        _state.value = s.copy(data = s.data + (field.key to rows))
    }

    /**
     * Submits the form against the given clinical context (patient + optional
     * visit/encounter) as a SUBMITTED documentation record.
     */
    fun submit(patientId: String, visitId: String?, encounterId: String?) {
        val s = _state.value as? FormUiState.Ready ?: return
        val errors = FormValidator.validate(s.schema, s.data)
        if (errors.isNotEmpty()) {
            _state.value = s.copy(errors = errors)
            return
        }
        _state.value = s.copy(errors = emptyList(), submitting = true)

        viewModelScope.launch {
            try {
                val api = retrofitClient.apis.value.formApi
                // Resolve the value bag onto the schema's real field keys via
                // SchemaKeyMapper.remap — exact keys win, then normalized and
                // concept-alias matches; unknown and null entries are dropped.
                val jsonElements = s.data.mapValues { (_, value) -> toJsonElement(value) }
                val payload = SchemaKeyMapper.remap(s.formDefinition.schemaJson, jsonElements)
                val resp = api.createSubmission(
                    CreateFormSubmissionDto(
                        formDefinitionId = s.formDefinition.id,
                        patientId = patientId,
                        visitId = visitId,
                        encounterId = encounterId,
                        dataJson = payload,
                        status = "SUBMITTED",
                    ),
                )
                if (resp.isSuccessful && resp.body() != null) {
                    _state.value = FormUiState.Submitted(resp.body()!!)
                } else {
                    // Surface the server's validation message, not just the code.
                    val detail = runCatching { resp.errorBody()?.string() }
                        .getOrNull()
                        ?.lineSequence()?.firstOrNull()?.take(300)
                    _state.value = s.copy(
                        submitting = false,
                        errors = listOf(detail ?: "Submit failed (${resp.code()})"),
                    )
                }
            } catch (e: Exception) {
                _state.value = s.copy(submitting = false, errors = listOf("Network error: ${e.message}"))
            }
        }
    }

    private fun buildInitial(schema: FormSchemaData): FormData {
        val out = mutableMapOf<String, Any?>()
        fun walk(fields: List<FormField>) {
            for (f in fields) {
                when (f.type) {
                    FormFieldType.SECTION -> Unit
                    FormFieldType.TAB, FormFieldType.COL -> walk(f.fields)
                    FormFieldType.TABLE -> out[f.key] = emptyList<Any>()
                    else -> if (f.defaultValue != null) out[f.key] = f.defaultValue
                }
            }
        }
        walk(schema.fields)
        return out
    }

    private fun toJsonElement(value: Any?): JsonElement {
        return when (value) {
            null -> JsonNull
            is Boolean -> JsonPrimitive(value)
            is Int -> JsonPrimitive(value)
            is Long -> JsonPrimitive(value)
            is Double -> JsonPrimitive(value)
            is Float -> JsonPrimitive(value)
            is String -> JsonPrimitive(value)
            is Map<*, *> -> JsonObject(value.entries.associate { (k, v) ->
                k.toString() to toJsonElement(v)
            })
            is List<*> -> JsonArray(value.map { toJsonElement(it) })
            else -> JsonPrimitive(value.toString())
        }
    }
}