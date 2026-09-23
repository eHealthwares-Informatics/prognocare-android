package com.ehealthinformatics.prognocare.feature.records

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ehealthinformatics.prognocare.data.remote.RetrofitClient
import com.ehealthinformatics.prognocare.data.remote.models.ClinicalRequest
import com.ehealthinformatics.prognocare.data.remote.models.Encounter
import com.ehealthinformatics.prognocare.data.remote.models.FormSubmission
import com.ehealthinformatics.prognocare.data.remote.models.Patient
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class PatientDetailState(
    val patient: Patient? = null,
    val encounters: List<Encounter> = emptyList(),
    val requests: List<ClinicalRequest> = emptyList(),
    val submissions: List<FormSubmission> = emptyList(),
    val isLoading: Boolean = true,
    val error: String? = null,
)

/**
 * Loads one patient plus their encounters, requests, and form submissions.
 * Uses an [AssistedFactory] so the screen can pass the patient id at
 * construction time (the route is created per patient).
 */
class PatientDetailViewModel @AssistedInject constructor(
    private val retrofitClient: RetrofitClient,
    @Assisted private val patientId: String,
) : ViewModel() {

    @AssistedFactory
    interface Factory {
        fun create(patientId: String): PatientDetailViewModel
    }

    private val _state = MutableStateFlow(PatientDetailState())
    val state: StateFlow<PatientDetailState> = _state.asStateFlow()

    init {
        refresh()
    }

    fun refresh() {
        viewModelScope.launch {
            _state.value = _state.value.copy(isLoading = true, error = null)
            try {
                val apis = retrofitClient.apis.value
                val patientResp = apis.patientApi.getById(patientId)
                val patient = patientResp.body()?.takeIf { patientResp.isSuccessful }

                val encounters = runCatching {
                    apis.encounterApi.list(patientId = patientId, limit = 50)
                        .body()?.data.orEmpty()
                }.getOrDefault(emptyList())

                val requests = runCatching {
                    apis.requestApi.list(patientId = patientId, limit = 50)
                        .body()?.data.orEmpty()
                }.getOrDefault(emptyList())

                val submissions = runCatching {
                    apis.formApi.listSubmissions(patientId = patientId, limit = 50)
                        .body()?.data.orEmpty()
                }.getOrDefault(emptyList())

                _state.value = PatientDetailState(
                    patient = patient,
                    encounters = encounters,
                    requests = requests,
                    submissions = submissions,
                    isLoading = false,
                    error = if (patient == null) "${patientResp.code()} ${patientResp.message()}" else null,
                )
            } catch (e: Exception) {
                _state.value = _state.value.copy(isLoading = false, error = e.message ?: "Failed to load")
            }
        }
    }
}
