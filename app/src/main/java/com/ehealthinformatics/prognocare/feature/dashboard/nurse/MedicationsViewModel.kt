package com.ehealthinformatics.prognocare.feature.dashboard.nurse

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ehealthinformatics.prognocare.data.auth.SessionStore
import com.ehealthinformatics.prognocare.data.remote.models.AdministerMedicationDto
import com.ehealthinformatics.prognocare.data.remote.models.ClinicalRequest
import com.ehealthinformatics.prognocare.data.remote.models.CreateMedicationDto
import com.ehealthinformatics.prognocare.data.remote.models.Medication
import com.ehealthinformatics.prognocare.feature.appointments.ApiException
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
import javax.inject.Inject

sealed class MedicationUiEvent {
    data class Success(val message: String) : MedicationUiEvent()
    data class Error(val message: String) : MedicationUiEvent()
}

/**
 * One row in the create-from-prescription picker: a PRESCRIPTION request item
 * that has not been converted to a medication record yet.
 */
data class PrescriptionItemUi(
    val requestId: String,
    val itemIndex: Int,
    val patientName: String,
    val name: String,
    val dose: String,
    val route: String,
    val frequency: String,
)

data class MedicationsState(
    val medications: List<Medication> = emptyList(),
    val prescriptions: List<PrescriptionItemUi> = emptyList(),
    val isLoading: Boolean = true,
    val error: String? = null,
)

@HiltViewModel
class MedicationsViewModel @Inject constructor(
    private val emrRepository: EmrRepository,
    @ApplicationContext private val context: Context,
) : ViewModel() {

    private val _state = MutableStateFlow(MedicationsState())
    val state: StateFlow<MedicationsState> = _state.asStateFlow()

    private val _events = MutableSharedFlow<MedicationUiEvent>(extraBufferCapacity = 1)
    val events: SharedFlow<MedicationUiEvent> = _events.asSharedFlow()

    init {
        load()
    }

    /** Pull-to-refresh entry point: reloads medications + open prescriptions. */
    fun refresh() = load()

    fun load() {
        viewModelScope.launch {
            _state.value = _state.value.copy(isLoading = true, error = null)
            try {
                val medications = emrRepository.medications(limit = 200)
                val prescriptions = loadOpenPrescriptions(medications)
                _state.value = _state.value.copy(
                    medications = medications,
                    prescriptions = prescriptions,
                    isLoading = false,
                )
            } catch (e: Exception) {
                _state.value = _state.value.copy(isLoading = false, error = e.message ?: "Failed to load")
            }
        }
    }

    /**
     * Open PRESCRIPTION requests mapped to per-item rows; items that already
     * have a live medication record are hidden (the API enforces convert-once).
     */
    private suspend fun loadOpenPrescriptions(medications: List<Medication>): List<PrescriptionItemUi> {
        return runCatching {
            val requests = emrRepository.requests(requestType = "PRESCRIPTION", limit = 100)
                .filter { it.status == "REQUESTED" || it.status == "IN_PROGRESS" }
            val converted = medications
                .filter { !it.isCancelled }
                .mapNotNull { med -> med.requestId?.let { requestId -> "$requestId#${med.name.lowercase()}" } }
                .toSet()
            requests.flatMap { request -> request.items.mapIndexedNotNull { index, item ->
                if (item.name.isBlank()) return@mapIndexedNotNull null
                if ("${request.id}#${item.name.lowercase()}" in converted) return@mapIndexedNotNull null
                PrescriptionItemUi(
                    requestId = request.id,
                    itemIndex = index,
                    patientName = request.patientName,
                    name = item.name,
                    dose = listOfNotNull(item.dose, item.doseUnit).joinToString(" ").ifBlank { "—" },
                    route = item.route ?: "—",
                    frequency = item.frequency ?: "—",
                )
            } }
        }.getOrDefault(emptyList())
    }

    /** Nurse converts one prescription item into a medication record. */
    fun createFromPrescription(item: PrescriptionItemUi) {
        viewModelScope.launch {
            try {
                emrRepository.createMedication(
                    CreateMedicationDto(
                        requestId = item.requestId,
                        itemIndex = item.itemIndex,
                    ),
                )
                _events.emit(MedicationUiEvent.Success("${item.name} added to medication list"))
                load()
            } catch (e: ApiException) {
                _events.emit(MedicationUiEvent.Error(e.message ?: "Could not create medication"))
            } catch (e: Exception) {
                _events.emit(MedicationUiEvent.Error(e.message ?: "Could not create medication"))
            }
        }
    }

    /** Records administration; the backend completes the source request. */
    fun administer(medicationId: String, notes: String? = null, outcome: String? = null) {
        viewModelScope.launch {
            try {
                val staffName = SessionStore.getStaffName(context)
                emrRepository.administerMedication(
                    medicationId,
                    AdministerMedicationDto(
                        administeredAt = java.time.OffsetDateTime.now().toString(),
                        notes = notes?.takeIf { it.isNotBlank() },
                        outcome = outcome,
                    ),
                )
                _events.emit(
                    MedicationUiEvent.Success(
                        "Administered" + (staffName?.let { " by $it" } ?: ""),
                    ),
                )
                load()
            } catch (e: ApiException) {
                _events.emit(MedicationUiEvent.Error(e.message ?: "Could not record administration"))
            } catch (e: Exception) {
                _events.emit(MedicationUiEvent.Error(e.message ?: "Could not record administration"))
            }
        }
    }
}
