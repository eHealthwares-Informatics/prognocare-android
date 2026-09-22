package com.ehealthinformatics.prognocare.feature.dashboard.patient

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
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.outlined.EventBusy
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
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
import com.ehealthinformatics.prognocare.designsystem.theme.Spacing
import com.ehealthinformatics.prognocare.feature.appointments.AppointmentsRepository
import com.ehealthinformatics.prognocare.feature.dashboard.doctor.AppointmentUiEvent
import com.ehealthinformatics.prognocare.feature.dashboard.doctor.formatTime

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PatientAppointmentScreen(
    onBack: () -> Unit,
    onBook: (() -> Unit)? = null,
    viewModel: PatientAppointmentsViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    var selectedTab by remember { mutableIntStateOf(0) }
    var showIdentityPicker by remember { mutableStateOf(false) }
    val tabs = listOf("Upcoming", "Past")

    LaunchedEffect(Unit) {
        viewModel.events.collect { event ->
            when (event) {
                is AppointmentUiEvent.Success -> snackbarHostState.showSnackbar(event.message)
                is AppointmentUiEvent.Error -> snackbarHostState.showSnackbar(event.message)
            }
        }
    }

    LaunchedEffect(state.needsIdentity) {
        if (state.needsIdentity) showIdentityPicker = true
    }

    val displayedAppointments = if (selectedTab == 0) state.upcoming else state.past

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text("My Appointments") },
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
        floatingActionButton = {
            if (selectedTab == 0 && onBook != null && !state.needsIdentity) {
                FloatingActionButton(
                    onClick = onBook,
                    containerColor = MaterialTheme.colorScheme.primary,
                ) {
                    Icon(Icons.Default.Add, contentDescription = "Book Appointment")
                }
            }
        },
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
        ) {
            // ── Tab Row ──────────────────────────────────────
            TabRow(
                selectedTabIndex = selectedTab,
                containerColor = MaterialTheme.colorScheme.surface,
                contentColor = MaterialTheme.colorScheme.primary,
            ) {
                tabs.forEachIndexed { index, title ->
                    Tab(
                        selected = selectedTab == index,
                        onClick = { selectedTab = index },
                        text = {
                            Text(
                                text = title,
                                fontWeight = if (selectedTab == index) FontWeight.SemiBold else FontWeight.Normal,
                            )
                        },
                    )
                }
            }

            when {
                state.isLoading -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
                state.error != null -> EmptyState(
                    title = "Could not load appointments",
                    message = state.error ?: "",
                )
                displayedAppointments.isEmpty() -> EmptyState(
                    icon = if (selectedTab == 0) Icons.Default.CalendarMonth else Icons.Outlined.EventBusy,
                    title = if (selectedTab == 0) "No upcoming appointments" else "No past appointments",
                    message = if (selectedTab == 0)
                        "Tap the + button to book your next appointment"
                    else
                        "Your completed and cancelled appointments will appear here",
                )
                else -> LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(Spacing.md),
                    contentPadding = PaddingValues(
                        horizontal = Spacing.lg,
                        vertical = Spacing.base,
                    ),
                ) {
                    items(displayedAppointments, key = { it.id }) { appointment ->
                        PatientAppointmentDetailCard(
                            appointment = appointment,
                            isCancelling = state.isCancelling,
                            onCancel = { viewModel.cancel(appointment.id) },
                        )
                    }
                    item { Spacer(modifier = Modifier.height(Spacing.xxxl)) }
                }
            }
        }
    }

    if (showIdentityPicker) {
        PatientIdentityPickerSheet(
            state = state.identity,
            onSearch = viewModel::searchIdentity,
            onSelect = {
                showIdentityPicker = false
                viewModel.confirmIdentity(it)
            },
            onDismiss = onBack,
        )
    }
}

/**
 * One-time self-identification: the patient picks their record (searched by
 * name or MRN) and it is stored on the device for future visits.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PatientIdentityPickerSheet(
    state: PatientIdentityState,
    onSearch: (String) -> Unit,
    onSelect: (com.ehealthinformatics.prognocare.data.remote.models.Patient) -> Unit,
    onDismiss: () -> Unit,
) {
    var query by remember { mutableStateOf("") }

    LaunchedEffect(Unit) { onSearch("") }
    LaunchedEffect(query) { onSearch(query) }

    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = Spacing.lg)
                .padding(bottom = Spacing.xl),
            verticalArrangement = Arrangement.spacedBy(Spacing.sm),
        ) {
            Text("Link your patient record", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
            Text(
                "Search for your record by name or MRN. This links your account to your patient file on this device.",
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
            if (state.isSearching) {
                Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(Modifier.size(24.dp))
                }
            }
            state.error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(280.dp),
            ) {
                items(state.results) { patient ->
                    TextButton(onClick = { onSelect(patient) }) {
                        Text("${patient.displayName} · ${patient.patientId}")
                    }
                }
            }
        }
    }
}

@Composable
private fun PatientAppointmentDetailCard(
    appointment: Appointment,
    isCancelling: Boolean,
    onCancel: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val (statusType, statusText) = when (appointment.status) {
        "SCHEDULED" -> StatusType.Scheduled to "Scheduled"
        "CHECKED_IN" -> StatusType.InProgress to "Checked In"
        "IN_PROGRESS" -> StatusType.InProgress to "In Progress"
        "COMPLETED" -> StatusType.Completed to "Completed"
        "CANCELLED" -> StatusType.Cancelled to "Cancelled"
        "NO_SHOW" -> StatusType.Cancelled to "No Show"
        else -> StatusType.Pending to appointment.statusDisplay
    }

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(Spacing.base),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(Spacing.base),
        ) {
            // ── Header Row ──────────────────────────────────
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = appointment.typeDisplay,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.SemiBold,
                )
                StatusBadge(text = statusText, type = statusType)
            }

            Spacer(modifier = Modifier.height(Spacing.sm))

            // ── Provider Info ───────────────────────────────
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primaryContainer),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = Icons.Default.Person,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier.size(20.dp),
                    )
                }
                Spacer(modifier = Modifier.width(Spacing.md))
                Column {
                    Text(
                        text = appointment.providerName ?: "Provider to be assigned",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold,
                    )
                    Text(
                        text = appointment.appointmentNumber ?: "",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }

            Spacer(modifier = Modifier.height(Spacing.md))

            // ── Date, Time, Location ────────────────────────
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(Spacing.xl),
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.CalendarMonth,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(16.dp),
                    )
                    Spacer(modifier = Modifier.width(Spacing.xs))
                    Text(
                        text = "${appointment.date} · ${formatTime(appointment.startTime)}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }

            appointment.scheduleLocation?.let { location ->
                Spacer(modifier = Modifier.height(Spacing.xs))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.LocationOn,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(16.dp),
                    )
                    Spacer(modifier = Modifier.width(Spacing.xs))
                    Text(
                        text = location,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }

            // ── Reason ──────────────────────────────────────
            appointment.reason?.takeIf { it.isNotBlank() }?.let { reason ->
                Spacer(modifier = Modifier.height(Spacing.sm))
                Text(
                    text = reason,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                            RoundedCornerShape(Spacing.sm),
                        )
                        .padding(Spacing.sm),
                )
            }

            // ── Cancel action (own scheduled appointments) ──
            if (appointment.status == "SCHEDULED") {
                Spacer(modifier = Modifier.height(Spacing.sm))
                Button(
                    onClick = onCancel,
                    enabled = !isCancelling,
                    modifier = Modifier.align(Alignment.End),
                ) {
                    Text(if (isCancelling) "Cancelling…" else "Cancel Appointment")
                }
            }
        }
    }
}
