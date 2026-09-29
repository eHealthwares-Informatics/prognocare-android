package com.ehealthinformatics.prognocare.feature.dashboard.admin

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
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
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import com.ehealthinformatics.prognocare.data.remote.RetrofitClient
import com.ehealthinformatics.prognocare.data.remote.models.CreatePatientDto
import com.ehealthinformatics.prognocare.designsystem.theme.Spacing
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class RegisterPatientState(
    val firstName: String = "",
    val lastName: String = "",
    val otherNames: String = "",
    val dateOfBirth: String = "",
    val gender: String = "",
    val phone: String = "",
    val email: String = "",
    val address: String = "",
    val nextOfKinName: String = "",
    val nextOfKinPhone: String = "",
    val nextOfKinRelationship: String = "",
    val isSaving: Boolean = false,
    val error: String? = null,
)

sealed class RegisterPatientEvent {
    data class Success(val patientId: String, val name: String) : RegisterPatientEvent()
    data class Error(val message: String) : RegisterPatientEvent()
}

@HiltViewModel
class RegisterPatientViewModel @Inject constructor(
    private val retrofitClient: RetrofitClient,
) : ViewModel() {

    private val _state = MutableStateFlow(RegisterPatientState())
    val state: StateFlow<RegisterPatientState> = _state.asStateFlow()

    private val _events = MutableSharedFlow<RegisterPatientEvent>(extraBufferCapacity = 1)
    val events: SharedFlow<RegisterPatientEvent> = _events.asSharedFlow()

    fun update(transform: (RegisterPatientState) -> RegisterPatientState) {
        _state.value = transform(_state.value)
    }

    fun save() {
        val current = _state.value
        if (current.firstName.isBlank() || current.lastName.isBlank()) {
            viewModelScope.launch { _events.emit(RegisterPatientEvent.Error("First and last name are required")) }
            return
        }
        if (current.isSaving) return
        viewModelScope.launch {
            _state.value = current.copy(isSaving = true, error = null)
            try {
                val resp = retrofitClient.apis.value.patientApi.create(
                    CreatePatientDto(
                        firstName = current.firstName.trim(),
                        lastName = current.lastName.trim(),
                        otherNames = current.otherNames.trim().takeIf { it.isNotEmpty() },
                        dateOfBirth = current.dateOfBirth.trim().takeIf { it.isNotEmpty() },
                        gender = current.gender.takeIf { it.isNotEmpty() },
                        phone = current.phone.trim().takeIf { it.isNotEmpty() },
                        email = current.email.trim().takeIf { it.isNotEmpty() },
                        address = current.address.trim().takeIf { it.isNotEmpty() },
                        nextOfKinName = current.nextOfKinName.trim().takeIf { it.isNotEmpty() },
                        nextOfKinPhone = current.nextOfKinPhone.trim().takeIf { it.isNotEmpty() },
                        nextOfKinRelationship = current.nextOfKinRelationship.trim().takeIf { it.isNotEmpty() },
                    ),
                )
                if (resp.isSuccessful && resp.body() != null) {
                    val patient = resp.body()!!
                    _events.emit(
                        RegisterPatientEvent.Success(
                            patientId = patient.patientId.ifBlank { patient.id },
                            name = patient.displayName,
                        ),
                    )
                    _state.value = RegisterPatientState()
                } else {
                    val detail = runCatching { resp.errorBody()?.string() }
                        .getOrNull()?.lineSequence()?.firstOrNull()?.take(200)
                    _events.emit(RegisterPatientEvent.Error(detail ?: "Registration failed (${resp.code()})"))
                }
            } catch (e: Exception) {
                _events.emit(RegisterPatientEvent.Error(e.message ?: "Registration failed"))
            } finally {
                _state.value = _state.value.copy(isSaving = false)
            }
        }
    }
}

private val GENDERS = listOf("MALE", "FEMALE")

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RegisterPatientScreen(
    onBack: () -> Unit,
    onRegistered: (String) -> Unit = {},
    viewModel: RegisterPatientViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(Unit) {
        viewModel.events.collect { event ->
            when (event) {
                is RegisterPatientEvent.Success -> {
                    snackbarHostState.showSnackbar("Patient ${event.name} registered (${event.patientId})")
                    onRegistered(event.patientId)
                }
                is RegisterPatientEvent.Error -> snackbarHostState.showSnackbar(event.message)
            }
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text("Register Patient") },
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
                .padding(Spacing.lg),
            verticalArrangement = Arrangement.spacedBy(Spacing.md),
        ) {
            SectionCard(title = "Identity") {
                OutlinedTextField(
                    value = state.firstName,
                    onValueChange = { v -> viewModel.update { it.copy(firstName = v) } },
                    label = { Text("First name *") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                OutlinedTextField(
                    value = state.lastName,
                    onValueChange = { v -> viewModel.update { it.copy(lastName = v) } },
                    label = { Text("Last name *") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                OutlinedTextField(
                    value = state.otherNames,
                    onValueChange = { v -> viewModel.update { it.copy(otherNames = v) } },
                    label = { Text("Other names") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                OutlinedTextField(
                    value = state.dateOfBirth,
                    onValueChange = { v -> viewModel.update { it.copy(dateOfBirth = v) } },
                    label = { Text("Date of birth (YYYY-MM-DD)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                Row(horizontalArrangement = Arrangement.spacedBy(Spacing.sm)) {
                    GENDERS.forEach { gender ->
                        FilterChip(
                            selected = state.gender == gender,
                            onClick = {
                                viewModel.update {
                                    it.copy(gender = if (it.gender == gender) "" else gender)
                                }
                            },
                            label = {
                                Text(gender.lowercase().replaceFirstChar { c -> c.uppercase() })
                            },
                        )
                    }
                }
            }

            SectionCard(title = "Contact") {
                OutlinedTextField(
                    value = state.phone,
                    onValueChange = { v -> viewModel.update { it.copy(phone = v) } },
                    label = { Text("Phone") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                OutlinedTextField(
                    value = state.email,
                    onValueChange = { v -> viewModel.update { it.copy(email = v) } },
                    label = { Text("Email") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                OutlinedTextField(
                    value = state.address,
                    onValueChange = { v -> viewModel.update { it.copy(address = v) } },
                    label = { Text("Address") },
                    minLines = 2,
                    modifier = Modifier.fillMaxWidth(),
                )
            }

            SectionCard(title = "Next of kin") {
                OutlinedTextField(
                    value = state.nextOfKinName,
                    onValueChange = { v -> viewModel.update { it.copy(nextOfKinName = v) } },
                    label = { Text("Next of kin name") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                OutlinedTextField(
                    value = state.nextOfKinPhone,
                    onValueChange = { v -> viewModel.update { it.copy(nextOfKinPhone = v) } },
                    label = { Text("Next of kin phone") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                OutlinedTextField(
                    value = state.nextOfKinRelationship,
                    onValueChange = { v -> viewModel.update { it.copy(nextOfKinRelationship = v) } },
                    label = { Text("Relationship") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
            }

            Button(
                onClick = { viewModel.save() },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                enabled = !state.isSaving,
                shape = RoundedCornerShape(Spacing.md),
            ) {
                if (state.isSaving) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(18.dp),
                        strokeWidth = 2.dp,
                        color = MaterialTheme.colorScheme.onPrimary,
                    )
                    Spacer(modifier = Modifier.width(Spacing.sm))
                }
                Text(if (state.isSaving) "Registering…" else "Register patient")
            }
            Spacer(modifier = Modifier.height(Spacing.xl))
        }
    }
}

@Composable
private fun SectionCard(
    title: String,
    content: @Composable () -> Unit,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(Spacing.base),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
    ) {
        Column(
            modifier = Modifier.padding(Spacing.base),
            verticalArrangement = Arrangement.spacedBy(Spacing.sm),
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
            )
            content()
        }
    }
}
