package com.ehealthinformatics.prognocare.feature.requests

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ehealthinformatics.prognocare.data.auth.SessionStore
import com.ehealthinformatics.prognocare.data.remote.RetrofitClient
import com.ehealthinformatics.prognocare.data.remote.models.ClinicalRequest
import com.ehealthinformatics.prognocare.data.remote.models.CreateRequestDto
import com.ehealthinformatics.prognocare.data.remote.models.Patient
import com.ehealthinformatics.prognocare.data.remote.models.RequestHistoryEntry
import com.ehealthinformatics.prognocare.data.remote.models.RequestItem
import com.ehealthinformatics.prognocare.feature.appointments.ApiException
import com.ehealthinformatics.prognocare.feature.records.EmrRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class RequestsListState(
    val isLoading: Boolean = true,
    val requests: List<ClinicalRequest> = emptyList(),
    val filterType: String? = null,
    val error: String? = null,
)

data class RequestDetailState(
    val request: ClinicalRequest? = null,
    val history: List<RequestHistoryEntry> = emptyList(),
    val isLoading: Boolean = true,
    val isBusy: Boolean = false,
    val error: String? = null,
)

data class CreateRequestState(
    val patientQuery: String = "",
    val patientResults: List<Patient> = emptyList(),
    val selectedPatient: Patient? = null,
    val requestType: String = "LAB",
    val priority: String = "ROUTINE",
    val diagnosis: String = "",
    val clinicalNotes: String = "",
    val items: List<RequestItem> = listOf(RequestItem(name = "")),
    val isSaving: Boolean = false,
    val error: String? = null,
)

sealed class RequestUiEvent {
    data class Success(val message: String) : RequestUiEvent()
    data class Error(val message: String) : RequestUiEvent()
}

@HiltViewModel
class RequestsListViewModel @Inject constructor(
    private val emrRepository: EmrRepository,
    @ApplicationContext private val context: Context,
) : ViewModel() {

    private val _state = MutableStateFlow(RequestsListState())
    val state: StateFlow<RequestsListState> = _state.asStateFlow()

    init {
        load()
    }

    fun load(filterType: String? = _state.value.filterType) {
        viewModelScope.launch {
            _state.value = _state.value.copy(isLoading = true, error = null, filterType = filterType)
            try {
                val staffId = SessionStore.getStaffId(context)
                val mine = emrRepository.requests(
                    requestType = filterType,
                    limit = 100,
                ).filter { staffId == null || it.orderingProviderId == staffId }
                _state.value = _state.value.copy(isLoading = false, requests = mine)
            } catch (e: Exception) {
                _state.value = _state.value.copy(isLoading = false, error = e.message ?: "Failed to load")
            }
        }
    }

    fun filter(type: String?) = load(type)

    /**
     * Medication-administration hook: marks a prescription request as
     * COMPLETED with an administration note. Best-effort; errors surface
     * through [state].
     */
    fun onMedicationAdministered(requestId: String) {
        viewModelScope.launch {
            try {
                emrRepository.transitionRequest(
                    requestId,
                    com.ehealthinformatics.prognocare.data.remote.models.TransitionRequestStatusDto(
                        status = "COMPLETED",
                        reason = "Administered by nurse",
                    ),
                )
                emrRepository.addRequestNote(requestId, "Medication administered (mobile)")
                load("PRESCRIPTION")
            } catch (e: Exception) {
                _state.value = _state.value.copy(error = e.message ?: "Could not record administration")
            }
        }
    }
}

@HiltViewModel
class RequestDetailViewModel @Inject constructor(
    private val emrRepository: EmrRepository,
) : ViewModel() {

    private val _state = MutableStateFlow(RequestDetailState())
    val state: StateFlow<RequestDetailState> = _state.asStateFlow()

    private val _events = MutableSharedFlow<RequestUiEvent>(extraBufferCapacity = 1)
    val events: SharedFlow<RequestUiEvent> = _events.asSharedFlow()

    private var requestId: String = ""

    fun bind(id: String) {
        if (requestId == id) return
        requestId = id
        load()
    }

    fun load() {
        if (requestId.isBlank()) return
        viewModelScope.launch {
            _state.value = _state.value.copy(isLoading = true, error = null)
            try {
                val request = emrRepository.request(requestId)
                val history = runCatching { emrRepository.requestHistory(requestId) }
                    .getOrDefault(emptyList())
                _state.value = _state.value.copy(
                    request = request,
                    history = history,
                    isLoading = false,
                )
            } catch (e: Exception) {
                _state.value = _state.value.copy(isLoading = false, error = e.message ?: "Failed to load")
            }
        }
    }

    fun transition(status: String, reason: String? = null) {
        if (_state.value.isBusy) return
        viewModelScope.launch {
            _state.value = _state.value.copy(isBusy = true)
            try {
                val updated = emrRepository.transitionRequest(
                    requestId,
                    com.ehealthinformatics.prognocare.data.remote.models.TransitionRequestStatusDto(
                        status = status,
                        reason = reason,
                    ),
                )
                _state.value = _state.value.copy(request = updated, isBusy = false)
                _events.emit(RequestUiEvent.Success("Request $status"))
                load()
            } catch (e: Exception) {
                _state.value = _state.value.copy(isBusy = false)
                _events.emit(RequestUiEvent.Error(e.message ?: "Transition failed"))
            }
        }
    }

    fun addNote(note: String) {
        if (note.isBlank() || _state.value.isBusy) return
        viewModelScope.launch {
            _state.value = _state.value.copy(isBusy = true)
            try {
                emrRepository.addRequestNote(requestId, note)
                _events.emit(RequestUiEvent.Success("Note added"))
                load()
            } catch (e: Exception) {
                _events.emit(RequestUiEvent.Error(e.message ?: "Could not add note"))
            } finally {
                _state.value = _state.value.copy(isBusy = false)
            }
        }
    }

    fun sync() {
        if (_state.value.isBusy) return
        viewModelScope.launch {
            _state.value = _state.value.copy(isBusy = true)
            try {
                val updated = emrRepository.syncRequest(requestId)
                _state.value = _state.value.copy(request = updated)
                _events.emit(RequestUiEvent.Success("Sync: ${updated.syncStatus ?: "done"}"))
            } catch (e: Exception) {
                _events.emit(RequestUiEvent.Error(e.message ?: "Sync failed"))
            } finally {
                _state.value = _state.value.copy(isBusy = false)
            }
        }
    }
}

@HiltViewModel
class CreateRequestViewModel @Inject constructor(
    private val emrRepository: EmrRepository,
    private val retrofitClient: RetrofitClient,
    @ApplicationContext private val context: Context,
) : ViewModel() {

    private val _state = MutableStateFlow(CreateRequestState())
    val state: StateFlow<CreateRequestState> = _state.asStateFlow()

    private val _events = MutableSharedFlow<RequestUiEvent>(extraBufferCapacity = 1)
    val events: SharedFlow<RequestUiEvent> = _events.asSharedFlow()

    fun update(transform: (CreateRequestState) -> CreateRequestState) {
        _state.value = transform(_state.value)
    }

    fun searchPatients(query: String) {
        if (query.isBlank()) return
        viewModelScope.launch {
            try {
                val response = retrofitClient.apis.value.patientApi.list(
                    page = 1,
                    limit = 10,
                    search = query,
                )
                _state.value = _state.value.copy(
                    patientResults = if (response.isSuccessful) response.body()?.data.orEmpty() else emptyList(),
                )
            } catch (e: Exception) {
                _state.value = _state.value.copy(patientResults = emptyList())
            }
        }
    }

    fun selectPatient(patient: Patient) {
        _state.value = _state.value.copy(
            selectedPatient = patient,
            patientResults = emptyList(),
            patientQuery = patient.displayName,
        )
    }

    fun updateItem(index: Int, transform: (RequestItem) -> RequestItem) {
        val items = _state.value.items.toMutableList()
        if (index in items.indices) {
            items[index] = transform(items[index])
            _state.value = _state.value.copy(items = items)
        }
    }

    fun addItem() {
        _state.value = _state.value.copy(items = _state.value.items + RequestItem(name = ""))
    }

    fun removeItem(index: Int) {
        val items = _state.value.items.toMutableList()
        if (items.size > 1 && index in items.indices) {
            items.removeAt(index)
            _state.value = _state.value.copy(items = items)
        }
    }

    fun save() {
        val current = _state.value
        val patient = current.selectedPatient
        if (patient == null) {
            viewModelScope.launch { _events.emit(RequestUiEvent.Error("Select a patient first")) }
            return
        }
        if (current.items.all { it.name.isBlank() }) {
            viewModelScope.launch { _events.emit(RequestUiEvent.Error("Add at least one item with a name")) }
            return
        }
        if (_state.value.isSaving) return
        viewModelScope.launch {
            _state.value = _state.value.copy(isSaving = true, error = null)
            try {
                val staffId = SessionStore.getStaffId(context)
                val staffName = SessionStore.getStaffName(context)
                emrRepository.createRequest(
                    CreateRequestDto(
                        patientId = patient.id,
                        patientName = patient.displayName,
                        requestType = current.requestType,
                        priority = current.priority,
                        diagnosis = current.diagnosis.takeIf { it.isNotBlank() },
                        clinicalNotes = current.clinicalNotes.takeIf { it.isNotBlank() },
                        orderingProviderId = staffId,
                        orderingProviderName = staffName,
                        items = current.items.filter { it.name.isNotBlank() },
                    ),
                )
                _events.emit(RequestUiEvent.Success("Request created"))
                _state.value = CreateRequestState()
            } catch (e: ApiException) {
                _events.emit(RequestUiEvent.Error(e.message ?: "Create failed"))
            } catch (e: Exception) {
                _events.emit(RequestUiEvent.Error(e.message ?: "Create failed"))
            } finally {
                _state.value = _state.value.copy(isSaving = false)
            }
        }
    }
}
