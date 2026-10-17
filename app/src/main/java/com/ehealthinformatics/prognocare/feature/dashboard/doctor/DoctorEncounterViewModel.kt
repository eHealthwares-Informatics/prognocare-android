package com.ehealthinformatics.prognocare.feature.dashboard.doctor

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ehealthinformatics.prognocare.data.remote.RetrofitClient
import com.ehealthinformatics.prognocare.data.remote.models.ClinicalRequest
import com.ehealthinformatics.prognocare.data.remote.models.Encounter
import com.ehealthinformatics.prognocare.data.remote.models.FormSubmission
import com.ehealthinformatics.prognocare.data.remote.models.Patient
import com.ehealthinformatics.prognocare.data.remote.models.UpdateEncounterDto
import com.ehealthinformatics.prognocare.data.remote.models.Visit
import com.ehealthinformatics.prognocare.feature.forms.VitalsReadings
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class DoctorEncounterState(
    val encounter: Encounter? = null,
    val patient: Patient? = null,
    /** Parent visit, when the encounter belongs to one. */
    val visit: Visit? = null,
    /** Display name resolved from the visit, patient record, or fallback. */
    val patientName: String = "",
    /** Documentation (form submissions) of the encounter AND its visit. */
    val submissions: List<FormSubmission> = emptyList(),
    /** Requests of the encounter AND its visit. */
    val requests: List<ClinicalRequest> = emptyList(),
    /** Latest VITALS submission for the visit (null when none). */
    val vitals: VitalsReadings.Vitals? = null,
    val isBusy: Boolean = false,
    val isLoading: Boolean = true,
    val error: String? = null,
)

/**
 * Loads one encounter with everything needed on the clinical screen: the
 * patient (name via the parent visit, the patient record, or the MRN), the
 * visit, the visit/encounter documentation, and the visit/encounter requests.
 * Also owns ending the encounter (status → COMPLETED, endedAt → now).
 */
@HiltViewModel
class DoctorEncounterViewModel @Inject constructor(
    private val retrofitClient: RetrofitClient,
) : ViewModel() {

    private val _state = MutableStateFlow(DoctorEncounterState())
    val state: StateFlow<DoctorEncounterState> = _state.asStateFlow()

    private var encounterId: String = ""

    fun bind(id: String) {
        if (encounterId == id) return
        encounterId = id
        load()
    }

    fun load() {
        if (encounterId.isBlank()) return
        viewModelScope.launch {
            _state.value = _state.value.copy(isLoading = true, error = null)
            try {
                val apis = retrofitClient.apis.value
                val encounterResp = apis.encounterApi.getById(encounterId)
                val encounter = encounterResp.body()?.takeIf { encounterResp.isSuccessful }
                if (encounter == null) {
                    _state.value = _state.value.copy(
                        isLoading = false,
                        error = "${encounterResp.code()} ${encounterResp.message()}",
                    )
                    return@launch
                }

                // Parent visit first: it carries the patient display name.
                val visit = encounter.visitId?.let { visitId ->
                    runCatching { apis.visitApi.getById(visitId).body() }.getOrNull()
                }

                val patient = runCatching {
                    apis.patientApi.getById(encounter.patientId).body()
                }.getOrNull() ?: runCatching {
                    apis.patientApi.getByMrn(encounter.patientId).body()
                }.getOrNull()

                val patientName = visit?.patientName?.takeIf { it.isNotBlank() && it != encounter.patientId }
                    ?: patient?.displayName?.takeIf { it.isNotBlank() }
                    ?: "Patient ${encounter.patientId}"

                val submissions = runCatching {
                    apis.formApi.listSubmissions(
                        patientId = encounter.patientId,
                        visitId = encounter.visitId,
                        encounterId = encounterId,
                        limit = 50,
                    ).body()?.data.orEmpty()
                }.getOrDefault(emptyList())

                // Vitals recorded outside this encounter/visit (e.g. nurse
                // flow before a visit was linked) are keyed by MRN only —
                // surface them so the VitalsCard updates after mobile save.
                val patientScopedVitals = runCatching {
                    apis.formApi.listSubmissions(
                        patientId = encounter.patientId,
                        limit = 50,
                    ).body()?.data.orEmpty()
                }.getOrDefault(emptyList()).filter {
                    it.formName.contains("vital", ignoreCase = true)
                }

                val vitalsSource = (submissions + patientScopedVitals).distinctBy { it.id }

                val requests = runCatching {
                    apis.requestApi.list(
                        patientId = encounter.patientId,
                        visitId = encounter.visitId,
                        limit = 50,
                    ).body()?.data.orEmpty()
                }.getOrDefault(emptyList())

                _state.value = DoctorEncounterState(
                    encounter = encounter,
                    patient = patient,
                    visit = visit,
                    patientName = patientName,
                    submissions = submissions,
                    requests = requests,
                    vitals = VitalsReadings.latest(vitalsSource),
                    isLoading = false,
                )
            } catch (e: Exception) {
                _state.value = _state.value.copy(isLoading = false, error = e.message ?: "Failed to load")
            }
        }
    }

    /** Ends the encounter: status COMPLETED + endedAt = now. */
    fun endEncounter() {
        if (_state.value.isBusy) return
        viewModelScope.launch {
            _state.value = _state.value.copy(isBusy = true, error = null)
            try {
                val response = retrofitClient.apis.value.encounterApi.update(
                    encounterId,
                    UpdateEncounterDto(
                        status = "COMPLETED",
                        endedAt = java.time.OffsetDateTime.now().toString(),
                    ),
                )
                val updated = response.body()?.takeIf { response.isSuccessful }
                _state.value = _state.value.copy(
                    isBusy = false,
                    encounter = updated ?: _state.value.encounter?.copy(status = "COMPLETED"),
                )
            } catch (e: Exception) {
                _state.value = _state.value.copy(isBusy = false, error = e.message ?: "Could not end encounter")
            }
        }
    }
}
