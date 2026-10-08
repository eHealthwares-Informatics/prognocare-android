package com.ehealthinformatics.prognocare.feature.clinical

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ehealthinformatics.prognocare.data.remote.RetrofitClient
import com.ehealthinformatics.prognocare.data.remote.api.DischargeDto
import com.ehealthinformatics.prognocare.data.remote.models.Admission
import com.ehealthinformatics.prognocare.data.remote.models.FormSubmission
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class AdmissionDetailState(
    val isLoading: Boolean = true,
    val admission: Admission? = null,
    val submissions: List<FormSubmission> = emptyList(),
    val error: String? = null,
    val isBusy: Boolean = false,
)

sealed class AdmissionDetailEvent {
    data class Success(val message: String) : AdmissionDetailEvent()
    data class Error(val message: String) : AdmissionDetailEvent()
}

/** Admission detail: admission info + visit-scoped documents + discharge. */
@HiltViewModel
class AdmissionDetailViewModel @Inject constructor(
    private val retrofitClient: RetrofitClient,
) : ViewModel() {

    private val _state = MutableStateFlow(AdmissionDetailState())
    val state: StateFlow<AdmissionDetailState> = _state.asStateFlow()

    private val _events = MutableSharedFlow<AdmissionDetailEvent>(extraBufferCapacity = 1)
    val events: SharedFlow<AdmissionDetailEvent> = _events.asSharedFlow()

    private var admissionId: String = ""

    fun bind(id: String) {
        if (admissionId == id) return
        admissionId = id
        load()
    }

    fun load() {
        if (admissionId.isBlank()) return
        viewModelScope.launch {
            _state.value = _state.value.copy(isLoading = true, error = null)
            try {
                val apis = retrofitClient.apis.value
                val admission = apis.admissionApi.getById(admissionId).body()
                    ?: throw IllegalStateException("Admission not found")

                // Documents scoped to the admission's linked visit.
                val submissions = runCatching {
                    admission.visitId?.let { visitId ->
                        apis.formApi.listSubmissions(visitId = visitId, limit = 50).body()?.data.orEmpty()
                    } ?: emptyList()
                }.getOrDefault(emptyList())

                _state.value = AdmissionDetailState(
                    isLoading = false,
                    admission = admission,
                    submissions = submissions,
                )
            } catch (e: Exception) {
                _state.value = _state.value.copy(isLoading = false, error = e.message ?: "Failed to load")
            }
        }
    }

    fun retry() = load()

    fun discharge(dischargeType: String = "DISCHARGED_HOME", summary: String? = null) {
        viewModelScope.launch {
            if (_state.value.isBusy) return@launch
            _state.value = _state.value.copy(isBusy = true)
            try {
                retrofitClient.apis.value.admissionApi.discharge(
                    admissionId,
                    DischargeDto(dischargeType = dischargeType, dischargeSummary = summary),
                ).body() ?: throw IllegalStateException("Discharge failed")
                _events.emit(AdmissionDetailEvent.Success("Patient discharged"))
                load()
            } catch (e: Exception) {
                _events.emit(AdmissionDetailEvent.Error(e.message ?: "Discharge failed"))
            } finally {
                _state.value = _state.value.copy(isBusy = false)
            }
        }
    }
}
