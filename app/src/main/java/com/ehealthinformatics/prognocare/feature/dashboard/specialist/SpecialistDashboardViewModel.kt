package com.ehealthinformatics.prognocare.feature.dashboard.specialist

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ehealthinformatics.prognocare.data.auth.SessionStore
import com.ehealthinformatics.prognocare.feature.appointments.ApiException
import com.ehealthinformatics.prognocare.feature.appointments.AppointmentQuery
import com.ehealthinformatics.prognocare.feature.appointments.AppointmentsRepository
import com.ehealthinformatics.prognocare.feature.records.EmrRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import javax.inject.Inject

/** Direction the specialist is viewing: addressed to me, or sent by me. */
enum class ReferralDirection(val label: String) {
    INCOMING("Incoming"),
    OUTGOING("Outgoing"),
}

sealed class ReferralUiEvent {
    data class Success(val message: String) : ReferralUiEvent()
    data class Error(val message: String) : ReferralUiEvent()
}

data class CreateReferralState(
    val encounters: List<com.ehealthinformatics.prognocare.data.remote.models.Encounter> = emptyList(),
    val selectedEncounter: com.ehealthinformatics.prognocare.data.remote.models.Encounter? = null,
    val specialists: List<com.ehealthinformatics.prognocare.data.remote.models.Staff> = emptyList(),
    val selectedSpecialist: com.ehealthinformatics.prognocare.data.remote.models.Staff? = null,
    val isLoadingScope: Boolean = false,
    val reason: String = "",
    val priority: String = "ROUTINE",
    val isSaving: Boolean = false,
)

@HiltViewModel
class SpecialistDashboardViewModel @Inject constructor(
    private val appointmentsRepository: AppointmentsRepository,
    private val emrRepository: EmrRepository,
    @ApplicationContext private val context: Context,
) : ViewModel() {

    private val _state = MutableStateFlow(SpecialistDashboardState())
    val state: StateFlow<SpecialistDashboardState> = _state.asStateFlow()

    private val _referrals = MutableStateFlow<List<com.ehealthinformatics.prognocare.data.remote.models.Referral>>(emptyList())
    val referrals: StateFlow<List<com.ehealthinformatics.prognocare.data.remote.models.Referral>> = _referrals.asStateFlow()

    private val _referralsLoading = MutableStateFlow(true)
    val referralsLoading: StateFlow<Boolean> = _referralsLoading.asStateFlow()

    private val _referralsError = MutableStateFlow<String?>(null)
    val referralsError: StateFlow<String?> = _referralsError.asStateFlow()

    private val _direction = MutableStateFlow(ReferralDirection.INCOMING)
    val direction: StateFlow<ReferralDirection> = _direction.asStateFlow()

    private val _statusFilter = MutableStateFlow<String?>(null)
    val statusFilter: StateFlow<String?> = _statusFilter.asStateFlow()

    private val _createState = MutableStateFlow(CreateReferralState())
    val createState: StateFlow<CreateReferralState> = _createState.asStateFlow()

    private val _referralDetail = MutableStateFlow<com.ehealthinformatics.prognocare.data.remote.models.Referral?>(null)
    val referralDetail: StateFlow<com.ehealthinformatics.prognocare.data.remote.models.Referral?> = _referralDetail.asStateFlow()

    private val _detailLoading = MutableStateFlow(false)
    val detailLoading: StateFlow<Boolean> = _detailLoading.asStateFlow()

    private val _events = MutableSharedFlow<ReferralUiEvent>(extraBufferCapacity = 1)
    val events: SharedFlow<ReferralUiEvent> = _events.asSharedFlow()

    init {
        loadDashboardData()
        loadReferrals()
    }

    fun retry() = loadDashboardData()

    fun refresh() {
        loadDashboardData()
        loadReferrals()
    }

    private fun loadDashboardData() {
        viewModelScope.launch {
            try {
                val today = LocalDate.now()
                val staffId = SessionStore.getStaffId(context)

                val myAppointments = appointmentsRepository.list(
                    AppointmentQuery(providerId = staffId, limit = 50, defaultToday = true),
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

                _state.value = _state.value.copy(
                    greeting = when (LocalDateTime.now().hour) {
                        in 5..11 -> "Good morning"
                        in 12..16 -> "Good afternoon"
                        else -> "Good evening"
                    },
                    specialistName = SessionStore.getStaffName(context) ?: "Specialist",
                    specialty = "Specialist Clinic",
                    todayDate = today.format(DateTimeFormatter.ofPattern("EEEE, MMM d")),
                    activePatients = summary?.metrics?.activeVisits ?: myEncounters.size,
                    completedReviews = summary?.metrics?.completed ?: 0,
                    urgentCases = myAppointments.count {
                        it.isUrgent && it.status !in listOf("COMPLETED", "CANCELLED", "NO_SHOW")
                    },
                    upcomingConsultations = consultations,
                    isLoading = false,
                )
            } catch (e: Exception) {
                _state.value = _state.value.copy(isLoading = false, error = e.message ?: "Failed to load")
            }
        }
    }

    /** Loads referrals self-scoped to the signed-in specialist/referrer. */
    fun loadReferrals(
        direction: ReferralDirection = _direction.value,
        status: String? = _statusFilter.value,
    ) {
        viewModelScope.launch {
            _referralsLoading.value = true
            _referralsError.value = null
            _direction.value = direction
            _statusFilter.value = status
            try {
                val staffId = SessionStore.getStaffId(context)
                val list = emrRepository.referrals(
                    status = status,
                    direction = direction.name.lowercase(),
                    providerId = staffId,
                    limit = 100,
                )
                _referrals.value = list
                if (direction == ReferralDirection.INCOMING) {
                    _state.value = _state.value.copy(
                        pendingReferrals = list.count { it.status == "PENDING" },
                    )
                }
            } catch (e: Exception) {
                _referralsError.value = e.message ?: "Failed to load referrals"
            } finally {
                _referralsLoading.value = false
            }
        }
    }

    fun setDirection(direction: ReferralDirection) = loadReferrals(direction, _statusFilter.value)

    fun setStatusFilter(status: String?) = loadReferrals(_direction.value, status)

    /** Loads one referral for the detail screen. */
    fun loadReferral(referralId: String) {
        viewModelScope.launch {
            _detailLoading.value = true
            try {
                _referralDetail.value = emrRepository.referral(referralId)
            } catch (e: Exception) {
                _referralDetail.value = null
                _events.emit(ReferralUiEvent.Error(e.message ?: "Failed to load referral"))
            } finally {
                _detailLoading.value = false
            }
        }
    }

    /** Specialist decision on a pending referral (accept/decline). */
    fun decide(referralId: String, decision: String, reason: String? = null) {
        viewModelScope.launch {
            try {
                emrRepository.decideReferral(
                    referralId,
                    com.ehealthinformatics.prognocare.data.remote.models.DecideReferralDto(
                        decision = decision,
                        reason = reason?.takeIf { it.isNotBlank() },
                    ),
                )
                _events.emit(ReferralUiEvent.Success("Referral ${decision.lowercase()}"))
                loadReferrals()
            } catch (e: ApiException) {
                _events.emit(ReferralUiEvent.Error(e.message ?: "Decision failed"))
            } catch (e: Exception) {
                _events.emit(ReferralUiEvent.Error(e.message ?: "Decision failed"))
            }
        }
    }

    /** Mark an accepted referral completed after the consultation. */
    fun complete(referralId: String, outcomeNotes: String? = null) {
        viewModelScope.launch {
            try {
                emrRepository.completeReferral(
                    referralId,
                    com.ehealthinformatics.prognocare.data.remote.models.CompleteReferralDto(
                        outcomeNotes = outcomeNotes?.takeIf { it.isNotBlank() },
                    ),
                )
                _events.emit(ReferralUiEvent.Success("Referral completed"))
                loadReferrals()
            } catch (e: Exception) {
                _events.emit(ReferralUiEvent.Error(e.message ?: "Could not complete referral"))
            }
        }
    }

    // ── Create referral (outgoing) ───────────────────────────────

    fun updateCreate(transform: (CreateReferralState) -> CreateReferralState) {
        _createState.value = transform(_createState.value)
    }

    /** Loads active encounters + specialist staff for the create dialog. */
    fun loadCreateScope() {
        if (_createState.value.encounters.isNotEmpty() || _createState.value.isLoadingScope) return
        viewModelScope.launch {
            _createState.value = _createState.value.copy(isLoadingScope = true)
            try {
                val encounters = runCatching {
                    emrRepository.encounters(limit = 100).filter { it.isActive }
                }.getOrDefault(emptyList())
                val specialists = runCatching {
                    emrRepository.staff(roleType = "Specialist", isActive = true, limit = 100)
                }.getOrDefault(emptyList())
                _createState.value = _createState.value.copy(
                    encounters = encounters.distinctBy { it.patientId },
                    specialists = specialists,
                    isLoadingScope = false,
                )
            } catch (e: Exception) {
                _events.emit(ReferralUiEvent.Error(e.message ?: "Could not load referral targets"))
                _createState.value = _createState.value.copy(isLoadingScope = false)
            }
        }
    }

    fun createReferral() {
        val current = _createState.value
        val encounter = current.selectedEncounter
        val specialist = current.selectedSpecialist
        if (encounter == null) {
            viewModelScope.launch { _events.emit(ReferralUiEvent.Error("Select an encounter first")) }
            return
        }
        if (specialist == null) {
            viewModelScope.launch { _events.emit(ReferralUiEvent.Error("Select a specialist")) }
            return
        }
        if (current.reason.isBlank()) {
            viewModelScope.launch { _events.emit(ReferralUiEvent.Error("Enter the referral reason")) }
            return
        }
        if (current.isSaving) return
        viewModelScope.launch {
            _createState.value = current.copy(isSaving = true)
            try {
                emrRepository.createReferral(
                    com.ehealthinformatics.prognocare.data.remote.models.CreateReferralDto(
                        patientId = encounter.patientId,
                        encounterId = encounter.id,
                        visitId = encounter.visitId,
                        specialistProviderId = specialist.id,
                        specialistProviderName = specialist.displayName,
                        specialty = specialist.department,
                        reason = current.reason.trim(),
                        priority = current.priority,
                    ),
                )
                _events.emit(ReferralUiEvent.Success("Referral sent to ${specialist.displayName}"))
                _createState.value = CreateReferralState()
                loadReferrals(ReferralDirection.OUTGOING, null)
            } catch (e: Exception) {
                _events.emit(ReferralUiEvent.Error(e.message ?: "Could not create referral"))
            } finally {
                _createState.value = _createState.value.copy(isSaving = false)
            }
        }
    }
}
