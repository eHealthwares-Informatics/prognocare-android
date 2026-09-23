package com.ehealthinformatics.prognocare.feature.dashboard.nurse

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ehealthinformatics.prognocare.designsystem.theme.Spacing
import com.ehealthinformatics.prognocare.designsystem.theme.Tertiary
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VitalsRecordingScreen(
    onBack: () -> Unit,
    onSaved: () -> Unit,
    viewModel: VitalsRecordingViewModel = hiltViewModel(),
) {
    var patientName by remember { mutableStateOf("") }
    var selectedPatient by remember { mutableStateOf<com.ehealthinformatics.prognocare.data.remote.models.Patient?>(null) }
    var patientResults by remember { mutableStateOf<List<com.ehealthinformatics.prognocare.data.remote.models.Patient>>(emptyList()) }
    var temperature by remember { mutableStateOf("") }
    var bpSystolic by remember { mutableStateOf("") }
    var bpDiastolic by remember { mutableStateOf("") }
    var heartRate by remember { mutableStateOf("") }
    var respiratoryRate by remember { mutableStateOf("") }
    var oxygenSaturation by remember { mutableStateOf("") }
    var weight by remember { mutableStateOf("") }
    var height by remember { mutableStateOf("") }
    var notes by remember { mutableStateOf("") }

    val focusManager = LocalFocusManager.current
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }
    val setupState by viewModel.state.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Record Vitals",
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
        snackbarHost = { SnackbarHost(snackbarHostState) },
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = Spacing.lg),
        ) {
            Spacer(modifier = Modifier.height(Spacing.sm))

            // Patient selector — real patient search
            VitalsSection("Patient") {
                OutlinedTextField(
                    value = patientName,
                    onValueChange = { query ->
                        patientName = query
                        selectedPatient = null
                        scope.launch {
                            viewModel.searchPatients(query) { results -> patientResults = results }
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Search patient by name or MRN") },
                    singleLine = true,
                    shape = RoundedCornerShape(Spacing.md),
                )
                if (selectedPatient == null) {
                    patientResults.take(4).forEach { patient ->
                        androidx.compose.material3.TextButton(
                            onClick = {
                                selectedPatient = patient
                                patientResults = emptyList()
                                patientName = patient.displayName
                            },
                        ) {
                            Text("${patient.displayName} · ${patient.patientId}")
                        }
                    }
                }
            }

            // Vital Signs
            VitalsSection("Vital Signs") {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(Spacing.md),
                ) {
                    VitalsInput(
                        label = "Temperature (°C)",
                        value = temperature,
                        onValueChange = { temperature = it },
                        keyboardType = KeyboardType.Decimal,
                        modifier = Modifier.weight(1f),
                    )
                    VitalsInput(
                        label = "Heart Rate (bpm)",
                        value = heartRate,
                        onValueChange = { heartRate = it },
                        keyboardType = KeyboardType.Number,
                        modifier = Modifier.weight(1f),
                    )
                }
                Spacer(modifier = Modifier.height(Spacing.md))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(Spacing.md),
                ) {
                    VitalsInput(
                        label = "BP Systolic",
                        value = bpSystolic,
                        onValueChange = { bpSystolic = it },
                        keyboardType = KeyboardType.Number,
                        modifier = Modifier.weight(1f),
                    )
                    VitalsInput(
                        label = "BP Diastolic",
                        value = bpDiastolic,
                        onValueChange = { bpDiastolic = it },
                        keyboardType = KeyboardType.Number,
                        modifier = Modifier.weight(1f),
                    )
                }
                Spacer(modifier = Modifier.height(Spacing.md))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(Spacing.md),
                ) {
                    VitalsInput(
                        label = "Resp. Rate",
                        value = respiratoryRate,
                        onValueChange = { respiratoryRate = it },
                        keyboardType = KeyboardType.Number,
                        modifier = Modifier.weight(1f),
                    )
                    VitalsInput(
                        label = "SpO₂ (%)",
                        value = oxygenSaturation,
                        onValueChange = { oxygenSaturation = it },
                        keyboardType = KeyboardType.Number,
                        modifier = Modifier.weight(1f),
                    )
                }
            }

            // Anthropometrics
            VitalsSection("Anthropometrics") {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(Spacing.md),
                ) {
                    VitalsInput(
                        label = "Weight (kg)",
                        value = weight,
                        onValueChange = { weight = it },
                        keyboardType = KeyboardType.Decimal,
                        modifier = Modifier.weight(1f),
                    )
                    VitalsInput(
                        label = "Height (cm)",
                        value = height,
                        onValueChange = { height = it },
                        keyboardType = KeyboardType.Decimal,
                        modifier = Modifier.weight(1f),
                    )
                }
            }

            // Notes
            VitalsSection("Notes (optional)") {
                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 3,
                    shape = RoundedCornerShape(Spacing.md),
                )
            }

            Spacer(modifier = Modifier.height(Spacing.xl))

            // Save button — submits the VITALS documentation form
            Button(
                onClick = {
                    val patient = selectedPatient
                    if (patient == null) {
                        scope.launch { snackbarHostState.showSnackbar("Select a patient first") }
                        return@Button
                    }
                    val form = setupState.vitalsForm
                    if (form == null) {
                        scope.launch { snackbarHostState.showSnackbar("VITALS form not available") }
                        return@Button
                    }
                    scope.launch {
                        val data: Map<String, Any?> = buildMap {
                            temperature.toDoubleOrNull()?.let { put("temperature", it) }
                            bpSystolic.toIntOrNull()?.let { put("bp_systolic", it) }
                            bpDiastolic.toIntOrNull()?.let { put("bp_diastolic", it) }
                            heartRate.toIntOrNull()?.let { put("heart_rate", it) }
                            respiratoryRate.toIntOrNull()?.let { put("respiratory_rate", it) }
                            oxygenSaturation.toIntOrNull()?.let { put("oxygen_saturation", it) }
                            weight.toDoubleOrNull()?.let { put("weight", it) }
                            height.toDoubleOrNull()?.let { put("height", it) }
                            if (notes.isNotBlank()) put("notes", notes)
                        }
                        val ok = viewModel.submitVitals(
                            formId = form.id,
                            patientId = patient.id,
                            data = data,
                        )
                        if (ok) {
                            snackbarHostState.showSnackbar("Vitals recorded successfully")
                            onSaved()
                        } else {
                            snackbarHostState.showSnackbar("Could not save vitals — check the form keys or server")
                        }
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                shape = RoundedCornerShape(Spacing.md),
                colors = ButtonDefaults.buttonColors(containerColor = Tertiary),
            ) {
                Icon(Icons.Default.Check, contentDescription = null)
                Spacer(modifier = Modifier.width(Spacing.sm))
                Text(
                    text = "Save Vitals",
                    fontWeight = FontWeight.SemiBold,
                )
            }

            Spacer(modifier = Modifier.height(Spacing.xxxl))
        }
    }
}

@Composable
private fun VitalsSection(
    title: String,
    content: @Composable () -> Unit,
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = Spacing.md),
        shape = RoundedCornerShape(Spacing.base),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
    ) {
        Column(modifier = Modifier.padding(Spacing.base)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(modifier = Modifier.height(Spacing.md))
            content()
        }
    }
}

@Composable
private fun VitalsInput(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    keyboardType: KeyboardType = KeyboardType.Text,
    modifier: Modifier = Modifier,
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = modifier,
        label = { Text(label) },
        keyboardOptions = KeyboardOptions(
            keyboardType = keyboardType,
            imeAction = ImeAction.Next,
        ),
        singleLine = true,
        shape = RoundedCornerShape(Spacing.md),
        colors = OutlinedTextFieldDefaults.colors(
            unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant,
        ),
    )
}
