package com.ehealthinformatics.prognocare.feature.dashboard.therapist

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
class TherapistDashboardViewModel @Inject constructor(
    private val appointmentsRepository: AppointmentsRepository,
    private val emrRepository: EmrRepository,
    @ApplicationContext private val context: Context,
) : ViewModel() {

    private val _state = MutableStateFlow(TherapistDashboardState())
    val state: StateFlow<TherapistDashboardState> = _state.asStateFlow()

    init {
        loadDashboardData()
    }

    fun retry() = loadDashboardData()

    private fun loadDashboardData() {
        viewModelScope.launch {
            try {
                val today = LocalDate.now()
                val staffId = SessionStore.getStaffId(context)

                val myAppointments = appointmentsRepository.list(
                    AppointmentQuery(date = today.toString(), providerId = staffId, limit = 50),
                )
                val myEncounters = runCatching {
                    emrRepository.encounters(limit = 100).filter { it.providerId == staffId }
                }.getOrDefault(emptyList())
                val summary = runCatching { emrRepository.dashboard(today.toString()) }.getOrNull()

                val sessions = myAppointments.map { apt ->
                    TherapySession(
                        id = apt.id,
                        patientName = apt.patientName,
                        patientMrn = apt.patientId,
                        sessionType = when (apt.appointmentType) {
                            "FOLLOW_UP" -> SessionType.FOLLOW_UP
                            "PROCEDURE", "TREATMENT" -> SessionType.TREATMENT
                            "REHABILITATION" -> SessionType.REHABILITATION
                            else -> SessionType.CONSULTATION
                        },
                        scheduledTime = apt.startTime,
                        duration = "",
                        location = apt.scheduleLocation ?: "—",
                        status = when (apt.status) {
                            "IN_PROGRESS" -> SessionStatus.IN_PROGRESS
                            "COMPLETED" -> SessionStatus.COMPLETED
                            "CANCELLED" -> SessionStatus.CANCELLED
                            "NO_SHOW" -> SessionStatus.NO_SHOW
                            else -> SessionStatus.SCHEDULED
                        },
                        notes = apt.reason,
                        isUrgent = apt.isUrgent,
                    )
                }

                _state.value = TherapistDashboardState(
                    greeting = when (LocalDateTime.now().hour) {
                        in 5..11 -> "Good morning"
                        in 12..16 -> "Good afternoon"
                        else -> "Good evening"
                    },
                    therapistName = SessionStore.getStaffName(context) ?: "Therapist",
                    specialty = "Therapy Clinic",
                    todayDate = today.format(DateTimeFormatter.ofPattern("EEEE, MMM d")),
                    todaySessions = sessions.size,
                    // Plans/assessments have no backend module — surfaced with demo tags.
                    activePlans = 0,
                    pendingAssessments = 0,
                    completedToday = summary?.metrics?.completed ?: sessions.count { it.status == SessionStatus.COMPLETED },
                    upcomingSessions = sessions,
                    activePatients = emptyList(),
                    recentAssessments = emptyList(),
                    therapyPlans = emptyList(),
                    isLoading = false,
                )
            } catch (e: Exception) {
                _state.value = _state.value.copy(isLoading = false, error = e.message ?: "Failed to load")
            }
        }
    }
}
