package com.ehealthinformatics.prognocare.feature.dashboard.patient

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ehealthinformatics.prognocare.data.auth.SessionStore
import com.ehealthinformatics.prognocare.data.remote.models.Appointment
import com.ehealthinformatics.prognocare.data.remote.models.Patient
import com.ehealthinformatics.prognocare.feature.appointments.AppointmentQuery
import com.ehealthinformatics.prognocare.feature.appointments.AppointmentsRepository
import com.ehealthinformatics.prognocare.feature.dashboard.doctor.AppointmentUiEvent
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class PatientIdentityState(
    val isSearching: Boolean = false,
    val results: List<Patient> = emptyList(),
    val error: String? = null,
)

data class PatientAppointmentsState(
    val isLoading: Boolean = true,
    val isCancelling: Boolean = false,
    val error: String? = null,
    /** MRN of the patient record linked to this device, when known. */
    val patientId: String? = null,
    val patientName: String? = null,
    val needsIdentity: Boolean = false,
    val upcoming: List<Appointment> = emptyList(),
    val past: List<Appointment> = emptyList(),
    val identity: PatientIdentityState = PatientIdentityState(),
)

/**
 * Patient-role appointments: requires a one-time link between the signed-in
 * account and their patient record (MRN), persisted via [SessionStore].
 */
@HiltViewModel
class PatientAppointmentsViewModel @Inject constructor(
    private val repository: AppointmentsRepository,
    @ApplicationContext private val context: Context,
) : ViewModel() {

    private val _state = MutableStateFlow(PatientAppointmentsState())
    val state: StateFlow<PatientAppointmentsState> = _state.asStateFlow()

    private val _events = MutableSharedFlow<AppointmentUiEvent>(extraBufferCapacity = 1)
    val events: SharedFlow<AppointmentUiEvent> = _events.asSharedFlow()

    init {
        load()
    }

    fun load() {
        val storedId = SessionStore.getPatientId(context)
        if (storedId == null) {
            _state.value = PatientAppointmentsState(
                isLoading = false,
                needsIdentity = true,
                patientName = SessionStore.getPatientName(context),
            )
            return
        }
        viewModelScope.launch {
            _state.value = _state.value.copy(
                isLoading = true,
                error = null,
                needsIdentity = false,
                patientId = storedId,
                patientName = SessionStore.getPatientName(context),
            )
            try {
                val appointments = repository.list(AppointmentQuery(patientId = storedId))
                val today = repository.today()
                val activeStatuses = setOf("SCHEDULED", "CHECKED_IN", "IN_PROGRESS")
                _state.value = _state.value.copy(
                    isLoading = false,
                    upcoming = appointments.filter {
                        it.date >= today && it.status in activeStatuses
                    },
                    past = appointments.filterNot {
                        it.date >= today && it.status in activeStatuses
                    },
                )
            } catch (e: Exception) {
                _state.value = _state.value.copy(isLoading = false, error = e.message)
            }
        }
    }

    /** One-time self-identification: search patient records by name or MRN. */
    fun searchIdentity(query: String) {
        viewModelScope.launch {
            _state.value = _state.value.copy(
                identity = PatientIdentityState(isSearching = true),
            )
            try {
                val results = repository.searchPatients(query)
                _state.value = _state.value.copy(
                    identity = PatientIdentityState(results = results),
                )
            } catch (e: Exception) {
                _state.value = _state.value.copy(
                    identity = PatientIdentityState(error = e.message),
                )
            }
        }
    }

    fun confirmIdentity(patient: Patient) {
        val mrn = patient.patientId.ifBlank { patient.id }
        if (mrn.isBlank()) return
        SessionStore.savePatient(context, mrn, patient.displayName)
        load()
    }

    fun cancel(id: String) {
        if (_state.value.isCancelling) return
        viewModelScope.launch {
            _state.value = _state.value.copy(isCancelling = true)
            try {
                repository.cancel(id, "Cancelled by patient")
                _events.emit(AppointmentUiEvent.Success("Appointment cancelled"))
                load()
            } catch (e: Exception) {
                _events.emit(AppointmentUiEvent.Error(e.message ?: "Cancel failed"))
            } finally {
                _state.value = _state.value.copy(isCancelling = false)
            }
        }
    }
}
