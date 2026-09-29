package com.ehealthinformatics.prognocare.feature.records

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ehealthinformatics.prognocare.data.remote.RetrofitClient
import com.ehealthinformatics.prognocare.data.remote.models.ClinicalRequest
import com.ehealthinformatics.prognocare.data.remote.models.Encounter
import com.ehealthinformatics.prognocare.data.remote.models.FormSubmission
import com.ehealthinformatics.prognocare.data.remote.models.Patient
import com.ehealthinformatics.prognocare.data.remote.models.Visit
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

data class PatientDetailState(
    val patient: Patient? = null,
    val visits: List<Visit> = emptyList(),
    val encounters: List<Encounter> = emptyList(),
    val requests: List<ClinicalRequest> = emptyList(),
    val submissions: List<FormSubmission> = emptyList(),
    val isLoading: Boolean = true,
    val error: String? = null,
)

/**
 * Loads one patient plus their encounters, requests, and form submissions.
 * The patient id is bound via [bind] from the route argument.
 */
@HiltViewModel
class PatientDetailViewModel @Inject constructor(
    private val retrofitClient: RetrofitClient,
) : ViewModel() {

    private var patientId: String = ""

    /** Bind the route argument and load (idempotent per patient). */
    fun bind(id: String) {
        if (patientId == id) return
        patientId = id
        refresh()
    }

    private val _state = MutableStateFlow(PatientDetailState())
    val state: StateFlow<PatientDetailState> = _state.asStateFlow()

    init {
        refresh()
    }

    fun refresh() {
        if (patientId.isBlank()) return
        viewModelScope.launch {
            _state.value = _state.value.copy(isLoading = true, error = null)
            try {
                val apis = retrofitClient.apis.value
                // The route argument may be the record id OR the MRN
                // (patient_id) — some role lists pass the MRN. Try id first,
                // then the by-MRN lookup.
                val idResp = apis.patientApi.getById(patientId)
                val patient = idResp.body()?.takeIf { idResp.isSuccessful }
                    ?: apis.patientApi.getByMrn(patientId).body()
                        ?.takeIf { it.id.isNotBlank() }

                // Clinical records key on the MRN (patient_id), while some
                // callers pass the record id — scope child queries by the
                // resolved MRN when the patient resolved.
                val scope = patient?.patientId?.takeIf { it.isNotBlank() } ?: patientId

                val visits = runCatching {
                    apis.visitApi.list(patientId = scope, limit = 50)
                        .body()?.data.orEmpty()
                }.getOrDefault(emptyList())

                val encounters = runCatching {
                    apis.encounterApi.list(patientId = scope, limit = 50)
                        .body()?.data.orEmpty()
                }.getOrDefault(emptyList())

                val requests = runCatching {
                    apis.requestApi.list(patientId = scope, limit = 50)
                        .body()?.data.orEmpty()
                }.getOrDefault(emptyList())

                val submissions = runCatching {
                    apis.formApi.listSubmissions(patientId = scope, limit = 50)
                        .body()?.data.orEmpty()
                }.getOrDefault(emptyList())

                _state.value = PatientDetailState(
                    patient = patient,
                    visits = visits,
                    encounters = encounters,
                    requests = requests,
                    submissions = submissions,
                    isLoading = false,
                    error = if (patient == null) "Patient not found" else null,
                )
            } catch (e: Exception) {
                _state.value = _state.value.copy(isLoading = false, error = e.message ?: "Failed to load")
            }
        }
    }
}
