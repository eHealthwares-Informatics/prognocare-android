package com.ehealthinformatics.prognocare.feature.dashboard.doctor

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ehealthinformatics.prognocare.data.remote.models.Appointment
import com.ehealthinformatics.prognocare.feature.appointments.AppointmentQuery
import com.ehealthinformatics.prognocare.feature.appointments.AppointmentsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/** Status chips on the appointments screen; TODAY filters to today's date. */
object AppointmentFilters {
    const val TODAY = "Today"
    const val SCHEDULED = "Scheduled"
    const val IN_PROGRESS = "In Progress"
    const val COMPLETED = "Completed"
    const val ALL = "All"
    val ALL_FILTERS = listOf(TODAY, SCHEDULED, IN_PROGRESS, COMPLETED, ALL)
}

sealed interface AppointmentUiEvent {
    data class Success(val message: String) : AppointmentUiEvent
    data class Error(val message: String) : AppointmentUiEvent
}

data class DoctorAppointmentsState(
    val isLoading: Boolean = true,
    val isMutating: Boolean = false,
    val error: String? = null,
    val appointments: List<Appointment> = emptyList(),
    val searchQuery: String = "",
    val filter: String = AppointmentFilters.TODAY,
)

/** Clinician-facing appointment schedule with status-transition actions. */
@HiltViewModel
class DoctorAppointmentsViewModel @Inject constructor(
    private val repository: AppointmentsRepository,
) : ViewModel() {

    private val _state = MutableStateFlow(DoctorAppointmentsState())
    val state: StateFlow<DoctorAppointmentsState> = _state.asStateFlow()

    private val _events = MutableSharedFlow<AppointmentUiEvent>(extraBufferCapacity = 1)
    val events: SharedFlow<AppointmentUiEvent> = _events.asSharedFlow()

    private var loadJob: Job? = null

    init {
        refresh()
    }

    fun refresh() {
        loadJob?.cancel()
        loadJob = viewModelScope.launch { load() }
    }

    fun setSearchQuery(query: String) {
        _state.value = _state.value.copy(searchQuery = query)
        loadJob?.cancel()
        loadJob = viewModelScope.launch {
            delay(300) // debounce
            load()
        }
    }

    fun setFilter(filter: String) {
        _state.value = _state.value.copy(filter = filter)
        loadJob?.cancel()
        loadJob = viewModelScope.launch { load() }
    }

    fun checkIn(id: String) = mutate("Patient checked in") {
        repository.checkIn(id)
    }

    fun complete(id: String) = mutate("Appointment completed") {
        repository.complete(id)
    }

    fun noShow(id: String) = mutate("Marked as no-show") {
        repository.noShow(id)
    }

    fun cancel(id: String, reason: String = "") = mutate("Appointment cancelled") {
        repository.cancel(id, reason)
    }

    private fun mutate(successMessage: String, action: suspend () -> Unit) {
        if (_state.value.isMutating) return
        viewModelScope.launch {
            _state.value = _state.value.copy(isMutating = true, error = null)
            try {
                action()
                _events.emit(AppointmentUiEvent.Success(successMessage))
                load()
            } catch (e: Exception) {
                _events.emit(AppointmentUiEvent.Error(e.message ?: "Action failed"))
            } finally {
                _state.value = _state.value.copy(isMutating = false)
            }
        }
    }

    private suspend fun load() {
        val snapshot = _state.value
        _state.value = snapshot.copy(isLoading = true, error = null)
        try {
            val query = AppointmentQuery(
                date = if (snapshot.filter == AppointmentFilters.TODAY) repository.today() else null,
                status = when (snapshot.filter) {
                    AppointmentFilters.SCHEDULED -> "SCHEDULED"
                    AppointmentFilters.IN_PROGRESS -> "IN_PROGRESS"
                    AppointmentFilters.COMPLETED -> "COMPLETED"
                    else -> null
                },
                search = snapshot.searchQuery.takeIf { it.isNotBlank() },
            )
            val result = repository.list(query)
            _state.value = _state.value.copy(isLoading = false, appointments = result)
        } catch (e: Exception) {
            _state.value = _state.value.copy(isLoading = false, error = e.message)
        }
    }
}
