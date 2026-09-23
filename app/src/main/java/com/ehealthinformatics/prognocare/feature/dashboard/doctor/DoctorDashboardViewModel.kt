package com.ehealthinformatics.prognocare.feature.dashboard.doctor

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ehealthinformatics.prognocare.data.auth.SessionStore
import com.ehealthinformatics.prognocare.data.remote.RetrofitClient
import com.ehealthinformatics.prognocare.data.remote.models.Appointment
import com.ehealthinformatics.prognocare.data.remote.models.DashboardSummary
import com.ehealthinformatics.prognocare.data.remote.models.Patient
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

data class AppointmentUi(
    val id: String,
    val patientName: String,
    val type: String,
    val time: String,
    val status: String,
    val isUrgent: Boolean = false,
    val patientId: String = "",
)

data class PatientUi(
    val id: String,
    val name: String,
    val mrn: String,
    val age: Int,
    val lastVisit: String,
    val diagnosis: String,
)

data class DoctorDashboardState(
    val greeting: String = "",
    val doctorName: String = "",
    val todayDate: String = "",
    val totalPatients: Int = 0,
    val todayAppointments: Int = 0,
    val pendingTasks: Int = 0,
    val activeEncounters: Int = 0,
    val completedToday: Int = 0,
    val urgentCount: Int = 0,
    val upcomingAppointments: List<AppointmentUi> = emptyList(),
    val recentPatients: List<PatientUi> = emptyList(),
    val isLoading: Boolean = true,
    val error: String? = null,
)

@HiltViewModel
class DoctorDashboardViewModel @Inject constructor(
    private val appointmentsRepository: AppointmentsRepository,
    private val emrRepository: EmrRepository,
    private val retrofitClient: RetrofitClient,
    @ApplicationContext private val context: Context,
) : ViewModel() {

    private val _state = MutableStateFlow(DoctorDashboardState())
    val state: StateFlow<DoctorDashboardState> = _state.asStateFlow()

    init {
        loadDashboardData()
    }

    fun retry() = loadDashboardData()

    private fun loadDashboardData() {
        _state.value = _state.value.copy(isLoading = true, error = null)
        viewModelScope.launch {
            try {
                val today = LocalDate.now()
                val staffId = SessionStore.getStaffId(context)

                val summary = runCatching { emrRepository.dashboard(today.toString()) }.getOrNull()

                // Self-scoped when a staff record is linked; falls back to the clinic-wide view.
                val myAppointments: List<Appointment> = appointmentsRepository.list(
                    AppointmentQuery(date = today.toString(), providerId = staffId, limit = 50),
                )
                val myEncounters = runCatching {
                    emrRepository.encounters(limit = 100).filter { it.providerId == staffId }
                }.getOrDefault(emptyList())

                val patientsPage = retrofitClient.apis.value.patientApi.let { api ->
                    val resp = api.list(page = 1, limit = 5)
                    if (resp.isSuccessful) resp.body()?.data.orEmpty() else emptyList()
                }

                val upcoming = myAppointments
                    .sortedBy { it.startTime }
                    .map { apt ->
                        AppointmentUi(
                            id = apt.id,
                            patientName = apt.patientName,
                            type = apt.typeDisplay,
                            time = apt.startTime,
                            status = apt.status,
                            isUrgent = apt.isUrgent,
                            patientId = apt.patientId,
                        )
                    }

                val recentPatients = patientsPage.map { p ->
                    PatientUi(
                        id = p.id,
                        name = p.displayName,
                        mrn = p.patientId,
                        age = p.ageYears,
                        lastVisit = p.createdAt?.take(10) ?: "—",
                        diagnosis = p.bloodGroup ?: "—",
                    )
                }

                _state.value = DoctorDashboardState(
                    greeting = greetingForNow(),
                    doctorName = SessionStore.getStaffName(context) ?: "Doctor",
                    todayDate = today.format(DateTimeFormatter.ofPattern("EEEE, MMM d")),
                    totalPatients = summary?.metrics?.totalPatients ?: 0,
                    todayAppointments = myAppointments.size,
                    pendingTasks = myAppointments.count {
                        it.status in listOf("SCHEDULED", "CHECKED_IN", "IN_PROGRESS")
                    },
                    activeEncounters = myEncounters.size,
                    completedToday = summary?.metrics?.completed ?: myAppointments.count { it.status == "COMPLETED" },
                    urgentCount = myAppointments.count {
                        it.priority in listOf("URGENT", "EMERGENCY") &&
                            it.status !in listOf("COMPLETED", "CANCELLED", "NO_SHOW")
                    },
                    upcomingAppointments = upcoming,
                    recentPatients = recentPatients,
                    isLoading = false,
                )
            } catch (e: Exception) {
                _state.value = _state.value.copy(isLoading = false, error = e.message ?: "Failed to load")
            }
        }
    }

    private fun greetingForNow(): String {
        return when (LocalDateTime.now().hour) {
            in 5..11 -> "Good morning"
            in 12..16 -> "Good afternoon"
            else -> "Good evening"
        }
    }
}
