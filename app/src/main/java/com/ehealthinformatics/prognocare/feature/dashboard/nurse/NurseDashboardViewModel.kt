package com.ehealthinformatics.prognocare.feature.dashboard.nurse

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ehealthinformatics.prognocare.data.auth.SessionStore
import com.ehealthinformatics.prognocare.data.remote.RetrofitClient
import com.ehealthinformatics.prognocare.feature.appointments.AppointmentQuery
import com.ehealthinformatics.prognocare.feature.appointments.AppointmentsRepository
import com.ehealthinformatics.prognocare.feature.forms.VitalsReadings
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
    private val retrofitClient: RetrofitClient,
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
        // Keep previous numbers visible while refreshing so the UI does not flash zeros.
        val refreshing = _state.value.isLoading || _state.value.taskQueue.isNotEmpty()
        _state.value = _state.value.copy(
            isLoading = !refreshing,
            isRefreshing = refreshing,
            error = null,
        )
        viewModelScope.launch {
            try {
                // Appointment window: explicit filter → settings date range → today.
                val todayAppointments = appointmentsRepository.list(
                    AppointmentQuery(limit = 100, defaultToday = true),
                )

                // VITALS documentation submissions (patient MRN keyed).
                val vitalsSubmissions = runCatching {
                    emrRepository.formSubmissions(limit = 100)
                }.getOrDefault(emptyList())
                    .filter { it.formName.contains("vital", ignoreCase = true) }
                    .filter { it.status != "DRAFT" }

                val vitalsByMrn = vitalsSubmissions
                    .groupBy { it.patientId }
                val vitalsMrns = vitalsByMrn.keys

                // Open PRESCRIPTION requests + due medications.
                val medRequests = runCatching {
                    emrRepository.requests(requestType = "PRESCRIPTION", limit = 50)
                }.getOrDefault(emptyList())
                val dueMeds = runCatching {
                    emrRepository.medications(limit = 100)
                }.getOrDefault(emptyList()).filter { it.isDue }

                val checkedInStatuses = listOf("CHECKED_IN", "IN_PROGRESS")
                val openStatuses = listOf("REQUESTED", "IN_PROGRESS")
                val closedStatuses = listOf("COMPLETED", "CANCELLED", "NO_SHOW")

                val checkedInAppts = todayAppointments.filter { it.status in checkedInStatuses }
                val completedAppts = todayAppointments.filter { it.status == "COMPLETED" }

                // Vitals pending = checked-in patients without a VITALS submission.
                val vitalsPending = checkedInAppts.count { apt ->
                    apt.patientId !in vitalsMrns
                }

                val medsDue = if (dueMeds.isNotEmpty()) {
                    dueMeds.size
                } else {
                    medRequests.count { it.isOpen }
                }

                val checkIns = todayAppointments.map { apt ->
                    NurseCheckIn(
                        id = apt.id,
                        patientName = apt.patientName,
                        patientId = apt.patientId,
                        appointmentTime = apt.startTime,
                        appointmentType = apt.typeDisplay,
                        providerName = apt.providerName ?: "—",
                        isCheckedIn = apt.status in checkedInStatuses ||
                            apt.status == "COMPLETED",
                        vitalsComplete = apt.patientId in vitalsMrns,
                    )
                }

                val tasks = todayAppointments
                    .filter { it.status !in closedStatuses }
                    .map { apt ->
                        val vitalsDone = apt.patientId in vitalsMrns
                        NurseTask(
                            id = apt.id,
                            patientName = apt.patientName,
                            patientId = apt.patientId,
                            taskType = if (apt.status in checkedInStatuses) {
                                if (vitalsDone) NurseTaskType.MEDICATION else NurseTaskType.VITALS
                            } else {
                                NurseTaskType.CHECK_IN
                            },
                            description = when {
                                apt.status in checkedInStatuses && !vitalsDone ->
                                    "Record pre-consultation vitals"
                                apt.status in checkedInStatuses && vitalsDone ->
                                    "Awaiting clinician / meds"
                                else -> "Check in for ${apt.typeDisplay.lowercase()}"
                            },
                            priority = if (apt.isUrgent) TaskPriority.URGENT else TaskPriority.NORMAL,
                            scheduledTime = apt.startTime,
                            status = when (apt.status) {
                                "IN_PROGRESS" -> TaskStatus.IN_PROGRESS
                                "CHECKED_IN" ->
                                    if (vitalsDone) TaskStatus.COMPLETED else TaskStatus.IN_PROGRESS
                                else -> TaskStatus.PENDING
                            },
                        )
                    }

                // Recent vitals → display rows (newest first, cap 10).
                // Resolve patient names so the list never shows raw MRNs/UUIDs.
                val patientNames = runCatching {
                    val resp = retrofitClient.apis.value.patientApi.list(page = 1, limit = 200)
                    if (resp.isSuccessful) {
                        resp.body()?.data.orEmpty().associate { p ->
                            (p.patientId.ifBlank { p.id }) to p.displayName
                        }
                    } else {
                        emptyMap<String, String>()
                    }
                }.getOrDefault(emptyMap())

                val recentVitals = vitalsSubmissions
                    .sortedByDescending { it.submittedAt ?: it.createdAt.orEmpty() }
                    .take(10)
                    .mapNotNull { submission ->
                        val readings = VitalsReadings.from(submission) ?: return@mapNotNull null
                        val bp = readings.bloodPressure
                        val bpParts = bp?.split("/")?.map { it.trim() }
                        val mrn = submission.patientId
                        VitalsRecord(
                            id = submission.id,
                            patientName = patientNames[mrn] ?: mrn.ifBlank { "Patient" },
                            recordedAt = (submission.submittedAt ?: submission.createdAt ?: "")
                                .take(16)
                                .replace('T', ' '),
                            temperature = readings.temperature,
                            bloodPressureSystolic = bpParts?.getOrNull(0)?.takeWhile { it.isDigit() },
                            bloodPressureDiastolic = bpParts?.getOrNull(1)?.takeWhile { it.isDigit() },
                            heartRate = readings.heartRate,
                            respiratoryRate = readings.respiratoryRate,
                            oxygenSaturation = readings.oxygenSaturation,
                            weight = readings.weight,
                            height = readings.height,
                            recordedBy = readings.recordedBy.orEmpty(),
                        )
                    }

                val pendingTasks = tasks.count { it.status != TaskStatus.COMPLETED }

                _state.value = NurseDashboardState(
                    greeting = when (LocalDateTime.now().hour) {
                        in 5..11 -> "Good morning"
                        in 12..16 -> "Good afternoon"
                        else -> "Good evening"
                    },
                    nurseName = SessionStore.getStaffName(context) ?: "Nurse",
                    todayDate = LocalDate.now().format(DateTimeFormatter.ofPattern("EEEE, MMM d")),
                    patientsCheckedIn = checkedInAppts.size,
                    vitalsToRecord = vitalsPending,
                    medsToAdminister = medsDue,
                    pendingTasks = pendingTasks,
                    completedToday = completedAppts.size,
                    urgentTasks = tasks.count { it.priority == TaskPriority.URGENT },
                    taskQueue = tasks,
                    upcomingCheckIns = checkIns.filter { !it.isCheckedIn },
                    recentVitals = recentVitals,
                    isLoading = false,
                    isRefreshing = false,
                )
            } catch (e: Exception) {
                _state.value = _state.value.copy(
                    isLoading = false,
                    isRefreshing = false,
                    error = e.message ?: "Failed to load",
                )
            }
        }
    }
}
