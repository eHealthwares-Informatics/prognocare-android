package com.ehealthinformatics.prognocare.feature.dashboard.nurse

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ehealthinformatics.prognocare.data.remote.RetrofitClient
import com.ehealthinformatics.prognocare.data.remote.models.FormDefinition
import com.ehealthinformatics.prognocare.data.remote.models.Patient
import com.ehealthinformatics.prognocare.feature.forms.SchemaKeyMapper
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

sealed class VitalsSaveResult {
    data object Success : VitalsSaveResult()
    data class Failure(val message: String) : VitalsSaveResult()
}

/**
 * Resolves the published VITALS form (code match) so the vitals recording
 * screen can submit into it, and submits values keyed by clinical concept —
 * they are mapped onto the form definition's actual field keys
 * ([SchemaKeyMapper]) so the payload always matches the schema the server
 * validates against.
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

    /** Debounce-less simple patient search used by the patient picker. */
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
     * Submits vitals as a VITALS form submission. [raw] is keyed by clinical
     * concept (e.g. "bpSystolic", "oxygenSaturation"); the schema mapper
     * translates them into whatever keys this facility's VITALS form uses.
     *
     * [patientMrn] must be the patient **MRN** (`Patient.patientId`) — the
     * same identifier encounters/visits store — not the patient row UUID.
     * Optional [visitId]/[encounterId] link the submission so encounter
     * screens can load it.
     */
    suspend fun submitVitals(
        form: FormDefinition,
        patientMrn: String,
        raw: Map<String, Any?>,
        visitId: String? = null,
        encounterId: String? = null,
    ): VitalsSaveResult {
        return runCatching {
            val payload = SchemaKeyMapper.map(form.schemaJson, raw)
            if (payload.isEmpty()) {
                return@runCatching VitalsSaveResult.Failure(
                    "the VITALS form schema has no recognizable fields — check the form definition",
                )
            }
            val response = retrofitClient.apis.value.formApi.createSubmission(
                com.ehealthinformatics.prognocare.data.remote.models.CreateFormSubmissionDto(
                    formDefinitionId = form.id,
                    patientId = patientMrn,
                    visitId = visitId,
                    encounterId = encounterId,
                    dataJson = payload,
                    status = "SUBMITTED",
                ),
            )
            if (response.isSuccessful) {
                VitalsSaveResult.Success
            } else {
                val detail = runCatching { response.errorBody()?.string() }.getOrNull()
                    ?.lineSequence()?.firstOrNull()?.take(220)
                VitalsSaveResult.Failure(detail ?: "${response.code()} ${response.message()}")
            }
        }.getOrElse {
            VitalsSaveResult.Failure(it.message ?: "unexpected error")
        }
    }

    /**
     * Resolves the patient's ongoing visit and active encounter (best-effort)
     * so vitals submitted from the nurse flow show up on the clinical screen.
     * Returns [VisitLink] with nulls when nothing is open.
     */
    data class VisitLink(val visitId: String?, val encounterId: String?)

    suspend fun resolveVisitLink(patientMrn: String): VisitLink {
        return runCatching {
            val apis = retrofitClient.apis.value
            val visit = apis.visitApi.list(status = "ONGOING", patientId = patientMrn, limit = 1)
                .body()?.data?.firstOrNull()
            val encounter = visit?.id?.let { visitId ->
                apis.encounterApi.list(visitId = visitId, limit = 20)
                    .body()?.data?.firstOrNull { it.isActive }
            } ?: apis.encounterApi.list(limit = 50)
                .body()?.data?.firstOrNull {
                    it.isActive && it.patientId == patientMrn
                }
            VisitLink(visitId = visit?.id, encounterId = encounter?.id)
        }.getOrDefault(VisitLink(null, null))
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
