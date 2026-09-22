package com.ehealthinformatics.prognocare.feature.checkin

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
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
import com.ehealthinformatics.prognocare.data.remote.models.Appointment
import com.ehealthinformatics.prognocare.designsystem.components.StatusBadge
import com.ehealthinformatics.prognocare.designsystem.components.StatusType
import com.ehealthinformatics.prognocare.designsystem.theme.Spacing
import com.ehealthinformatics.prognocare.designsystem.theme.Tertiary
import com.ehealthinformatics.prognocare.feature.appointments.AppointmentQuery
import com.ehealthinformatics.prognocare.feature.appointments.AppointmentsRepository
import com.ehealthinformatics.prognocare.feature.dashboard.doctor.AppointmentUiEvent
import com.ehealthinformatics.prognocare.feature.dashboard.doctor.formatTime
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class CheckInQueueState(
    val isLoading: Boolean = true,
    val isMutating: Boolean = false,
    val error: String? = null,
    val appointments: List<Appointment> = emptyList(),
)

/**
 * Front-desk check-in queue shared by the Nurse, Support and Admin roles:
 * today's appointments with a check-in action. Role gating happens upstream —
 * only roles with the check-in route reach this screen, and its only mutation
 * is check-in.
 */
@HiltViewModel
class CheckInViewModel @Inject constructor(
    private val repository: AppointmentsRepository,
) : ViewModel() {

    private val _state = MutableStateFlow(CheckInQueueState())
    val state: StateFlow<CheckInQueueState> = _state.asStateFlow()

    private val _events = MutableSharedFlow<AppointmentUiEvent>(extraBufferCapacity = 1)
    val events: SharedFlow<AppointmentUiEvent> = _events.asSharedFlow()

    init {
        refresh()
    }

    fun refresh() {
        viewModelScope.launch {
            _state.value = _state.value.copy(isLoading = true, error = null)
            try {
                val appointments = repository.list(
                    AppointmentQuery(date = repository.today(), limit = 100),
                )
                _state.value = _state.value.copy(isLoading = false, appointments = appointments)
            } catch (e: Exception) {
                _state.value = _state.value.copy(isLoading = false, error = e.message)
            }
        }
    }

    fun checkIn(id: String) {
        if (_state.value.isMutating) return
        viewModelScope.launch {
            _state.value = _state.value.copy(isMutating = true)
            try {
                repository.checkIn(id)
                _events.emit(AppointmentUiEvent.Success("Patient checked in"))
                refresh()
            } catch (e: Exception) {
                _events.emit(AppointmentUiEvent.Error(e.message ?: "Check-in failed"))
            } finally {
                _state.value = _state.value.copy(isMutating = false)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CheckInQueueScreen(
    title: String,
    onBack: () -> Unit,
    viewModel: CheckInViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    var selectedFilter by remember { mutableStateOf("All") }
    val filters = listOf("All", "Waiting", "Checked In", "In Progress")

    LaunchedEffect(Unit) {
        viewModel.events.collect { event ->
            when (event) {
                is AppointmentUiEvent.Success -> snackbarHostState.showSnackbar(event.message)
                is AppointmentUiEvent.Error -> snackbarHostState.showSnackbar(event.message)
            }
        }
    }

    val queue = state.appointments.filter { it.status in setOf("SCHEDULED", "CHECKED_IN", "IN_PROGRESS") }
    val filteredQueue = queue.filter { appointment ->
        when (selectedFilter) {
            "Waiting" -> appointment.status == "SCHEDULED"
            "Checked In" -> appointment.status == "CHECKED_IN"
            "In Progress" -> appointment.status == "IN_PROGRESS"
            else -> true
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.SemiBold,
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface),
            )
        },
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
        ) {
            // Filter chips
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = Spacing.lg, vertical = Spacing.sm),
                horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
            ) {
                filters.forEach { filter ->
                    FilterChip(
                        selected = selectedFilter == filter,
                        onClick = { selectedFilter = filter },
                        label = { Text(filter) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                        ),
                    )
                }
            }

            // Summary
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = Spacing.lg, vertical = Spacing.sm),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(
                    text = "${queue.count { it.status == "SCHEDULED" }} waiting",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(
                    text = "${queue.count { it.status == "CHECKED_IN" }} checked in",
                    style = MaterialTheme.typography.bodySmall,
                    color = Tertiary,
                )
            }

            when {
                state.isLoading -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
                state.error != null -> Text(
                    text = "Could not load the queue: ${state.error}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.padding(Spacing.lg),
                )
                filteredQueue.isEmpty() -> Text(
                    text = "No patients in the queue today.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(Spacing.lg),
                )
                else -> LazyColumn(
                    contentPadding = PaddingValues(horizontal = Spacing.lg),
                    verticalArrangement = Arrangement.spacedBy(Spacing.sm),
                ) {
                    items(filteredQueue, key = { it.id }) { appointment ->
                        CheckInQueueCard(
                            appointment = appointment,
                            isMutating = state.isMutating,
                            onCheckIn = { viewModel.checkIn(appointment.id) },
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun CheckInQueueCard(
    appointment: Appointment,
    isMutating: Boolean,
    onCheckIn: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(Spacing.base),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(Spacing.base),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            // Avatar
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primaryContainer),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = appointment.patientName.take(2),
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                    fontWeight = FontWeight.Bold,
                )
            }

            Spacer(modifier = Modifier.width(Spacing.md))

            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = appointment.patientName,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Medium,
                    )
                    Spacer(modifier = Modifier.width(Spacing.sm))
                    StatusBadge(
                        text = when (appointment.status) {
                            "SCHEDULED" -> "Waiting"
                            "CHECKED_IN" -> "Checked In"
                            else -> "In Progress"
                        },
                        type = when (appointment.status) {
                            "SCHEDULED" -> StatusType.Pending
                            "CHECKED_IN" -> StatusType.Active
                            else -> StatusType.InProgress
                        },
                    )
                }
                Spacer(modifier = Modifier.height(Spacing.xxs))
                Text(
                    text = appointment.typeDisplay +
                        (appointment.providerName?.let { " · $it" } ?: ""),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = formatTime(appointment.startTime),
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.SemiBold,
                )
                Spacer(modifier = Modifier.height(Spacing.xs))
                // Front-desk roles only check in — SCHEDULED (waiting) or
                // CHECKED_IN (start the visit). No-show/complete stay clinician-only.
                if (appointment.status == "SCHEDULED" || appointment.status == "CHECKED_IN") {
                    TextButton(onClick = onCheckIn, enabled = !isMutating) {
                        Text(
                            if (appointment.status == "SCHEDULED") "Check In" else "Start Visit",
                            color = Tertiary,
                            fontWeight = FontWeight.SemiBold,
                        )
                    }
                }
            }
        }
    }
}
