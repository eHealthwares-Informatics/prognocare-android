package com.ehealthinformatics.prognocare.feature.dashboard.specialist

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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
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
import com.ehealthinformatics.prognocare.data.remote.RetrofitClient
import com.ehealthinformatics.prognocare.data.remote.models.Patient
import com.ehealthinformatics.prognocare.designsystem.components.EmptyState
import com.ehealthinformatics.prognocare.designsystem.components.ErrorState
import com.ehealthinformatics.prognocare.designsystem.theme.Spacing
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.debounce
import javax.inject.Inject

@HiltViewModel
class SpecialistPatientListViewModel @Inject constructor(
    private val retrofitClient: RetrofitClient,
) : ViewModel() {

    private val _search = MutableStateFlow("")
    val search = _search

    private val _patients = MutableStateFlow<List<Patient>>(emptyList())
    val patients = _patients

    private val _isLoading = MutableStateFlow(true)
    val isLoading = _isLoading

    private val _error = MutableStateFlow<String?>(null)
    val error = _error

    init {
        viewModelScope.launch {
            @OptIn(kotlinx.coroutines.FlowPreview::class)
            _search.debounce(300).collect { fetch(it) }
        }
    }

    fun onSearch(query: String) {
        _search.value = query
    }

    fun refresh() = fetch(_search.value)

    private fun fetch(query: String) {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null
            try {
                val response = retrofitClient.apis.value.patientApi.list(
                    page = 1,
                    limit = 50,
                    search = query.takeIf { it.isNotBlank() },
                )
                if (!response.isSuccessful) {
                    _error.value = "${response.code()} ${response.message()}"
                } else {
                    _patients.value = response.body()?.data.orEmpty()
                }
            } catch (e: Exception) {
                _error.value = e.message ?: "Network error"
            } finally {
                _isLoading.value = false
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SpecialistPatientListScreen(
    onBack: () -> Unit,
    onPatientClick: (String) -> Unit,
    viewModel: SpecialistPatientListViewModel = hiltViewModel(),
) {
    val searchQuery by viewModel.search.collectAsStateWithLifecycle()
    val patients by viewModel.patients.collectAsStateWithLifecycle()
    val isLoading by viewModel.isLoading.collectAsStateWithLifecycle()
    val error by viewModel.error.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("My Patients") },
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
                .padding(innerPadding),
        ) {
            OutlinedTextField(
                value = searchQuery,
                onValueChange = viewModel::onSearch,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = Spacing.lg, vertical = Spacing.sm),
                placeholder = { Text("Search patients...") },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                singleLine = true,
            )

            when {
                isLoading && patients.isEmpty() -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center,
                    ) { CircularProgressIndicator() }
                }
                error != null && patients.isEmpty() -> {
                    ErrorState(message = error ?: "Failed to load", onRetry = viewModel::refresh)
                }
                patients.isEmpty() -> {
                    EmptyState(
                        title = "No patients found",
                        message = "Patients matching your search will appear here.",
                    )
                }
                else -> {
                    LazyColumn(
                        contentPadding = PaddingValues(horizontal = Spacing.lg),
                        verticalArrangement = Arrangement.spacedBy(Spacing.sm),
                    ) {
                        items(patients, key = { it.id }) { patient ->
                            SpecialistPatientCard(
                                patient = patient,
                                onClick = { onPatientClick(patient.id) },
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SpecialistPatientCard(
    patient: Patient,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
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
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primaryContainer),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = patient.initials.ifBlank { "?" },
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                    fontWeight = FontWeight.Bold,
                )
            }
            Spacer(modifier = Modifier.padding(start = Spacing.md))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = patient.displayName,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Medium,
                )
                Text(
                    text = "${patient.patientId} · ${patient.gender ?: "—"}" +
                        (if (patient.ageYears > 0) " · ${patient.ageYears}y" else ""),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}
