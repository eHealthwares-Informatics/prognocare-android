package com.ehealthinformatics.prognocare.feature.clinical

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import com.ehealthinformatics.prognocare.designsystem.components.EmptyState
import com.ehealthinformatics.prognocare.designsystem.components.ErrorState
import com.ehealthinformatics.prognocare.designsystem.components.LocationScopeChip
import com.ehealthinformatics.prognocare.designsystem.components.StatusBadge
import com.ehealthinformatics.prognocare.designsystem.components.StatusType
import com.ehealthinformatics.prognocare.designsystem.theme.Spacing

// ── Shared bits ──────────────────────────────────────────────────

@Composable
private fun ClinicalSnackbarHost(hostState: SnackbarHostState) {
    SnackbarHost(hostState = hostState)
}

private fun statusType(status: String): StatusType = when (status.uppercase()) {
    "SCHEDULED", "BOOKED" -> StatusType.Scheduled
    "CHECKED_IN", "IN_PROGRESS", "ONGOING" -> StatusType.InProgress
    "COMPLETED", "FULFILLED", "FINISHED" -> StatusType.Completed
    "CANCELLED", "NO_SHOW", "MISSED" -> StatusType.Cancelled
    else -> StatusType.Pending
}

private fun priorityType(priority: String?): StatusType? = when (priority?.uppercase()) {
    "URGENT", "EMERGENCY", "HIGH", "STAT" -> StatusType.Urgent
    else -> null
}

/** Title row with the location-scope chip used by all three list screens. */
@Composable
private fun ScopeBar(
    viewModel: Any,
    onScopeChanged: () -> Unit,
) {
    val scope = when (viewModel) {
        is ClinicalAppointmentsViewModel -> viewModel.locationScope
        is ClinicalVisitsViewModel -> viewModel.locationScope
        is ClinicalEncountersViewModel -> viewModel.locationScope
        else -> null
    } ?: return
    LocationScopeChip(locationScope = scope, onScopeChanged = onScopeChanged)
}

// ── Appointments ─────────────────────────────────────────────────

private val STATUS_FILTERS = listOf(
    "All" to null,
    "Scheduled" to "SCHEDULED",
    "Checked in" to "CHECKED_IN",
    "Completed" to "COMPLETED",
    "Cancelled" to "CANCELLED",
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ClinicalAppointmentsScreen(
    onBack: () -> Unit,
    onCreateAppointment: () -> Unit,
    viewModel: ClinicalAppointmentsViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(Unit) {
        viewModel.load()
        viewModel.events.collect { event ->
            when (event) {
                is ClinicalUiEvent.Success -> snackbarHostState.showSnackbar(event.message)
                is ClinicalUiEvent.Error -> snackbarHostState.showSnackbar(event.message)
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Appointments", fontWeight = FontWeight.Bold) },
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
        snackbarHost = { ClinicalSnackbarHost(snackbarHostState) },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = onCreateAppointment,
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
                shape = RoundedCornerShape(Spacing.lg),
            ) {
                Icon(Icons.Default.Add, contentDescription = null)
                Spacer(modifier = Modifier.width(Spacing.sm))
                Text("Schedule", fontWeight = FontWeight.SemiBold)
            }
        },
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
        ) {
            // Location scope ("location based query")
            Row(modifier = Modifier.padding(horizontal = Spacing.lg)) {
                ScopeBar(viewModel) { viewModel.load() }
            }

            // Status filters
            LazyRow(
                contentPadding = PaddingValues(horizontal = Spacing.lg, vertical = Spacing.xs),
                horizontalArrangement = Arrangement.spacedBy(Spacing.xs),
            ) {
                items(STATUS_FILTERS) { (label, value) ->
                    FilterChip(
                        selected = state.statusFilter == value,
                        onClick = { viewModel.setStatus(value) },
                        label = { Text(label) },
                    )
                }
            }

            when {
                state.isLoading -> Column(
                    modifier = Modifier.fillMaxSize(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                ) { CircularProgressIndicator() }

                state.error != null -> ErrorState(message = state.error!!, onRetry = { viewModel.load() })

                state.appointments.isEmpty() -> EmptyState(
                    title = "No appointments",
                    message = "Nothing matches the current location and filters.",
                )

                else -> LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(
                        start = Spacing.lg, end = Spacing.lg, bottom = 100.dp,
                    ),
                    verticalArrangement = Arrangement.spacedBy(Spacing.sm),
                ) {
                    items(state.appointments, key = { it.id }) { appointment ->
                        AppointmentCard(
                            appointment = appointment,
                            onCheckIn = { viewModel.checkIn(appointment.id) },
                            onComplete = { viewModel.complete(appointment.id) },
                            onCancel = { viewModel.cancel(appointment.id) },
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun AppointmentCard(
    appointment: com.ehealthinformatics.prognocare.data.remote.models.Appointment,
    onCheckIn: () -> Unit,
    onComplete: () -> Unit,
    onCancel: () -> Unit,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(Spacing.base),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
    ) {
        Column(modifier = Modifier.padding(Spacing.base)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = appointment.patientName.ifBlank { "Patient ${appointment.patientId}" },
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold,
                    )
                    Text(
                        text = "${appointment.date} · ${appointment.startTime}" +
                            (appointment.endTime?.let { "–$it" } ?: ""),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Column(horizontalAlignment = Alignment.End) {
                    StatusBadge(text = appointment.statusDisplay, type = statusType(appointment.status))
                    priorityType(appointment.priority)?.let {
                        Spacer(modifier = Modifier.height(Spacing.xxs))
                        StatusBadge(text = appointment.priorityDisplay, type = it)
                    }
                }
            }

            Text(
                text = listOfNotNull(
                    appointment.typeDisplay,
                    appointment.providerName,
                    appointment.scheduleLocation,
                ).joinToString(" · "),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            appointment.reason?.takeIf { it.isNotBlank() }?.let {
                Text(
                    text = it,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                )
            }

            Row(horizontalArrangement = Arrangement.spacedBy(Spacing.xs)) {
                if (appointment.status == "SCHEDULED") {
                    TextButton(onClick = onCheckIn) { Text("Check in") }
                }
                if (appointment.isActive) {
                    TextButton(onClick = onComplete) { Text("Complete") }
                }
                if (appointment.status in listOf("SCHEDULED", "CHECKED_IN")) {
                    TextButton(onClick = onCancel) {
                        Text("Cancel", color = MaterialTheme.colorScheme.error)
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun CreateAppointmentDialog(
    onDismiss: () -> Unit,
    onCreated: () -> Unit,
    viewModel: CreateAppointmentViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    var patientQuery by remember { mutableStateOf("") }

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

            if (state.selectedPatient == null) {
                OutlinedTextField(
                    value = patientQuery,
                    onValueChange = { patientQuery = it },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Patient") },
                    placeholder = { Text("Search by name or MRN") },
                    singleLine = true,
                )
                LazyColumn(modifier = Modifier.fillMaxWidth().heightIn(max = 160.dp)) {
                    items(state.patientResults) { patient ->
                        TextButton(onClick = { viewModel.selectPatient(patient) }) {
                            Text("${patient.displayName} · ${patient.patientId}")
                        }
                    }
                }
            } else {
                TextButton(onClick = viewModel::clearPatient) {
                    Text("Patient: ${state.selectedPatient?.displayName} · change")
                }
            }

            Text("Provider", style = MaterialTheme.typography.labelLarge)
            FlowRow(horizontalArrangement = Arrangement.spacedBy(Spacing.xs)) {
                FilterChip(
                    selected = state.selectedProvider == null,
                    onClick = { viewModel.selectProvider(null) },
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

            Text("Type", style = MaterialTheme.typography.labelLarge)
            FlowRow(horizontalArrangement = Arrangement.spacedBy(Spacing.xs)) {
                com.ehealthinformatics.prognocare.data.remote.models.AppointmentType.entries.forEach { type ->
                    FilterChip(
                        selected = state.appointmentType == type.name,
                        onClick = { viewModel.update { s -> s.copy(appointmentType = type.name) } },
                        label = { Text(type.name.replace('_', ' ').lowercase()) },
                    )
                }
            }

            Text("Priority", style = MaterialTheme.typography.labelLarge)
            FlowRow(horizontalArrangement = Arrangement.spacedBy(Spacing.xs)) {
                com.ehealthinformatics.prognocare.data.remote.models.AppointmentPriority.entries.forEach { priority ->
                    FilterChip(
                        selected = state.priority == priority.name,
                        onClick = { viewModel.update { s -> s.copy(priority = priority.name) } },
                        label = { Text(priority.name.lowercase()) },
                    )
                }
            }

            Row(horizontalArrangement = Arrangement.spacedBy(Spacing.sm)) {
                OutlinedTextField(
                    value = state.date,
                    onValueChange = { viewModel.update { s -> s.copy(date = it) } },
                    modifier = Modifier.weight(1f),
                    label = { Text("Date (yyyy-MM-dd)") },
                    singleLine = true,
                )
                OutlinedTextField(
                    value = state.startTime,
                    onValueChange = { viewModel.update { s -> s.copy(startTime = it) } },
                    modifier = Modifier.weight(1f),
                    label = { Text("Start (HH:mm)") },
                    singleLine = true,
                )
            }

            OutlinedTextField(
                value = state.reason,
                onValueChange = { viewModel.update { s -> s.copy(reason = it) } },
                modifier = Modifier.fillMaxWidth(),
                label = { Text("Reason") },
                minLines = 2,
            )

            state.error?.let {
                Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
            }

            TextButton(
                onClick = { viewModel.save(onCreated = onCreated) },
                modifier = Modifier.fillMaxWidth(),
                enabled = !state.isSaving,
            ) {
                if (state.isSaving) {
                    CircularProgressIndicator(modifier = Modifier.heightIn(min = 18.dp), strokeWidth = 2.dp)
                    Spacer(modifier = Modifier.width(Spacing.sm))
                }
                Text(if (state.isSaving) "Scheduling…" else "Schedule appointment")
            }
        }
    }
}

// ── Visits ───────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ClinicalVisitsScreen(
    onBack: () -> Unit,
    onStartVisit: () -> Unit,
    onVisitClick: (String) -> Unit = {},
    viewModel: ClinicalVisitsViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(Unit) {
        viewModel.load()
        viewModel.events.collect { event ->
            when (event) {
                is ClinicalUiEvent.Success -> snackbarHostState.showSnackbar(event.message)
                is ClinicalUiEvent.Error -> snackbarHostState.showSnackbar(event.message)
            }
        }
    }

    // Refresh when returning from the create-visit dialog.
    val lifecycleOwner = androidx.compose.ui.platform.LocalLifecycleOwner.current
    androidx.compose.runtime.DisposableEffect(lifecycleOwner) {
        val observer = androidx.lifecycle.LifecycleEventObserver { _, event ->
            if (event == androidx.lifecycle.Lifecycle.Event.ON_RESUME) {
                viewModel.load()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Visits", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = { viewModel.load() }) {
                        Icon(Icons.Default.Refresh, contentDescription = "Refresh")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                ),
            )
        },
        snackbarHost = { ClinicalSnackbarHost(snackbarHostState) },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = onStartVisit,
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
                shape = RoundedCornerShape(Spacing.lg),
            ) {
                Icon(Icons.Default.Add, contentDescription = null)
                Spacer(modifier = Modifier.width(Spacing.sm))
                Text("Start visit", fontWeight = FontWeight.SemiBold)
            }
        },
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
        ) {
            Row(modifier = Modifier.padding(horizontal = Spacing.lg)) {
                ScopeBar(viewModel) { viewModel.load() }
            }

            when {
                state.isLoading -> Column(
                    modifier = Modifier.fillMaxSize(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                ) { CircularProgressIndicator() }

                state.error != null -> ErrorState(message = state.error!!, onRetry = { viewModel.load() })

                state.visits.isEmpty() -> EmptyState(
                    title = "No visits",
                    message = "No visits recorded for this location yet.",
                )

                else -> LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(
                        start = Spacing.lg, end = Spacing.lg, bottom = 100.dp,
                    ),
                    verticalArrangement = Arrangement.spacedBy(Spacing.sm),
                ) {
                    items(state.visits, key = { it.id }) { visit ->
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onVisitClick(visit.id) },
                            shape = RoundedCornerShape(Spacing.base),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                            elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                        ) {
                            Column(modifier = Modifier.padding(Spacing.base)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = visit.patientName.ifBlank { "Patient ${visit.patientId}" },
                                            style = MaterialTheme.typography.titleSmall,
                                            fontWeight = FontWeight.SemiBold,
                                        )
                                        Text(
                                            text = listOfNotNull(
                                                visit.visitNumber,
                                                visit.startDatetime?.take(16)?.replace('T', ' '),
                                            ).joinToString(" · "),
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        )
                                    }
                                    StatusBadge(
                                        text = visit.status.lowercase().replaceFirstChar { c -> c.uppercase() },
                                        type = statusType(visit.status),
                                    )
                                }
                                Text(
                                    text = listOfNotNull(
                                        visit.typeDisplay,
                                        visit.providerName,
                                    ).joinToString(" · "),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                                if (visit.isOngoing) {
                                    Row(horizontalArrangement = Arrangement.spacedBy(Spacing.xs)) {
                                        TextButton(onClick = { viewModel.end(visit.id) }) { Text("End") }
                                        TextButton(onClick = { viewModel.cancel(visit.id) }) {
                                            Text("Cancel", color = MaterialTheme.colorScheme.error)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun CreateVisitDialog(
    onDismiss: () -> Unit,
    onCreated: () -> Unit,
    viewModel: CreateVisitViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    var patientQuery by remember { mutableStateOf("") }

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
                "Start Visit",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.SemiBold,
            )

            if (state.selectedPatient == null) {
                OutlinedTextField(
                    value = patientQuery,
                    onValueChange = { patientQuery = it },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Patient") },
                    placeholder = { Text("Search by name or MRN") },
                    singleLine = true,
                )
                LazyColumn(modifier = Modifier.fillMaxWidth().heightIn(max = 160.dp)) {
                    items(state.patientResults) { patient ->
                        TextButton(onClick = { viewModel.selectPatient(patient) }) {
                            Text("${patient.displayName} · ${patient.patientId}")
                        }
                    }
                }
            } else {
                TextButton(onClick = viewModel::clearPatient) {
                    Text("Patient: ${state.selectedPatient?.displayName} · change")
                }
            }

            Text("Visit type", style = MaterialTheme.typography.labelLarge)
            FlowRow(horizontalArrangement = Arrangement.spacedBy(Spacing.xs)) {
                com.ehealthinformatics.prognocare.data.remote.models.VisitType.entries.forEach { type ->
                    FilterChip(
                        selected = state.visitType == type.name,
                        onClick = { viewModel.setType(type.name) },
                        label = { Text(type.name.replace('_', ' ').lowercase()) },
                    )
                }
            }

            state.error?.let {
                Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
            }

            TextButton(
                onClick = { viewModel.save(onCreated = { onCreated() }) },
                modifier = Modifier.fillMaxWidth(),
                enabled = !state.isSaving,
            ) {
                if (state.isSaving) {
                    CircularProgressIndicator(modifier = Modifier.heightIn(min = 18.dp), strokeWidth = 2.dp)
                    Spacer(modifier = Modifier.width(Spacing.sm))
                }
                Text(if (state.isSaving) "Starting…" else "Start visit")
            }
        }
    }
}

// ── Encounters ───────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ClinicalEncountersScreen(
    onBack: () -> Unit,
    onEncounterClick: (String) -> Unit,
    onDocumentEncounter: () -> Unit,
    viewModel: ClinicalEncountersViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    LaunchedEffect(Unit) { viewModel.load() }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Encounters", fontWeight = FontWeight.Bold) },
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
            ExtendedFloatingActionButton(
                onClick = onDocumentEncounter,
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
                shape = RoundedCornerShape(Spacing.lg),
            ) {
                Icon(Icons.Default.Add, contentDescription = null)
                Spacer(modifier = Modifier.width(Spacing.sm))
                Text("Document", fontWeight = FontWeight.SemiBold)
            }
        },
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
        ) {
            Row(modifier = Modifier.padding(horizontal = Spacing.lg)) {
                ScopeBar(viewModel) { viewModel.load() }
            }

            when {
                state.isLoading -> Column(
                    modifier = Modifier.fillMaxSize(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                ) { CircularProgressIndicator() }

                state.error != null -> ErrorState(message = state.error!!, onRetry = { viewModel.load() })

                state.encounters.isEmpty() -> EmptyState(
                    title = "No encounters",
                    message = "No encounters for this location yet.",
                )

                else -> LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(
                        start = Spacing.lg, end = Spacing.lg, bottom = 100.dp,
                    ),
                    verticalArrangement = Arrangement.spacedBy(Spacing.sm),
                ) {
                    items(state.encounters, key = { it.id }) { encounter ->
                        val visit = encounter.visitId?.let { state.visitsById[it] }
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onEncounterClick(encounter.id) },
                            shape = RoundedCornerShape(Spacing.base),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                            elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                        ) {
                            Column(modifier = Modifier.padding(Spacing.base)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = encounter.typeDisplay.ifBlank { "Encounter" },
                                            style = MaterialTheme.typography.titleSmall,
                                            fontWeight = FontWeight.SemiBold,
                                        )
                                        Text(
                                            text = listOfNotNull(
                                                visit?.patientName,
                                                encounter.encounterDatetime?.take(16)?.replace('T', ' '),
                                            ).joinToString(" · "),
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        )
                                    }
                                    visit?.let {
                                        StatusBadge(
                                            text = it.status.lowercase().replaceFirstChar { c -> c.uppercase() },
                                            type = statusType(it.status),
                                        )
                                    }
                                }
                                encounter.reason?.takeIf { it.isNotBlank() }?.let {
                                    Text(
                                        text = it,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        maxLines = 2,
                                    )
                                }
                                encounter.providerName?.let {
                                    Text(
                                        text = it,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun CreateEncounterDialog(
    onDismiss: () -> Unit,
    onCreated: (String) -> Unit,
    viewModel: CreateEncounterViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    var patientQuery by remember { mutableStateOf("") }

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
                "Document Encounter",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.SemiBold,
            )

            // ── Mode selector ────────────────────────────────
            FlowRow(horizontalArrangement = Arrangement.spacedBy(Spacing.xs)) {
                EncounterStartMode.entries.forEach { mode ->
                    FilterChip(
                        selected = state.mode == mode,
                        onClick = { viewModel.setMode(mode) },
                        label = { Text(mode.label) },
                    )
                }
            }

            // ── Patient selection (mandatory in every mode) ──
            when (state.mode) {
                EncounterStartMode.FROM_VISIT -> {
                    if (state.isLoading) {
                        CircularProgressIndicator(Modifier.heightIn(min = 24.dp))
                    } else {
                        LazyColumn(modifier = Modifier.fillMaxWidth().heightIn(max = 220.dp)) {
                            items(state.visits, key = { it.id }) { visit ->
                                ListItem(
                                    headlineContent = {
                                        Text(visit.patientName.ifBlank { "Patient ${visit.patientId}" })
                                    },
                                    supportingContent = {
                                        Text(listOfNotNull(visit.typeDisplay, visit.startDatetime?.take(10)).joinToString(" · "))
                                    },
                                    trailingContent = {
                                        if (state.selectedVisit?.id == visit.id) {
                                            Icon(Icons.Default.Check, contentDescription = "Selected")
                                        }
                                    },
                                    modifier = Modifier.clickable { viewModel.selectVisit(visit) },
                                )
                            }
                        }
                        if (state.visits.isEmpty()) {
                            Text(
                                "No patients with open visits",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                }
                EncounterStartMode.FROM_APPOINTMENT -> {
                    if (state.isLoading) {
                        CircularProgressIndicator(Modifier.heightIn(min = 24.dp))
                    } else {
                        LazyColumn(modifier = Modifier.fillMaxWidth().heightIn(max = 220.dp)) {
                            items(state.appointments, key = { it.id }) { appointment ->
                                ListItem(
                                    headlineContent = {
                                        Text(appointment.patientName.ifBlank { "Patient ${appointment.patientId}" })
                                    },
                                    supportingContent = {
                                        Text(listOfNotNull(appointment.startTime, appointment.typeDisplay).joinToString(" · "))
                                    },
                                    trailingContent = {
                                        if (state.selectedAppointment?.id == appointment.id) {
                                            Icon(Icons.Default.Check, contentDescription = "Selected")
                                        }
                                    },
                                    modifier = Modifier.clickable { viewModel.selectAppointment(appointment) },
                                )
                            }
                        }
                        if (state.appointments.isEmpty()) {
                            Text(
                                "No open appointments today",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                }
                EncounterStartMode.FROM_PATIENT -> {
                    if (state.selectedPatient == null) {
                        OutlinedTextField(
                            value = patientQuery,
                            onValueChange = { patientQuery = it },
                            modifier = Modifier.fillMaxWidth(),
                            label = { Text("Patient") },
                            placeholder = { Text("Search by name or MRN") },
                            singleLine = true,
                        )
                        LazyColumn(modifier = Modifier.fillMaxWidth().heightIn(max = 200.dp)) {
                            items(state.patientResults) { patient ->
                                TextButton(onClick = { viewModel.selectPatient(patient) }) {
                                    Text("${patient.displayName} · ${patient.patientId}")
                                }
                            }
                        }
                    } else {
                        TextButton(onClick = viewModel::clearSelection) {
                            Text("Patient: ${state.selectedPatient?.displayName} · change")
                        }
                    }
                }
            }

            Text("Encounter type", style = MaterialTheme.typography.labelLarge)
            FlowRow(horizontalArrangement = Arrangement.spacedBy(Spacing.xs)) {
                com.ehealthinformatics.prognocare.data.remote.models.EncounterType.entries.forEach { type ->
                    FilterChip(
                        selected = state.encounterType == type.name,
                        onClick = { viewModel.setType(type.name) },
                        label = { Text(type.name.replace('_', ' ').lowercase()) },
                    )
                }
            }

            OutlinedTextField(
                value = state.reason,
                onValueChange = { viewModel.update { s -> s.copy(reason = it) } },
                modifier = Modifier.fillMaxWidth(),
                label = { Text("Reason") },
                minLines = 2,
            )

            OutlinedTextField(
                value = state.notes,
                onValueChange = { viewModel.update { s -> s.copy(notes = it) } },
                modifier = Modifier.fillMaxWidth(),
                label = { Text("Notes") },
                minLines = 2,
            )

            state.error?.let {
                Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
            }

            TextButton(
                onClick = { viewModel.save(onCreated = { encounter -> onCreated(encounter.id) }) },
                modifier = Modifier.fillMaxWidth(),
                enabled = !state.isSaving && !state.isLoading,
            ) {
                if (state.isSaving) {
                    CircularProgressIndicator(modifier = Modifier.heightIn(min = 18.dp), strokeWidth = 2.dp)
                    Spacer(modifier = Modifier.width(Spacing.sm))
                }
                Text(if (state.isSaving) "Saving…" else "Document encounter")
            }
        }
    }
}
