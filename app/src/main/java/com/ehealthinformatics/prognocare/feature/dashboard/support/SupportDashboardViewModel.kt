package com.ehealthinformatics.prognocare.feature.dashboard.support

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ehealthinformatics.prognocare.data.auth.SessionStore
import com.ehealthinformatics.prognocare.feature.appointments.AppointmentQuery
import com.ehealthinformatics.prognocare.feature.appointments.AppointmentsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import javax.inject.Inject

/**
 * Support front-desk dashboard: today's appointment schedule drives the
 * check-in queue; tickets have no backend module and stay demo-tagged.
 */
@HiltViewModel
class SupportDashboardViewModel @Inject constructor(
    private val appointmentsRepository: AppointmentsRepository,
    @ApplicationContext private val context: Context,
) : ViewModel() {

    private val _state = MutableStateFlow(SupportDashboardState())
    val state: StateFlow<SupportDashboardState> = _state.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error = _error

    init {
        loadDashboardData()
    }

    fun retry() = loadDashboardData()

    private fun loadDashboardData() {
        viewModelScope.launch {
            try {
                _state.update { it.copy(isLoading = true) }
                _error.value = null
                val today = java.time.LocalDate.now().toString()
                val appointments = appointmentsRepository.list(AppointmentQuery(date = today, limit = 100))

                val queue = appointments.map { apt ->
                    SupportCheckIn(
                        id = apt.id,
                        patientName = apt.patientName,
                        patientMrn = apt.patientId,
                        appointmentTime = apt.startTime,
                        appointmentType = apt.typeDisplay,
                        doctorName = apt.providerName ?: "—",
                        status = when (apt.status) {
                            "CHECKED_IN" -> CheckInStatus.CHECKED_IN
                            "IN_PROGRESS" -> CheckInStatus.IN_SESSION
                            "COMPLETED" -> CheckInStatus.CHECKED_OUT
                            "NO_SHOW" -> CheckInStatus.NO_SHOW
                            else -> CheckInStatus.WAITING
                        },
                        phone = null,
                    )
                }

                _state.update {
                    it.copy(
                        isLoading = false,
                        greeting = getGreeting(),
                        todayDate = getTodayDate(),
                        supportName = SessionStore.getStaffName(context) ?: "Front Desk",
                        patientsWaiting = queue.count { c -> c.status == CheckInStatus.WAITING },
                        checkedInToday = queue.count { c ->
                            c.status in listOf(
                                CheckInStatus.CHECKED_IN,
                                CheckInStatus.IN_SESSION,
                                CheckInStatus.CHECKED_OUT,
                            )
                        },
                        activeRequests = 0, // tickets have no backend module yet
                        completedToday = queue.count { c -> c.status == CheckInStatus.CHECKED_OUT },
                        checkInQueue = queue,
                        recentRequests = emptyList(),
                    )
                }
            } catch (e: Exception) {
                _state.update { it.copy(isLoading = false) }
                _error.value = e.message ?: "Failed to load schedule"
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

    fun resolveRequest(requestId: String) {
        // Tickets have no backend module yet; no-op surfaced as demo data.
    }

    private fun getGreeting(): String {
        val hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
        return when {
            hour < 12 -> "Good morning"
            hour < 17 -> "Good afternoon"
            else -> "Good evening"
        }
    }

    private fun getTodayDate(): String {
        val sdf = SimpleDateFormat("EEEE, MMM d", Locale.US)
        return sdf.format(Calendar.getInstance().time)
    }
}
