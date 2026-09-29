package com.ehealthinformatics.prognocare.feature.clinical

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ehealthinformatics.prognocare.data.location.LocationScope
import com.ehealthinformatics.prognocare.data.remote.ApiBundle
import com.ehealthinformatics.prognocare.data.remote.RetrofitClient
import com.ehealthinformatics.prognocare.data.remote.models.Appointment
import com.ehealthinformatics.prognocare.data.remote.models.CreateAppointmentDto
import com.ehealthinformatics.prognocare.data.remote.models.CreateEncounterDto
import com.ehealthinformatics.prognocare.data.remote.models.CreateVisitDto
import com.ehealthinformatics.prognocare.data.remote.models.Encounter
import com.ehealthinformatics.prognocare.data.remote.models.Patient
import com.ehealthinformatics.prognocare.data.remote.models.PaginatedResponse
import com.ehealthinformatics.prognocare.data.remote.models.Staff
import com.ehealthinformatics.prognocare.data.remote.models.Visit
import com.ehealthinformatics.prognocare.feature.appointments.ApiException
import com.ehealthinformatics.prognocare.feature.appointments.AppointmentQuery
import com.ehealthinformatics.prognocare.feature.appointments.AppointmentsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import retrofit2.Response

/** Emitted to the UI as transient snackbars. */
sealed class ClinicalUiEvent {
    data class Success(val message: String) : ClinicalUiEvent()
    data class Error(val message: String) : ClinicalUiEvent()
}

/** Unwraps a Retrofit response or throws a user-presentable [ApiException]. */
private fun <T> Response<T>.bodyOrThrow(what: String): T =
    if (isSuccessful) {
        body() ?: throw ApiException(code(), "Empty response for $what")
    } else {
        throw ApiException(code(), "Failed to load $what (${code()})")
    }

// ── Appointments ─────────────────────────────────────────────────

data class ClinicalAppointmentsState(
    val isLoading: Boolean = true,
    val appointments: List<Appointment> = emptyList(),
    val dateFilter: String? = null,
    val statusFilter: String? = null,
    val error: String? = null,
)

/**
 * Appointments list + actions, scoped to the user's location
 * ("location based query"): results are filtered client-side by the active
 * [LocationScope] and checked in / completed / cancelled in place.
 */
@HiltViewModel
class ClinicalAppointmentsViewModel @Inject constructor(
    private val appointmentsRepository: AppointmentsRepository,
    val locationScope: LocationScope,
) : ViewModel() {

    private val _state = MutableStateFlow(ClinicalAppointmentsState())
    val state: StateFlow<ClinicalAppointmentsState> = _state.asStateFlow()

    private val _events = MutableSharedFlow<ClinicalUiEvent>(extraBufferCapacity = 1)
    val events: SharedFlow<ClinicalUiEvent> = _events.asSharedFlow()

    /** Called when the screen enters composition (and again on return). */
    fun load(
        date: String? = _state.value.dateFilter,
        status: String? = _state.value.statusFilter,
    ) {
        viewModelScope.launch {
            _state.value = _state.value.copy(
                isLoading = true,
                error = null,
                dateFilter = date,
                statusFilter = status,
            )
            try {
                val all = appointmentsRepository.list(
                    AppointmentQuery(date = date, status = status, limit = 100),
                )
                val scopeId = locationScope.current().id
                _state.value = _state.value.copy(
                    isLoading = false,
                    appointments = all.filter {
                        LocationScope.matches(scopeId, it.locationId, it.scheduleLocation)
                    },
                )
            } catch (e: Exception) {
                _state.value = _state.value.copy(isLoading = false, error = e.message ?: "Failed to load")
            }
        }
    }

    fun setDate(date: String?) = load(date = date?.takeIf { it.isNotBlank() })
    fun setStatus(status: String?) = load(status = status?.takeIf { it.isNotBlank() })

    fun checkIn(id: String) = transition(id, "Checked in") { appointmentsRepository.checkIn(id) }

    fun complete(id: String) = transition(id, "Completed") { appointmentsRepository.complete(id) }

    fun cancel(id: String) = transition(id, "Cancelled") { appointmentsRepository.cancel(id) }

    private fun transition(id: String, label: String, action: suspend () -> Any) {
        viewModelScope.launch {
            try {
                action()
                _events.emit(ClinicalUiEvent.Success(label))
                load()
            } catch (e: Exception) {
                _events.emit(ClinicalUiEvent.Error(e.message ?: "Action failed"))
            }
        }
    }
}

data class CreateAppointmentUiState(
    val patientQuery: String = "",
    val patientResults: List<Patient> = emptyList(),
    val selectedPatient: Patient? = null,
    val providers: List<Staff> = emptyList(),
    val selectedProvider: Staff? = null,
    val appointmentType: String = "CONSULTATION",
    val priority: String = "ROUTINE",
    val date: String = "",
    val startTime: String = "09:00",
    val reason: String = "",
    val isSaving: Boolean = false,
    val error: String? = null,
)

/** Creates appointments with the active [LocationScope] location attached. */
@HiltViewModel
class CreateAppointmentViewModel @Inject constructor(
    private val appointmentsRepository: AppointmentsRepository,
    val locationScope: LocationScope,
) : ViewModel() {

    private val _state = MutableStateFlow(CreateAppointmentUiState())
    val state: StateFlow<CreateAppointmentUiState> = _state.asStateFlow()

    private val _events = MutableSharedFlow<ClinicalUiEvent>(extraBufferCapacity = 1)
    val events: SharedFlow<ClinicalUiEvent> = _events.asSharedFlow()

    init {
        viewModelScope.launch {
            _state.value = _state.value.copy(
                providers = runCatching { appointmentsRepository.searchProviders() }.getOrDefault(emptyList()),
                date = appointmentsRepository.today(),
            )
        }
    }

    fun update(transform: (CreateAppointmentUiState) -> CreateAppointmentUiState) {
        _state.value = transform(_state.value)
    }

    fun searchPatients(query: String) {
        if (query.isBlank()) return
        viewModelScope.launch {
            try {
                _state.value = _state.value.copy(
                    patientResults = appointmentsRepository.searchPatients(query, limit = 10),
                )
            } catch (_: Exception) {
                _state.value = _state.value.copy(patientResults = emptyList())
            }
        }
    }

    fun selectPatient(patient: Patient) = _state.update {
        it.copy(
            selectedPatient = patient,
            patientResults = emptyList(),
            patientQuery = patient.displayName,
        )
    }

    fun clearPatient() = _state.update { it.copy(selectedPatient = null, patientQuery = "") }

    fun selectProvider(provider: Staff?) = _state.update { it.copy(selectedProvider = provider) }

    fun save(onCreated: () -> Unit) {
        val snapshot = _state.value
        val patient = snapshot.selectedPatient
        if (patient == null) {
            viewModelScope.launch { _events.emit(ClinicalUiEvent.Error("Select a patient first")) }
            return
        }
        if (snapshot.date.isBlank()) {
            viewModelScope.launch { _events.emit(ClinicalUiEvent.Error("Pick a date (yyyy-MM-dd)")) }
            return
        }
        if (snapshot.isSaving) return
        viewModelScope.launch {
            _state.value = snapshot.copy(isSaving = true, error = null)
            try {
                appointmentsRepository.create(
                    CreateAppointmentDto(
                        patientId = patient.patientId.ifBlank { patient.id },
                        patientName = patient.displayName,
                        appointmentType = snapshot.appointmentType,
                        date = snapshot.date,
                        startTime = snapshot.startTime,
                        providerId = snapshot.selectedProvider?.id,
                        providerName = snapshot.selectedProvider?.displayName,
                        locationId = locationScope.current().id,
                        priority = snapshot.priority,
                        reason = snapshot.reason.takeIf { it.isNotBlank() },
                    ),
                )
                _state.value = _state.value.copy(isSaving = false)
                _events.emit(ClinicalUiEvent.Success("Appointment scheduled"))
                onCreated()
            } catch (e: Exception) {
                _state.value = _state.value.copy(isSaving = false, error = e.message)
                _events.emit(ClinicalUiEvent.Error(e.message ?: "Could not schedule appointment"))
            }
        }
    }
}

// ── Visits ───────────────────────────────────────────────────────

data class ClinicalVisitsState(
    val isLoading: Boolean = true,
    val visits: List<Visit> = emptyList(),
    val error: String? = null,
)

/** Visits list (location-scoped) + end/cancel actions. */
@HiltViewModel
class ClinicalVisitsViewModel @Inject constructor(
    private val retrofitClient: RetrofitClient,
    val locationScope: LocationScope,
) : ViewModel() {

    private val _state = MutableStateFlow(ClinicalVisitsState())
    val state: StateFlow<ClinicalVisitsState> = _state.asStateFlow()

    private val _events = MutableSharedFlow<ClinicalUiEvent>(extraBufferCapacity = 1)
    val events: SharedFlow<ClinicalUiEvent> = _events.asSharedFlow()

    /** Called when the screen enters composition (and again on return). */
    fun load() {
        viewModelScope.launch {
            _state.value = _state.value.copy(isLoading = true, error = null)
            try {
                val page = retrofitClient.apis.value.visitApi
                    .list(limit = 100)
                    .bodyOrThrow("visits")
                val scopeId = locationScope.current().id
                _state.value = _state.value.copy(
                    isLoading = false,
                    visits = page.data.filter { LocationScope.matches(scopeId, it.locationId) },
                )
            } catch (e: Exception) {
                _state.value = _state.value.copy(isLoading = false, error = e.message ?: "Failed to load")
            }
        }
    }

    fun end(id: String) = action(id, "Visit ended") { it.visitApi.end(id) }

    fun cancel(id: String) = action(id, "Visit cancelled") { it.visitApi.cancel(id) }

    private fun action(id: String, label: String, block: suspend (ApiBundle) -> Unit) {
        viewModelScope.launch {
            try {
                block(retrofitClient.apis.value)
                _events.emit(ClinicalUiEvent.Success(label))
                load()
            } catch (e: Exception) {
                _events.emit(ClinicalUiEvent.Error(e.message ?: "Action failed"))
            }
        }
    }
}

data class CreateVisitUiState(
    val patientQuery: String = "",
    val patientResults: List<Patient> = emptyList(),
    val selectedPatient: Patient? = null,
    val visitType: String = "OUTPATIENT",
    val isSaving: Boolean = false,
    val error: String? = null,
)

/** Creates visits with the active [LocationScope] location attached. */
@HiltViewModel
class CreateVisitViewModel @Inject constructor(
    private val retrofitClient: RetrofitClient,
    val locationScope: LocationScope,
) : ViewModel() {

    private val _state = MutableStateFlow(CreateVisitUiState())
    val state: StateFlow<CreateVisitUiState> = _state.asStateFlow()

    private val _events = MutableSharedFlow<ClinicalUiEvent>(extraBufferCapacity = 1)
    val events: SharedFlow<ClinicalUiEvent> = _events.asSharedFlow()

    fun update(transform: (CreateVisitUiState) -> CreateVisitUiState) {
        _state.value = transform(_state.value)
    }

    fun searchPatients(query: String) {
        if (query.isBlank()) return
        viewModelScope.launch {
            try {
                val response = retrofitClient.apis.value.patientApi.list(page = 1, limit = 10, search = query)
                _state.value = _state.value.copy(patientResults = response.body()?.data.orEmpty())
            } catch (_: Exception) {
                _state.value = _state.value.copy(patientResults = emptyList())
            }
        }
    }

    fun selectPatient(patient: Patient) = _state.update {
        it.copy(
            selectedPatient = patient,
            patientResults = emptyList(),
            patientQuery = patient.displayName,
        )
    }

    fun clearPatient() = _state.update { it.copy(selectedPatient = null, patientQuery = "") }

    fun setType(type: String) = _state.update { it.copy(visitType = type) }

    fun save(onCreated: (Visit) -> Unit) {
        val snapshot = _state.value
        val patient = snapshot.selectedPatient
        if (patient == null) {
            viewModelScope.launch { _events.emit(ClinicalUiEvent.Error("Select a patient first")) }
            return
        }
        if (snapshot.isSaving) return
        viewModelScope.launch {
            _state.value = snapshot.copy(isSaving = true, error = null)
            try {
                val visit = retrofitClient.apis.value.visitApi.create(
                    CreateVisitDto(
                        patientId = patient.patientId.ifBlank { patient.id },
                        patientName = patient.displayName,
                        visitType = snapshot.visitType,
                        locationId = locationScope.current().id,
                    ),
                ).bodyOrThrow("visit")
                _state.value = _state.value.copy(isSaving = false)
                _events.emit(ClinicalUiEvent.Success("Visit started"))
                onCreated(visit)
            } catch (e: Exception) {
                _state.value = _state.value.copy(isSaving = false, error = e.message)
                _events.emit(ClinicalUiEvent.Error(e.message ?: "Could not start visit"))
            }
        }
    }
}

// ── Encounters ───────────────────────────────────────────────────

data class ClinicalEncountersState(
    val isLoading: Boolean = true,
    val encounters: List<Encounter> = emptyList(),
    val visitsById: Map<String, Visit> = emptyMap(),
    val error: String? = null,
)

/**
 * Encounters list, scoped through their parent visit's location: the backend
 * carries no location on an encounter itself, so filtering by the active
 * [LocationScope] uses the visit each encounter belongs to.
 */
@HiltViewModel
class ClinicalEncountersViewModel @Inject constructor(
    private val retrofitClient: RetrofitClient,
    val locationScope: LocationScope,
) : ViewModel() {

    private val _state = MutableStateFlow(ClinicalEncountersState())
    val state: StateFlow<ClinicalEncountersState> = _state.asStateFlow()

    /** Called when the screen enters composition (and again on return). */
    fun load() {
        viewModelScope.launch {
            _state.value = _state.value.copy(isLoading = true, error = null)
            try {
                val apis = retrofitClient.apis.value
                val encounterPage = apis.encounterApi.list(limit = 100).bodyOrThrow("encounters")
                val visitPage = runCatching {
                    apis.visitApi.list(limit = 100).bodyOrThrow("visits")
                }.getOrDefault(PaginatedResponse(data = emptyList()))
                val visitsById = visitPage.data.associateBy { it.id }
                val scopeId = locationScope.current().id
                val filtered = encounterPage.data.filter { encounter ->
                    LocationScope.matches(scopeId, encounter.visitId?.let { visitsById[it]?.locationId })
                }
                _state.value = _state.value.copy(
                    isLoading = false,
                    encounters = filtered,
                    visitsById = visitsById,
                )
            } catch (e: Exception) {
                _state.value = _state.value.copy(isLoading = false, error = e.message ?: "Failed to load")
            }
        }
    }
}


enum class EncounterStartMode(val label: String) {
    FROM_VISIT("From visits"),
    FROM_PATIENT("From patient"),
    FROM_APPOINTMENT("From appointment"),
}

data class CreateEncounterUiState(
    val mode: EncounterStartMode = EncounterStartMode.FROM_VISIT,
    val isLoading: Boolean = false,
    // FROM_VISIT: patients currently with an open visit (one per patient).
    val visits: List<Visit> = emptyList(),
    val selectedVisit: Visit? = null,
    // FROM_APPOINTMENT: today's open appointments (one per patient).
    val appointments: List<Appointment> = emptyList(),
    val selectedAppointment: Appointment? = null,
    // Free patient search, used by FROM_PATIENT.
    val patientQuery: String = "",
    val patientResults: List<Patient> = emptyList(),
    val selectedPatient: Patient? = null,
    val encounterType: String = "CONSULTATION",
    val reason: String = "",
    val notes: String = "",
    val isSaving: Boolean = false,
    val error: String? = null,
)

/**
 * Documents a new encounter in one of three explicit modes. The patient is
 * mandatory in every mode and each mode's candidate list shows a patient at
 * most once:
 *  - [EncounterStartMode.FROM_VISIT]: pick an ongoing visit; the encounter
 *    joins that visit.
 *  - [EncounterStartMode.FROM_PATIENT]: pick any patient; the encounter is
 *    created without a visit.
 *  - [EncounterStartMode.FROM_APPOINTMENT]: pick a today's open appointment;
 *    a visit is started for the appointment and the encounter joins it.
 */
@HiltViewModel
class CreateEncounterViewModel @Inject constructor(
    private val retrofitClient: RetrofitClient,
    private val appointmentsRepository: AppointmentsRepository,
    val locationScope: LocationScope,
) : ViewModel() {

    private val _state = MutableStateFlow(CreateEncounterUiState())
    val state: StateFlow<CreateEncounterUiState> = _state.asStateFlow()

    private val _events = MutableSharedFlow<ClinicalUiEvent>(extraBufferCapacity = 1)
    val events: SharedFlow<ClinicalUiEvent> = _events.asSharedFlow()

    init {
        setMode(EncounterStartMode.FROM_VISIT)
    }

    fun update(transform: (CreateEncounterUiState) -> CreateEncounterUiState) {
        _state.value = transform(_state.value)
    }

    fun setMode(mode: EncounterStartMode) {
        if (mode == _state.value.mode && _state.value.visits.isNotEmpty()) return
        _state.value = _state.value.copy(
            mode = mode,
            isLoading = true,
            selectedVisit = null,
            selectedAppointment = null,
            selectedPatient = null,
            patientQuery = "",
            patientResults = emptyList(),
            error = null,
        )
        viewModelScope.launch {
            when (mode) {
                EncounterStartMode.FROM_VISIT -> {
                    val visits = runCatching {
                        retrofitClient.apis.value.visitApi
                            .list(status = "ONGOING", limit = 100)
                            .bodyOrThrow("visits")
                            .data
                            // One entry per patient: first ongoing visit wins.
                            .distinctBy { it.patientId }
                    }.getOrDefault(emptyList())
                    _state.value = _state.value.copy(isLoading = false, visits = visits)
                }
                EncounterStartMode.FROM_APPOINTMENT -> {
                    val appointments = runCatching {
                        appointmentsRepository.list(
                            AppointmentQuery(
                                date = appointmentsRepository.today(),
                                limit = 100,
                            ),
                        )
                            .filter { it.status == "SCHEDULED" || it.status == "CHECKED_IN" }
                            .sortedBy { it.startTime }
                            // One entry per patient: earliest appointment wins.
                            .distinctBy { it.patientId }
                    }.getOrDefault(emptyList())
                    _state.value = _state.value.copy(isLoading = false, appointments = appointments)
                }
                EncounterStartMode.FROM_PATIENT ->
                    _state.value = _state.value.copy(isLoading = false)
            }
        }
    }

    /** Candidates for the active mode, already deduplicated per patient. */
    fun selectVisit(visit: Visit) = _state.update {
        it.copy(
            selectedVisit = visit,
            selectedAppointment = null,
            selectedPatient = null,
            patientQuery = visit.patientName.ifBlank { "Patient ${visit.patientId}" },
        )
    }

    fun selectAppointment(appointment: Appointment) = _state.update {
        it.copy(
            selectedAppointment = appointment,
            selectedVisit = null,
            selectedPatient = null,
            patientQuery = appointment.patientName.ifBlank { "Patient ${appointment.patientId}" },
        )
    }

    fun searchPatients(query: String) {
        if (_state.value.mode != EncounterStartMode.FROM_PATIENT) return
        viewModelScope.launch {
            try {
                val response = retrofitClient.apis.value.patientApi.list(
                    page = 1,
                    limit = 10,
                    search = query.takeIf { it.isNotBlank() },
                )
                // Distinct patients only, and never one already covered by an
                // ongoing visit or a today's appointment list.
                val results = response.body()?.data.orEmpty().distinctBy { it.id }
                _state.value = _state.value.copy(patientResults = results)
            } catch (_: Exception) {
                _state.value = _state.value.copy(patientResults = emptyList())
            }
        }
    }

    fun selectPatient(patient: Patient) = _state.update {
        it.copy(
            selectedPatient = patient,
            patientResults = emptyList(),
            patientQuery = patient.displayName,
            selectedVisit = null,
            selectedAppointment = null,
        )
    }

    fun clearSelection() = _state.update {
        it.copy(
            selectedPatient = null,
            selectedVisit = null,
            selectedAppointment = null,
            patientQuery = "",
        )
    }

    fun setType(type: String) = _state.update { it.copy(encounterType = type) }

    fun save(onCreated: (Encounter) -> Unit) {
        val snapshot = _state.value
        if (snapshot.isSaving) return
        viewModelScope.launch {
            _state.value = snapshot.copy(isSaving = true, error = null)
            try {
                val apis = retrofitClient.apis.value
                val now = java.time.OffsetDateTime.now().toString()
                val encounter = when (snapshot.mode) {
                    EncounterStartMode.FROM_VISIT -> {
                        val visit = snapshot.selectedVisit
                            ?: throw ApiException(400, "Select a patient with an open visit")
                        apis.encounterApi.create(
                            CreateEncounterDto(
                                patientId = visit.patientId,
                                visitId = visit.id,
                                encounterType = snapshot.encounterType,
                                encounterDatetime = now,
                                reason = snapshot.reason.takeIf { it.isNotBlank() },
                                notes = snapshot.notes.takeIf { it.isNotBlank() },
                            ),
                        ).bodyOrThrow("encounter")
                    }
                    EncounterStartMode.FROM_PATIENT -> {
                        val patient = snapshot.selectedPatient
                            ?: throw ApiException(400, "Select a patient first")
                        // No visit: a standalone encounter for the patient.
                        apis.encounterApi.create(
                            CreateEncounterDto(
                                patientId = patient.patientId.ifBlank { patient.id },
                                encounterType = snapshot.encounterType,
                                encounterDatetime = now,
                                reason = snapshot.reason.takeIf { it.isNotBlank() },
                                notes = snapshot.notes.takeIf { it.isNotBlank() },
                            ),
                        ).bodyOrThrow("encounter")
                    }
                    EncounterStartMode.FROM_APPOINTMENT -> {
                        val appointment = snapshot.selectedAppointment
                            ?: throw ApiException(400, "Select an appointment first")
                        // A visit is started for the appointment; the
                        // encounter joins it so it stays visit-scoped.
                        val visit = apis.visitApi.create(
                            CreateVisitDto(
                                patientId = appointment.patientId,
                                patientName = appointment.patientName,
                                visitType = "OUTPATIENT",
                                providerId = appointment.providerId,
                                providerName = appointment.providerName,
                                locationId = locationScope.current().id,
                                appointmentId = appointment.id,
                                startDatetime = now,
                            ),
                        ).bodyOrThrow("visit")
                        apis.encounterApi.create(
                            CreateEncounterDto(
                                patientId = appointment.patientId,
                                visitId = visit.id,
                                encounterType = snapshot.encounterType,
                                providerId = appointment.providerId,
                                providerName = appointment.providerName,
                                encounterDatetime = now,
                                reason = snapshot.reason.takeIf { it.isNotBlank() },
                                notes = snapshot.notes.takeIf { it.isNotBlank() },
                            ),
                        ).bodyOrThrow("encounter")
                    }
                }
                _state.value = _state.value.copy(isSaving = false)
                _events.emit(ClinicalUiEvent.Success("Encounter documented"))
                onCreated(encounter)
            } catch (e: Exception) {
                _state.value = _state.value.copy(isSaving = false, error = e.message)
                _events.emit(ClinicalUiEvent.Error(e.message ?: "Could not document encounter"))
            }
        }
    }
}
