package com.ehealthinformatics.prognocare.feature.clinical

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ehealthinformatics.prognocare.data.remote.RetrofitClient
import com.ehealthinformatics.prognocare.data.remote.api.AdmitFromVisitDto
import com.ehealthinformatics.prognocare.data.remote.models.Admission
import com.ehealthinformatics.prognocare.data.remote.models.ClinicalRequest
import com.ehealthinformatics.prognocare.data.remote.models.Encounter
import com.ehealthinformatics.prognocare.data.remote.models.FormSubmission
import com.ehealthinformatics.prognocare.data.remote.models.Visit
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class VisitDetailState(
    val isLoading: Boolean = true,
    val visit: Visit? = null,
    val encounters: List<Encounter> = emptyList(),
    val requests: List<ClinicalRequest> = emptyList(),
    val submissions: List<FormSubmission> = emptyList(),
    val admission: Admission? = null,
    val error: String? = null,
    val isBusy: Boolean = false,
)

sealed class VisitDetailEvent {
    data class Success(val message: String) : VisitDetailEvent()
    data class Error(val message: String) : VisitDetailEvent()
}

/** Visit detail: visit info + visit-scoped encounters/requests/documents. */
@HiltViewModel
class VisitDetailViewModel @Inject constructor(
    private val retrofitClient: RetrofitClient,
) : ViewModel() {

    private val _state = MutableStateFlow(VisitDetailState())
    val state: StateFlow<VisitDetailState> = _state.asStateFlow()

    private val _events = MutableSharedFlow<VisitDetailEvent>(extraBufferCapacity = 1)
    val events: SharedFlow<VisitDetailEvent> = _events.asSharedFlow()

    private var visitId: String = ""

    fun bind(id: String) {
        if (visitId == id) return
        visitId = id
        load()
    }

    fun load() {
        if (visitId.isBlank()) return
        viewModelScope.launch {
            _state.value = _state.value.copy(isLoading = true, error = null)
            try {
                val apis = retrofitClient.apis.value
                val visit = apis.visitApi.getById(visitId).body()
                    ?: throw IllegalStateException("Visit not found")

                val encounters = runCatching {
                    apis.encounterApi.list(visitId = visitId, limit = 50).body()?.data.orEmpty()
                }.getOrDefault(emptyList())

                val requests = runCatching {
                    apis.requestApi.list(visitId = visitId, limit = 50).body()?.data.orEmpty()
                }.getOrDefault(emptyList())

                val submissions = runCatching {
                    apis.formApi.listSubmissions(visitId = visitId, limit = 50).body()?.data.orEmpty()
                }.getOrDefault(emptyList())

                // Active admission linked to this visit (if any).
                val admission = runCatching {
                    apis.admissionApi.list(status = "ADMITTED", limit = 100).body()?.data.orEmpty()
                        .firstOrNull { it.visitId == visitId }
                }.getOrNull()

                _state.value = VisitDetailState(
                    isLoading = false,
                    visit = visit,
                    encounters = encounters,
                    requests = requests,
                    submissions = submissions,
                    admission = admission,
                )
            } catch (e: Exception) {
                _state.value = _state.value.copy(isLoading = false, error = e.message ?: "Failed to load")
            }
        }
    }

    fun retry() = load()

    fun endVisit() = action("Visit ended") {
        it.visitApi.end(visitId).body() ?: throw IllegalStateException("End failed")
        load()
    }

    fun cancelVisit() = action("Visit cancelled") {
        it.visitApi.cancel(visitId).body() ?: throw IllegalStateException("Cancel failed")
        load()
    }

    /** Convert this ongoing visit into an inpatient admission. */
    fun admitFromVisit() = action("Patient admitted") {
        val admission = it.admissionApi.admitFromVisit(visitId, AdmitFromVisitDto()).body()
            ?: throw IllegalStateException("Admission failed")
        load()
        admission
    }

    private fun action(label: String, block: suspend (com.ehealthinformatics.prognocare.data.remote.ApiBundle) -> Unit) {
        viewModelScope.launch {
            if (_state.value.isBusy) return@launch
            _state.value = _state.value.copy(isBusy = true)
            try {
                block(retrofitClient.apis.value)
                _events.emit(VisitDetailEvent.Success(label))
            } catch (e: Exception) {
                _events.emit(VisitDetailEvent.Error(e.message ?: "Action failed"))
            } finally {
                _state.value = _state.value.copy(isBusy = false)
            }
        }
    }
}
