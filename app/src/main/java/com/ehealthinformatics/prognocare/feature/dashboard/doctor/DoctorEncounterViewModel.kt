package com.ehealthinformatics.prognocare.feature.dashboard.doctor

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ehealthinformatics.prognocare.data.remote.RetrofitClient
import com.ehealthinformatics.prognocare.data.remote.models.Encounter
import com.ehealthinformatics.prognocare.data.remote.models.FormSubmission
import com.ehealthinformatics.prognocare.data.remote.models.Patient
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class DoctorEncounterState(
    val encounter: Encounter? = null,
    val patient: Patient? = null,
    val submissions: List<FormSubmission> = emptyList(),
    val isLoading: Boolean = true,
    val error: String? = null,
)

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
                val patient = encounter?.let { enc ->
                    runCatching {
                        apis.patientApi.getById(enc.patientId).body()
                    }.getOrNull()
                }
                val submissions = runCatching {
                    apis.formApi.listSubmissions(
                        patientId = encounter?.patientId,
                        encounterId = encounterId,
                        limit = 50,
                    ).body()?.data.orEmpty()
                }.getOrDefault(emptyList())

                _state.value = DoctorEncounterState(
                    encounter = encounter,
                    patient = patient,
                    submissions = submissions,
                    isLoading = false,
                    error = if (encounter == null) {
                        "${encounterResp.code()} ${encounterResp.message()}"
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
