package com.ehealthinformatics.prognocare.feature.dashboard.nurse

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ehealthinformatics.prognocare.data.remote.RetrofitClient
import com.ehealthinformatics.prognocare.data.remote.models.FormDefinition
import com.ehealthinformatics.prognocare.data.remote.models.Patient
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class VitalsSetupState(
    val vitalsForm: FormDefinition? = null,
    val isLoading: Boolean = true,
    val error: String? = null,
)

/**
 * Resolves the published VITALS form (code match) so the vitals recording
 * screen can hand off into the dynamic-form renderer with the right context.
 */
@HiltViewModel
class VitalsRecordingViewModel @Inject constructor(
    private val retrofitClient: RetrofitClient,
) : ViewModel() {

    private val _state = MutableStateFlow(VitalsSetupState())
    val state: StateFlow<VitalsSetupState> = _state.asStateFlow()

    init {
        load()
    }

    /** Debounced-less simple patient search used by the patient picker. */
    fun searchPatients(query: String, onResult: (List<Patient>) -> Unit) {
        if (query.isBlank()) return
        viewModelScope.launch {
            try {
                val response = retrofitClient.apis.value.patientApi.list(
                    page = 1,
                    limit = 10,
                    search = query,
                )
                onResult(if (response.isSuccessful) response.body()?.data.orEmpty() else emptyList())
            } catch (e: Exception) {
                onResult(emptyList())
            }
        }
    }

    /**
     * Submits vitals values into the VITALS form definition. Field keys are
     * best-effort (backend seed schema); unknown keys are ignored server-side.
     */
    suspend fun submitVitals(formId: String, patientId: String, data: Map<String, Any?>): Boolean {
        return runCatching {
            val response = retrofitClient.apis.value.formApi.createSubmission(
                com.ehealthinformatics.prognocare.data.remote.models.CreateFormSubmissionDto(
                    formDefinitionId = formId,
                    patientId = patientId,
                    dataJson = kotlinx.serialization.json.Json.encodeToJsonElement(
                        kotlinx.serialization.serializer<Map<String, Any?>>(),
                        data,
                    ),
                    status = "SUBMITTED",
                ),
            )
            response.isSuccessful
        }.getOrDefault(false)
    }

    fun load() {
        viewModelScope.launch {
            _state.value = _state.value.copy(isLoading = true, error = null)
            try {
                val response = retrofitClient.apis.value.formApi.availableForms()
                val forms = if (response.isSuccessful) response.body()?.data.orEmpty() else emptyList()
                val vitals = forms.firstOrNull {
                    it.code.equals("VITALS", ignoreCase = true) ||
                        it.name.contains("vital", ignoreCase = true)
                }
                _state.value = VitalsSetupState(
                    vitalsForm = vitals,
                    isLoading = false,
                    error = if (vitals == null && !response.isSuccessful) {
                        "${response.code()} ${response.message()}"
                    } else {
                        null
                    },
                )
            } catch (e: Exception) {
                _state.value = _state.value.copy(isLoading = false, error = e.message ?: "Failed to load")
            }
        }
    }
}
