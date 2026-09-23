package com.ehealthinformatics.prognocare.feature.dashboard.admin

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
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import javax.inject.Inject

@HiltViewModel
class AdminDashboardViewModel @Inject constructor(
    private val appointmentsRepository: AppointmentsRepository,
    private val emrRepository: EmrRepository,
    @ApplicationContext private val context: Context,
) : ViewModel() {

    private val _state = MutableStateFlow(AdminDashboardState())
    val state: StateFlow<AdminDashboardState> = _state.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error = _error

    init {
        loadDashboardData()
    }

    fun retry() = loadDashboardData()

    private fun loadDashboardData() {
        viewModelScope.launch {
            try {
                _state.value = _state.value.copy(isLoading = true, error = null)
                val today = java.time.LocalDate.now().toString()
                val appointments = appointmentsRepository.list(AppointmentQuery(date = today, limit = 100))
                val summary = runCatching { emrRepository.dashboard(today) }.getOrNull()
                val staff = runCatching { emrRepository.staff(limit = 100) }.getOrDefault(emptyList())

                val queue = appointments.map { apt ->
                    AdminCheckIn(
                        id = apt.id,
                        patientName = apt.patientName,
                        patientMrn = apt.patientId,
                        patientAge = 0,
                        appointmentTime = apt.startTime,
                        appointmentType = apt.typeDisplay,
                        providerName = apt.providerName ?: "—",
                        department = apt.scheduleLocation ?: "—",
                        status = when (apt.status) {
                            "CHECKED_IN" -> CheckInStatus.CHECKED_IN
                            "IN_PROGRESS" -> CheckInStatus.IN_VISIT
                            "COMPLETED" -> CheckInStatus.CHECKED_OUT
                            "NO_SHOW" -> CheckInStatus.NO_SHOW
                            else -> CheckInStatus.WAITING
                        },
                        checkedInAt = if (apt.status in listOf("CHECKED_IN", "IN_PROGRESS")) "Now" else null,
                        checkedOutAt = if (apt.status == "COMPLETED") "Now" else null,
                    )
                }

                _state.value = AdminDashboardState(
                    greeting = greeting(),
                    adminName = SessionStore.getStaffName(context) ?: "Administrator",
                    todayDate = todayDate(),
                    totalPatients = summary?.metrics?.totalPatients ?: 0,
                    checkedInToday = queue.count {
                        it.status in listOf(CheckInStatus.CHECKED_IN, CheckInStatus.IN_VISIT, CheckInStatus.CHECKED_OUT)
                    },
                    waitingForCheckIn = queue.count { it.status == CheckInStatus.WAITING },
                    totalStaff = staff.size,
                    activeVisits = summary?.metrics?.activeVisits ?: 0,
                    checkInQueue = queue,
                    isLoading = false,
                )
            } catch (e: Exception) {
                _state.value = _state.value.copy(isLoading = false, error = e.message ?: "Failed to load")
            }
        }
    }

    /** Live patient-directory search for the registration desk. */
    fun searchPatients(query: String) {
        if (query.isBlank()) {
            _state.value = _state.value.copy(searchResults = emptyList())
            return
        }
        viewModelScope.launch {
            try {
                val response = emrRepository.patients(search = query, limit = 10)
                _state.value = _state.value.copy(
                    searchResults = response.data.map { p ->
                        AdminPatient(
                            id = p.id,
                            name = p.displayName,
                            age = p.ageYears,
                            mrn = p.patientId,
                            gender = p.gender ?: "—",
                            phone = p.phone ?: "—",
                            email = p.email ?: "—",
                            lastVisit = p.createdAt?.take(10),
                            status = if (p.isActive) PatientStatus.ACTIVE else PatientStatus.INACTIVE,
                        )
                    },
                )
            } catch (e: Exception) {
                _error.value = e.message
            }
        }
    }

    /** Real check-in against the appointments API. */
    fun checkInPatient(checkInId: String) {
        viewModelScope.launch {
            try {
                appointmentsRepository.checkIn(checkInId)
                loadDashboardData()
            } catch (e: Exception) {
                _error.value = e.message ?: "Check-in failed"
            }
        }
    }

    /** Check-out = completing the appointment. */
    fun checkOutPatient(checkInId: String) {
        viewModelScope.launch {
            try {
                appointmentsRepository.complete(checkInId)
                loadDashboardData()
            } catch (e: Exception) {
                _error.value = e.message ?: "Check-out failed"
            }
        }
    }

    private fun greeting(): String {
        val hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
        return when {
            hour < 12 -> "Good morning"
            hour < 17 -> "Good afternoon"
            else -> "Good evening"
        }
    }

    private fun todayDate(): String {
        val sdf = SimpleDateFormat("EEEE, MMM d", Locale.US)
        return sdf.format(Calendar.getInstance().time)
    }
}
