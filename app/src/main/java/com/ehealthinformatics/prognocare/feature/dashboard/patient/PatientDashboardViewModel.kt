package com.ehealthinformatics.prognocare.feature.dashboard.patient

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
class PatientDashboardViewModel @Inject constructor(
    private val appointmentsRepository: AppointmentsRepository,
    private val emrRepository: EmrRepository,
    @ApplicationContext private val context: Context,
) : ViewModel() {

    private val _state = MutableStateFlow(PatientDashboardState())
    val state: StateFlow<PatientDashboardState> = _state.asStateFlow()

    init {
        loadDashboardData()
    }

    fun retry() = loadDashboardData()

    private fun loadDashboardData() {
        viewModelScope.launch {
            val mrn = SessionStore.getPatientId(context)
            if (mrn == null) {
                _state.value = PatientDashboardState(
                    isLoading = false,
                    shouldShowProfilePrompt = true,
                    tagline = "Link your patient record to see your health data",
                )
                return@launch
            }
            _state.value = _state.value.copy(isLoading = true, error = null)
            try {
                val today = LocalDate.now().toString()
                val appointmentList = appointmentsRepository.list(
                    AppointmentQuery(patientId = mrn, limit = 50),
                )
                val requestList = runCatching {
                    emrRepository.requests(patientId = mrn, limit = 50)
                }.getOrDefault(emptyList())
                val records = runCatching {
                    emrRepository.encounters(patientId = mrn, limit = 50)
                }.getOrDefault(emptyList())

                val activeStatuses = listOf("SCHEDULED", "CHECKED_IN", "IN_PROGRESS")
                val upcoming = appointmentList.filter {
                    it.date >= today && it.status in activeStatuses
                }.sortedBy { it.date + it.startTime }

                val rxRequests = requestList.filter { it.requestType == "PRESCRIPTION" }
                val openRx = rxRequests.filter { it.isOpen }

                val nextAppointment = upcoming.firstOrNull()?.let { apt ->
                    PatientAppointment(
                        id = apt.id,
                        providerName = apt.providerName ?: "—",
                        providerSpecialty = apt.providerName?.let { "Provider" } ?: "—",
                        type = apt.typeDisplay,
                        date = apt.date,
                        time = apt.startTime,
                        location = apt.scheduleLocation ?: "—",
                        status = apt.status,
                        reason = apt.reason,
                    )
                }

                _state.value = PatientDashboardState(
                    greeting = when (LocalDateTime.now().hour) {
                        in 5..11 -> "Good morning"
                        in 12..16 -> "Good afternoon"
                        else -> "Good evening"
                    },
                    patientName = SessionStore.getPatientName(context)
                        ?: appointmentList.firstOrNull()?.patientName
                        ?: "Patient",
                    mrn = mrn,
                    todayDate = LocalDate.now().format(DateTimeFormatter.ofPattern("EEEE, MMM d")),
                    tagline = "Take charge of your health",
                    upcomingAppointments = upcoming.size,
                    activeMedications = openRx.sumOf { it.items.size },
                    labResults = records.size,
                    healthScore = 0,
                    healthScoreLabel = "—",
                    nextAppointment = nextAppointment,
                    recentAppointments = appointmentList
                        .filter { it.date < today || it.status !in activeStatuses }
                        .take(5)
                        .map { apt ->
                            PatientAppointment(
                                id = apt.id,
                                providerName = apt.providerName ?: "—",
                                providerSpecialty = "",
                                type = apt.typeDisplay,
                                date = apt.date,
                                time = apt.startTime,
                                location = apt.scheduleLocation ?: "—",
                                status = apt.status,
                                reason = apt.reason,
                            )
                        },
                    currentMedications = openRx.flatMap { rx ->
                        rx.items.map { item ->
                            PatientMedication(
                                id = item.id ?: "${rx.id}-${item.name}",
                                name = item.name,
                                dosage = listOfNotNull(item.dose, item.doseUnit).joinToString(" "),
                                frequency = item.frequency ?: "—",
                                route = item.route ?: "—",
                                prescriber = rx.orderingProviderName ?: "—",
                                startDate = rx.requestedAt?.take(10) ?: "—",
                                instructions = item.instructions ?: "",
                                nextDose = "",
                            )
                        }
                    },
                    recentRecords = records.take(5).map { enc ->
                        PatientRecord(
                            id = enc.id,
                            title = enc.typeDisplay,
                            type = "ENCOUNTER",
                            date = enc.encounterDatetime?.take(10) ?: "—",
                            providerName = enc.providerName ?: "—",
                            summary = enc.reason ?: "",
                        )
                    },
                    healthAlerts = emptyList(),
                    shouldShowProfilePrompt = false,
                    isLoading = false,
                )
            } catch (e: Exception) {
                _state.value = _state.value.copy(isLoading = false, error = e.message ?: "Failed to load")
            }
        }
    }
}
