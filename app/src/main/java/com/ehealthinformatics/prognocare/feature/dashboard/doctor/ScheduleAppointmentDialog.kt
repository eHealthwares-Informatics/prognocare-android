package com.ehealthinformatics.prognocare.feature.dashboard.doctor

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import com.ehealthinformatics.prognocare.data.location.LocationScope
import com.ehealthinformatics.prognocare.data.remote.models.AppointmentPriority
import com.ehealthinformatics.prognocare.data.remote.models.Location
import com.ehealthinformatics.prognocare.data.remote.models.AppointmentType
import com.ehealthinformatics.prognocare.data.remote.models.CreateAppointmentDto
import com.ehealthinformatics.prognocare.data.remote.models.Patient
import com.ehealthinformatics.prognocare.data.remote.models.Staff
import com.ehealthinformatics.prognocare.designsystem.theme.Spacing
import com.ehealthinformatics.prognocare.feature.appointments.AppointmentsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class ScheduleFormState(
    val patients: List<Patient> = emptyList(),
    val providers: List<Staff> = emptyList(),
    val isSearchingPatients: Boolean = false,
    val selectedPatient: Patient? = null,
    val selectedProvider: Staff? = null,
    val locations: List<Location> = emptyList(),
    val selectedLocationId: String? = null,
    val appointmentType: String = AppointmentType.CONSULTATION.name,
    val priority: String = AppointmentPriority.ROUTINE.name,
    val date: String = "",
    val startTime: String = "09:00",
    val reason: String = "",
    val isBooking: Boolean = false,
    val error: String? = null,
)

@HiltViewModel
class ScheduleAppointmentViewModel @Inject constructor(
    private val repository: AppointmentsRepository,
    private val locationScope: LocationScope,
) : ViewModel() {

    private val _state = MutableStateFlow(ScheduleFormState())
    val state: StateFlow<ScheduleFormState> = _state.asStateFlow()

    init {
        viewModelScope.launch {
            _state.update { it.copy(date = repository.today()) }
            loadProviders()
            // Preselect the user's active location scope; the picker below
            // can change or clear it per appointment.
            _state.update { it.copy(selectedLocationId = locationScope.current().id) }
            runCatching { locationScope.locations(limit = 20) }
                .onSuccess { locations -> _state.update { it.copy(locations = locations) } }
        }
    }

    fun setLocation(locationId: String?) = _state.update { it.copy(selectedLocationId = locationId) }

    fun searchPatients(query: String) {
        viewModelScope.launch {
            _state.update { it.copy(isSearchingPatients = true) }
            try {
                val patients = repository.searchPatients(query)
                _state.update { it.copy(patients = patients, isSearchingPatients = false) }
            } catch (e: Exception) {
                _state.update {
                    it.copy(patients = emptyList(), isSearchingPatients = false, error = e.message)
                }
            }
        }
    }

    fun selectPatient(patient: Patient) = _state.update { it.copy(selectedPatient = patient) }
    fun clearPatient() = _state.update { it.copy(selectedPatient = null) }
    fun selectProvider(provider: Staff) = _state.update { it.copy(selectedProvider = provider) }
    fun clearProvider() = _state.update { it.copy(selectedProvider = null) }
    fun setType(type: String) = _state.update { it.copy(appointmentType = type) }
    fun setPriority(priority: String) = _state.update { it.copy(priority = priority) }
    fun setDate(date: String) = _state.update { it.copy(date = date) }
    fun setStartTime(time: String) = _state.update { it.copy(startTime = time) }
    fun setReason(reason: String) = _state.update { it.copy(reason = reason) }

    fun book(onScheduled: () -> Unit) {
        val snapshot = _state.value
        val patient = snapshot.selectedPatient ?: run {
            _state.update { it.copy(error = "Select a patient first") }
            return
        }
        if (snapshot.date.isBlank()) {
            _state.update { it.copy(error = "Pick a date (yyyy-MM-dd)") }
            return
        }
        viewModelScope.launch {
            _state.update { it.copy(isBooking = true, error = null) }
            try {
                repository.create(
                    CreateAppointmentDto(
                        patientId = patient.patientId.ifBlank { patient.id },
                        patientName = patient.displayName,
                        appointmentType = snapshot.appointmentType,
                        date = snapshot.date,
                        startTime = snapshot.startTime,
                        providerId = snapshot.selectedProvider?.id,
                        providerName = snapshot.selectedProvider?.displayName,
                        locationId = snapshot.selectedLocationId,
                        priority = snapshot.priority,
                        reason = snapshot.reason.takeIf { it.isNotBlank() },
                    ),
                )
                _state.update { it.copy(isBooking = false) }
                onScheduled()
            } catch (e: Exception) {
                _state.update { it.copy(isBooking = false, error = e.message) }
            }
        }
    }

    private suspend fun loadProviders() {
        try {
            _state.update { it.copy(providers = repository.searchProviders()) }
        } catch (_: Exception) {
            // Provider list is optional for scheduling
        }
    }
}

private val TIME_SLOTS = listOf(
    "08:00", "08:30", "09:00", "09:30", "10:00", "10:30",
    "11:00", "11:30", "12:00", "14:00", "14:30", "15:00",
    "15:30", "16:00", "16:30", "17:00",
)

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun ScheduleAppointmentDialog(
    onDismiss: () -> Unit,
    onScheduled: () -> Unit,
    viewModel: ScheduleAppointmentViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    var patientQuery by remember { mutableStateOf("") }

    LaunchedEffect(Unit) { viewModel.searchPatients("") }
    LaunchedEffect(patientQuery) { viewModel.searchPatients(patientQuery) }

    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = Spacing.lg)
                .padding(bottom = Spacing.xl),
            verticalArrangement = Arrangement.spacedBy(Spacing.sm),
        ) {
            Text(
                "Schedule Appointment",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.SemiBold,
            )

            // Patient
            if (state.selectedPatient == null) {
                OutlinedTextField(
                    value = patientQuery,
                    onValueChange = { patientQuery = it },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Patient") },
                    placeholder = { Text("Search by name or MRN") },
                    singleLine = true,
                )
                if (state.isSearchingPatients) {
                    CircularProgressIndicator(Modifier.fillMaxWidth().heightIn(min = 24.dp))
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxWidth().heightIn(max = 180.dp),
                        contentPadding = PaddingValues(vertical = Spacing.xxs),
                    ) {
                        items(state.patients) { patient ->
                            TextButton(onClick = { viewModel.selectPatient(patient) }) {
                                Text("${patient.displayName} · ${patient.patientId}")
                            }
                        }
                    }
                }
            } else {
                TextButton(onClick = viewModel::clearPatient) {
                    Text("Patient: ${state.selectedPatient?.displayName} · change")
                }
            }

            // Provider
            FlowRow(horizontalArrangement = Arrangement.spacedBy(Spacing.xs)) {
                FilterChip(
                    selected = state.selectedProvider == null,
                    onClick = viewModel::clearProvider,
                    label = { Text("No provider") },
                )
                state.providers.forEach { provider ->
                    FilterChip(
                        selected = state.selectedProvider?.id == provider.id,
                        onClick = { viewModel.selectProvider(provider) },
                        label = { Text(provider.displayName) },
                    )
                }
            }

            // Type chips
            Text("Type", style = MaterialTheme.typography.labelLarge)
            FlowRow(horizontalArrangement = Arrangement.spacedBy(Spacing.xs)) {
                AppointmentType.entries.forEach { type ->
                    FilterChip(
                        selected = state.appointmentType == type.name,
                        onClick = { viewModel.setType(type.name) },
                        label = { Text(type.name.replace('_', ' ').lowercase()) },
                    )
                }
            }

            // Priority chips
            Text("Priority", style = MaterialTheme.typography.labelLarge)
            FlowRow(horizontalArrangement = Arrangement.spacedBy(Spacing.xs)) {
                AppointmentPriority.entries.forEach { priority ->
                    FilterChip(
                        selected = state.priority == priority.name,
                        onClick = { viewModel.setPriority(priority.name) },
                        label = { Text(priority.name.lowercase()) },
                    )
                }
            }

            // Location
            Text("Location", style = MaterialTheme.typography.labelLarge)
            FlowRow(horizontalArrangement = Arrangement.spacedBy(Spacing.xs)) {
                FilterChip(
                    selected = state.selectedLocationId == null,
                    onClick = { viewModel.setLocation(null) },
                    label = { Text("No location") },
                )
                state.locations.forEach { location ->
                    FilterChip(
                        selected = state.selectedLocationId == location.id,
                        onClick = { viewModel.setLocation(location.id) },
                        label = { Text(location.name) },
                    )
                }
            }

            // Date & time
            OutlinedTextField(
                value = state.date,
                onValueChange = viewModel::setDate,
                modifier = Modifier.fillMaxWidth(),
                label = { Text("Date (yyyy-MM-dd)") },
                singleLine = true,
            )
            Text("Start time", style = MaterialTheme.typography.labelLarge)
            FlowRow(horizontalArrangement = Arrangement.spacedBy(Spacing.xs)) {
                TIME_SLOTS.forEach { slot ->
                    FilterChip(
                        selected = state.startTime == slot,
                        onClick = { viewModel.setStartTime(slot) },
                        label = { Text(formatTime(slot)) },
                    )
                }
            }

            OutlinedTextField(
                value = state.reason,
                onValueChange = viewModel::setReason,
                modifier = Modifier.fillMaxWidth(),
                label = { Text("Reason (optional)") },
                singleLine = true,
            )

            state.error?.let { Text(it, color = MaterialTheme.colorScheme.error) }

            Row(horizontalArrangement = Arrangement.End, modifier = Modifier.fillMaxWidth()) {
                TextButton(onClick = onDismiss) { Text("Cancel") }
                Button(
                    onClick = { viewModel.book(onScheduled) },
                    enabled = !state.isBooking && state.selectedPatient != null,
                ) {
                    Text(if (state.isBooking) "Scheduling…" else "Schedule")
                }
            }
        }
    }
}
