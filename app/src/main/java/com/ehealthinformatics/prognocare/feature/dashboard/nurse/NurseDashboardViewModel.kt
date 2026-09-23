package com.ehealthinformatics.prognocare.feature.dashboard.nurse

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ehealthinformatics.prognocare.data.auth.SessionStore
import com.ehealthinformatics.prognocare.feature.appointments.AppointmentQuery
import com.ehealthinformatics.prognocare.feature.appointments.AppointmentsRepository
import com.ehealthinformatics.prognocare.feature.records.EmrRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import javax.inject.Inject

@HiltViewModel
class NurseDashboardViewModel @Inject constructor(
    private val appointmentsRepository: AppointmentsRepository,
    private val emrRepository: EmrRepository,
    @ApplicationContext private val context: Context,
) : ViewModel() {

    private val _state = MutableStateFlow(NurseDashboardState())
    val state: StateFlow<NurseDashboardState> = _state.asStateFlow()

    init {
        loadDashboardData()
    }

    fun retry() = loadDashboardData()

    /** Marking a check-in task complete checks the appointment in. */
    fun completeTask(taskId: String) {
        viewModelScope.launch {
            runCatching { appointmentsRepository.checkIn(taskId) }
                .onSuccess { loadDashboardData() }
                .onFailure { _state.value = _state.value.copy(error = it.message) }
        }
    }

    private fun loadDashboardData() {
        _state.value = _state.value.copy(isLoading = true, error = null)
        viewModelScope.launch {
            try {
                val today = LocalDate.now().toString()

                // Today's scheduled appointments drive the check-in queue.
                val todayAppointments = appointmentsRepository.list(AppointmentQuery(date = today, limit = 50))
                val activeVisits = runCatching {
                    emrRepository.encounters(limit = 50)
                }.getOrDefault(emptyList())

                // Open medication requests (PRESCRIPTION not yet completed).
                val medRequests = runCatching {
                    emrRepository.requests(requestType = "PRESCRIPTION", limit = 50)
                }.getOrDefault(emptyList())

                val checkIns = todayAppointments.map { apt ->
                    NurseCheckIn(
                        id = apt.id,
                        patientName = apt.patientName,
                        patientId = apt.patientId,
                        appointmentTime = apt.startTime,
                        appointmentType = apt.typeDisplay,
                        providerName = apt.providerName ?: "—",
                        isCheckedIn = apt.status in listOf("CHECKED_IN", "IN_PROGRESS", "COMPLETED"),
                        vitalsComplete = false,
                    )
                }

                val tasks = todayAppointments
                    .filter { it.status !in listOf("COMPLETED", "CANCELLED", "NO_SHOW") }
                    .map { apt ->
                        NurseTask(
                            id = apt.id,
                            patientName = apt.patientName,
                            patientId = apt.patientId,
                            taskType = if (apt.status in listOf("CHECKED_IN", "IN_PROGRESS")) {
                                NurseTaskType.VITALS
                            } else {
                                NurseTaskType.CHECK_IN
                            },
                            description = if (apt.status in listOf("CHECKED_IN", "IN_PROGRESS")) {
                                "Record pre-consultation vitals"
                            } else {
                                "Check in for ${apt.typeDisplay.lowercase()}"
                            },
                            priority = if (apt.isUrgent) TaskPriority.URGENT else TaskPriority.NORMAL,
                            scheduledTime = apt.startTime,
                            status = when (apt.status) {
                                "IN_PROGRESS" -> TaskStatus.IN_PROGRESS
                                "CHECKED_IN" -> TaskStatus.IN_PROGRESS
                                else -> TaskStatus.PENDING
                            },
                        )
                    }

                _state.value = NurseDashboardState(
                    greeting = when (LocalDateTime.now().hour) {
                        in 5..11 -> "Good morning"
                        in 12..16 -> "Good afternoon"
                        else -> "Good evening"
                    },
                    nurseName = SessionStore.getStaffName(context) ?: "Nurse",
                    todayDate = LocalDate.now().format(DateTimeFormatter.ofPattern("EEEE, MMM d")),
                    patientsCheckedIn = todayAppointments.count {
                        it.status in listOf("CHECKED_IN", "IN_PROGRESS")
                    },
                    vitalsToRecord = todayAppointments.count { it.status == "CHECKED_IN" },
                    medsToAdminister = medRequests.count { it.isOpen },
                    pendingTasks = tasks.count { it.status != TaskStatus.COMPLETED },
                    completedToday = todayAppointments.count { it.status == "COMPLETED" },
                    urgentTasks = tasks.count { it.priority == TaskPriority.URGENT },
                    taskQueue = tasks,
                    upcomingCheckIns = checkIns.filter { !it.isCheckedIn },
                    recentVitals = emptyList(), // vitals live in documentation submissions
                    isLoading = false,
                )
            } catch (e: Exception) {
                _state.value = _state.value.copy(isLoading = false, error = e.message ?: "Failed to load")
            }
        }
    }
}
