package com.ehealthinformatics.prognocare.feature.dashboard.patient

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import com.ehealthinformatics.prognocare.data.auth.SessionStore
import com.ehealthinformatics.prognocare.data.remote.models.AppointmentPriority
import com.ehealthinformatics.prognocare.data.remote.models.AppointmentType
import com.ehealthinformatics.prognocare.data.remote.models.CreateAppointmentDto
import com.ehealthinformatics.prognocare.data.remote.models.Patient
import com.ehealthinformatics.prognocare.data.remote.models.Staff
import com.ehealthinformatics.prognocare.designsystem.theme.Spacing
import com.ehealthinformatics.prognocare.feature.appointments.AppointmentsRepository
import com.ehealthinformatics.prognocare.feature.dashboard.doctor.formatTime
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import android.content.Context
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

private val BOOKING_SLOTS = listOf(
    "09:00", "09:30", "10:00", "10:30", "11:00", "11:30",
    "13:00", "13:30", "14:00", "14:30", "15:00", "15:30", "16:00",
)

data class BookingFormState(
    val patientId: String? = null,
    val patientName: String? = null,
    val needsIdentity: Boolean = false,
    val identityResults: List<Patient> = emptyList(),
    val isSearchingIdentity: Boolean = false,
    val providers: List<Staff> = emptyList(),
    val selectedProvider: Staff? = null,
    val appointmentType: String = AppointmentType.CONSULTATION.name,
    val priority: String = AppointmentPriority.ROUTINE.name,
    val date: String = "",
    val startTime: String = "09:00",
    val reason: String = "",
    val isBooking: Boolean = false,
    val error: String? = null,
)

@HiltViewModel
class PatientBookingViewModel @Inject constructor(
    private val repository: AppointmentsRepository,
    @ApplicationContext private val context: Context,
) : ViewModel() {

    private val _state = MutableStateFlow(BookingFormState())
    val state: StateFlow<BookingFormState> = _state.asStateFlow()

    init {
        val storedId = SessionStore.getPatientId(context)
        if (storedId == null) {
            _state.value = BookingFormState(needsIdentity = true)
            searchIdentity("")
        } else {
            viewModelScope.launch {
                _state.update {
                    it.copy(
                        patientId = storedId,
                        patientName = SessionStore.getPatientName(context),
                        date = repository.today(),
                    )
                }
                loadProviders()
            }
        }
    }

    fun searchIdentity(query: String) {
        viewModelScope.launch {
            _state.update { it.copy(isSearchingIdentity = true) }
            try {
                _state.update {
                    it.copy(identityResults = repository.searchPatients(query), isSearchingIdentity = false)
                }
            } catch (e: Exception) {
                _state.update {
                    it.copy(identityResults = emptyList(), isSearchingIdentity = false, error = e.message)
                }
            }
        }
    }

    fun confirmIdentity(patient: Patient) {
        val mrn = patient.patientId.ifBlank { patient.id }
        if (mrn.isBlank()) return
        SessionStore.savePatient(context, mrn, patient.displayName)
        viewModelScope.launch {
            _state.update {
                it.copy(
                    needsIdentity = false,
                    patientId = mrn,
                    patientName = patient.displayName,
                    identityResults = emptyList(),
                    date = repository.today(),
                )
            }
            loadProviders()
        }
    }

    fun selectProvider(provider: Staff) = _state.update { it.copy(selectedProvider = provider) }
    fun setType(type: String) = _state.update { it.copy(appointmentType = type) }
    fun setPriority(priority: String) = _state.update { it.copy(priority = priority) }
    fun setDate(date: String) = _state.update { it.copy(date = date) }
    fun setStartTime(time: String) = _state.update { it.copy(startTime = time) }
    fun setReason(reason: String) = _state.update { it.copy(reason = reason) }

    fun book(onBooked: () -> Unit) {
        val snapshot = _state.value
        val patientId = snapshot.patientId ?: run {
            _state.update { it.copy(error = "Link your patient record first") }
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
                        patientId = patientId,
                        patientName = snapshot.patientName,
                        appointmentType = snapshot.appointmentType,
                        date = snapshot.date,
                        startTime = snapshot.startTime,
                        providerId = snapshot.selectedProvider?.id,
                        providerName = snapshot.selectedProvider?.displayName,
                        priority = snapshot.priority,
                        reason = snapshot.reason.takeIf { it.isNotBlank() },
                    ),
                )
                _state.update { it.copy(isBooking = false) }
                onBooked()
            } catch (e: Exception) {
                _state.update { it.copy(isBooking = false, error = e.message) }
            }
        }
    }

    private suspend fun loadProviders() {
        try {
            _state.update { it.copy(providers = repository.searchProviders()) }
        } catch (_: Exception) {
            // Provider list is optional
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun PatientAppointmentBookingScreen(
    onBack: () -> Unit,
    onBook: () -> Unit,
    viewModel: PatientBookingViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Book Appointment") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                ),
            )
        },
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = Spacing.lg),
            verticalArrangement = Arrangement.spacedBy(Spacing.md),
        ) {
            if (state.needsIdentity) {
                BookingIdentitySection(
                    state = state,
                    onSearch = viewModel::searchIdentity,
                    onSelect = viewModel::confirmIdentity,
                )
            } else {
                BookingForm(
                    state = state,
                    onSelectProvider = viewModel::selectProvider,
                    onSetType = viewModel::setType,
                    onSetPriority = viewModel::setPriority,
                    onSetDate = viewModel::setDate,
                    onSetStartTime = viewModel::setStartTime,
                    onSetReason = viewModel::setReason,
                )
            }

            state.error?.let { Text(it, color = MaterialTheme.colorScheme.error) }

            Button(
                onClick = { viewModel.book(onBook) },
                enabled = !state.isBooking && !state.needsIdentity,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(if (state.isBooking) "Booking…" else "Confirm Booking")
            }
            Spacer(modifier = Modifier.height(Spacing.xl))
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun BookingIdentitySection(
    state: BookingFormState,
    onSearch: (String) -> Unit,
    onSelect: (Patient) -> Unit,
) {
    var query by remember { mutableStateOf("") }
    LaunchedEffect(query) { onSearch(query) }

    Text("Link your patient record", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
    Text(
        "Search for your record by name or MRN to book appointments for yourself.",
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
    OutlinedTextField(
        value = query,
        onValueChange = { query = it },
        modifier = Modifier.fillMaxWidth(),
        label = { Text("Your name or MRN") },
        singleLine = true,
    )
    if (state.isSearchingIdentity) {
        Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator(Modifier.size(24.dp))
        }
    }
    LazyColumn(modifier = Modifier.fillMaxWidth().height(220.dp)) {
        items(state.identityResults) { patient ->
            TextButton(onClick = { onSelect(patient) }) {
                Text("${patient.displayName} · ${patient.patientId}")
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun BookingForm(
    state: BookingFormState,
    onSelectProvider: (Staff) -> Unit,
    onSetType: (String) -> Unit,
    onSetPriority: (String) -> Unit,
    onSetDate: (String) -> Unit,
    onSetStartTime: (String) -> Unit,
    onSetReason: (String) -> Unit,
) {
    Text(
        text = "Booking for ${state.patientName ?: state.patientId ?: ""}",
        style = MaterialTheme.typography.titleSmall,
        fontWeight = FontWeight.SemiBold,
    )

    Text("Provider", style = MaterialTheme.typography.labelLarge)
    FlowRow(horizontalArrangement = Arrangement.spacedBy(Spacing.xs)) {
        state.providers.forEach { provider ->
            FilterChip(
                selected = state.selectedProvider?.id == provider.id,
                onClick = { onSelectProvider(provider) },
                label = { Text(provider.displayName) },
            )
        }
        if (state.providers.isEmpty()) {
            Text(
                "No providers configured — a provider will be assigned at the clinic.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }

    Text("Type", style = MaterialTheme.typography.labelLarge)
    FlowRow(horizontalArrangement = Arrangement.spacedBy(Spacing.xs)) {
        AppointmentType.entries.forEach { type ->
            FilterChip(
                selected = state.appointmentType == type.name,
                onClick = { onSetType(type.name) },
                label = { Text(type.name.replace('_', ' ').lowercase()) },
            )
        }
    }

    Text("Priority", style = MaterialTheme.typography.labelLarge)
    FlowRow(horizontalArrangement = Arrangement.spacedBy(Spacing.xs)) {
        AppointmentPriority.entries.forEach { priority ->
            FilterChip(
                selected = state.priority == priority.name,
                onClick = { onSetPriority(priority.name) },
                label = { Text(priority.name.lowercase()) },
            )
        }
    }

    OutlinedTextField(
        value = state.date,
        onValueChange = onSetDate,
        modifier = Modifier.fillMaxWidth(),
        label = { Text("Date (yyyy-MM-dd)") },
        singleLine = true,
    )

    Text("Time slot", style = MaterialTheme.typography.labelLarge)
    FlowRow(horizontalArrangement = Arrangement.spacedBy(Spacing.xs)) {
        BOOKING_SLOTS.forEach { slot ->
            FilterChip(
                selected = state.startTime == slot,
                onClick = { onSetStartTime(slot) },
                label = { Text(formatTime(slot)) },
            )
        }
    }

    OutlinedTextField(
        value = state.reason,
        onValueChange = onSetReason,
        modifier = Modifier.fillMaxWidth(),
        label = { Text("Reason for visit") },
        singleLine = true,
    )
}
