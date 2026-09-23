package com.ehealthinformatics.prognocare.feature.dashboard.specialist

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
class SpecialistDashboardViewModel @Inject constructor(
    private val appointmentsRepository: AppointmentsRepository,
    private val emrRepository: EmrRepository,
    @ApplicationContext private val context: Context,
) : ViewModel() {

    private val _state = MutableStateFlow(SpecialistDashboardState())
    val state: StateFlow<SpecialistDashboardState> = _state.asStateFlow()

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

                val consultations = myAppointments.map { apt ->
                    SpecialistConsultation(
                        id = apt.id,
                        patientName = apt.patientName,
                        patientMrn = apt.patientId,
                        type = apt.typeDisplay,
                        date = apt.date,
                        time = apt.startTime,
                        location = apt.scheduleLocation ?: "—",
                        status = apt.status,
                        reason = apt.reason,
                        isUrgent = apt.isUrgent,
                    )
                }

                _state.value = SpecialistDashboardState(
                    greeting = when (LocalDateTime.now().hour) {
                        in 5..11 -> "Good morning"
                        in 12..16 -> "Good afternoon"
                        else -> "Good evening"
                    },
                    specialistName = SessionStore.getStaffName(context) ?: "Specialist",
                    specialty = "Specialist Clinic",
                    todayDate = today.format(DateTimeFormatter.ofPattern("EEEE, MMM d")),
                    // Referrals have no backend module yet — keep zeroed with demo tags in UI.
                    pendingReferrals = 0,
                    activePatients = summary?.metrics?.activeVisits ?: myEncounters.size,
                    completedReviews = summary?.metrics?.completed ?: 0,
                    urgentCases = myAppointments.count {
                        it.isUrgent && it.status !in listOf("COMPLETED", "CANCELLED", "NO_SHOW")
                    },
                    recentReferrals = emptyList(),
                    upcomingConsultations = consultations,
                    specialtyStats = SpecialtyStats(),
                    isLoading = false,
                )
            } catch (e: Exception) {
                _state.value = _state.value.copy(isLoading = false, error = e.message ?: "Failed to load")
            }
        }
    }
}
