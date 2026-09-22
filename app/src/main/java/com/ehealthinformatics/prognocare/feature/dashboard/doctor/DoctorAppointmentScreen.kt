package com.ehealthinformatics.prognocare.feature.dashboard.doctor

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
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
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ehealthinformatics.prognocare.data.remote.models.Appointment
import com.ehealthinformatics.prognocare.designsystem.components.EmptyState
import com.ehealthinformatics.prognocare.designsystem.components.StatusBadge
import com.ehealthinformatics.prognocare.designsystem.components.StatusType
import com.ehealthinformatics.prognocare.designsystem.theme.AppThemeColors
import com.ehealthinformatics.prognocare.designsystem.theme.Spacing

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DoctorAppointmentScreen(
    onBack: () -> Unit,
    onPatientClick: (String) -> Unit,
    viewModel: DoctorAppointmentsViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    var showScheduleDialog by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        viewModel.events.collect { event ->
            when (event) {
                is AppointmentUiEvent.Success -> snackbarHostState.showSnackbar(event.message)
                is AppointmentUiEvent.Error -> snackbarHostState.showSnackbar(event.message)
            }
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Appointments",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.SemiBold,
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = { showScheduleDialog = true }) {
                        Icon(Icons.Default.Add, contentDescription = "New Appointment")
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
                .padding(innerPadding),
        ) {
            // Search
            OutlinedTextField(
                value = state.searchQuery,
                onValueChange = viewModel::setSearchQuery,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = Spacing.lg, vertical = Spacing.sm),
                placeholder = { Text("Search patients…") },
                leadingIcon = {
                    Icon(Icons.Default.Search, contentDescription = null)
                },
                singleLine = true,
                shape = RoundedCornerShape(Spacing.md),
                colors = OutlinedTextFieldDefaults.colors(
                    unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant,
                ),
            )

            // Filter chips
            LazyRow(
                modifier = Modifier.fillMaxWidth(),
                contentPadding = PaddingValues(horizontal = Spacing.lg),
                horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
            ) {
                items(AppointmentFilters.ALL_FILTERS) { filter ->
                    FilterChip(
                        selected = state.filter == filter,
                        onClick = { viewModel.setFilter(filter) },
                        label = { Text(filter) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                        ),
                    )
                }
            }

            Spacer(modifier = Modifier.height(Spacing.sm))

            when {
                state.isLoading -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
                state.error != null -> EmptyState(
                    title = "Could not load appointments",
                    message = state.error ?: "",
                )
                state.appointments.isEmpty() -> EmptyState(
                    title = "No appointments",
                    message = "Nothing matches the current filters.",
                )
                else -> LazyColumn(
                    contentPadding = PaddingValues(horizontal = Spacing.lg),
                    verticalArrangement = Arrangement.spacedBy(Spacing.sm),
                ) {
                    items(state.appointments, key = { it.id }) { appointment ->
                        AppointmentListItem(
                            appointment = appointment,
                            isMutating = state.isMutating,
                            onCheckIn = { viewModel.checkIn(appointment.id) },
                            onComplete = { viewModel.complete(appointment.id) },
                            onNoShow = { viewModel.noShow(appointment.id) },
                            onCancel = { viewModel.cancel(appointment.id) },
                            onClick = { onPatientClick(appointment.patientId) },
                        )
                    }
                }
            }
        }
    }

    if (showScheduleDialog) {
        ScheduleAppointmentDialog(
            onDismiss = { showScheduleDialog = false },
            onScheduled = {
                showScheduleDialog = false
                viewModel.refresh()
            },
        )
    }
}

/** Clinician actions per appointment status. */
private fun doctorActions(appointment: Appointment): List<String> = when (appointment.status) {
    "SCHEDULED" -> listOf("Check In", "No-Show", "Cancel")
    "CHECKED_IN" -> listOf("Start Visit", "Cancel")
    "IN_PROGRESS" -> listOf("Complete")
    else -> emptyList()
}

@Composable
private fun AppointmentListItem(
    appointment: Appointment,
    isMutating: Boolean,
    onCheckIn: () -> Unit,
    onComplete: () -> Unit,
    onNoShow: () -> Unit,
    onCancel: () -> Unit,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var actionsOpen by remember { mutableStateOf(false) }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(Spacing.base),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface,
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(Spacing.base),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.width(56.dp)) {
                Text(
                    text = formatTime(appointment.startTime),
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                )
                Text(
                    text = appointment.date,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            Box(
                modifier = Modifier
                    .width(3.dp)
                    .height(40.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(
                        when {
                            appointment.status == "IN_PROGRESS" || appointment.status == "CHECKED_IN" ->
                                MaterialTheme.colorScheme.primary
                            appointment.isUrgent -> AppThemeColors.current.warning
                            else -> MaterialTheme.colorScheme.outlineVariant
                        },
                    ),
            )

            Spacer(modifier = Modifier.width(Spacing.md))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = appointment.patientName,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Medium,
                )
                Text(
                    text = listOfNotNull(
                        appointment.typeDisplay,
                        appointment.providerName?.let { "· $it" },
                    ).joinToString(" "),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            appointmentStatusBadge(appointment)

            if (doctorActions(appointment).isNotEmpty()) {
                Box {
                    IconButton(onClick = { actionsOpen = true }, enabled = !isMutating) {
                        Icon(Icons.Default.FilterList, contentDescription = "Actions")
                    }
                    DropdownMenu(expanded = actionsOpen, onDismissRequest = { actionsOpen = false }) {
                        doctorActions(appointment).forEach { action ->
                            DropdownMenuItem(
                                text = { Text(action) },
                                onClick = {
                                    actionsOpen = false
                                    when (action) {
                                        "Check In", "Start Visit" -> onCheckIn()
                                        "Complete" -> onComplete()
                                        "No-Show" -> onNoShow()
                                        "Cancel" -> onCancel()
                                    }
                                },
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun appointmentStatusBadge(appointment: Appointment) {
    when (appointment.status) {
        "IN_PROGRESS" -> StatusBadge(text = "In Progress", type = StatusType.InProgress)
        "COMPLETED" -> StatusBadge(text = "Completed", type = StatusType.Completed)
        "CHECKED_IN" -> StatusBadge(text = "Checked In", type = StatusType.Active)
        "CANCELLED" -> StatusBadge(text = "Cancelled", type = StatusType.Cancelled)
        "NO_SHOW" -> StatusBadge(text = "No Show", type = StatusType.Cancelled)
        else -> StatusBadge(
            text = if (appointment.isUrgent) "Urgent" else "Scheduled",
            type = if (appointment.isUrgent) StatusType.Urgent else StatusType.Scheduled,
        )
    }
}

/** `HH:mm` 24-hour backend value → `hh:mm a` display. */
internal fun formatTime(startTime: String): String = runCatching {
    val parts = startTime.split(":")
    val hour = parts.getOrNull(0)?.toIntOrNull() ?: return startTime
    val minute = parts.getOrNull(1) ?: "00"
    val amPm = if (hour >= 12) "PM" else "AM"
    val displayHour = when {
        hour == 0 -> 12
        hour > 12 -> hour - 12
        else -> hour
    }
    "%02d:%s %s".format(displayHour, minute, amPm)
}.getOrDefault(startTime)
