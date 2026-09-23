package com.ehealthinformatics.prognocare.feature.dashboard.admin

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.FlowRow
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
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.FloatingActionButtonDefaults
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
import com.ehealthinformatics.prognocare.data.remote.models.CreateStaffDto
import com.ehealthinformatics.prognocare.data.remote.models.Staff
import com.ehealthinformatics.prognocare.designsystem.components.EmptyState
import com.ehealthinformatics.prognocare.designsystem.components.ErrorState
import com.ehealthinformatics.prognocare.designsystem.components.StatusBadge
import com.ehealthinformatics.prognocare.designsystem.components.StatusType
import com.ehealthinformatics.prognocare.designsystem.theme.Spacing
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class AdminStaffViewModel @Inject constructor(
    private val retrofitClient: RetrofitClient,
) : ViewModel() {

    private val _staff = MutableStateFlow<List<Staff>>(emptyList())
    val staff = _staff

    private val _isLoading = MutableStateFlow(true)
    val isLoading = _isLoading

    private val _error = MutableStateFlow<String?>(null)
    val error = _error

    init {
        load()
    }

    fun load() {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null
            try {
                val response = retrofitClient.apis.value.staffApi.list(limit = 100)
                if (!response.isSuccessful) {
                    _error.value = "${response.code()} ${response.message()}"
                } else {
                    _staff.value = response.body()?.data.orEmpty()
                }
            } catch (e: Exception) {
                _error.value = e.message ?: "Network error"
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun addStaff(firstName: String, lastName: String, roleType: String, email: String?) {
        viewModelScope.launch {
            try {
                val response = retrofitClient.apis.value.staffApi.create(
                    CreateStaffDto(
                        firstName = firstName,
                        lastName = lastName,
                        roleType = roleType,
                        email = email?.takeIf { it.isNotBlank() },
                    ),
                )
                if (response.isSuccessful) load() else _error.value = "${response.code()} ${response.message()}"
            } catch (e: Exception) {
                _error.value = e.message ?: "Could not add staff"
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminStaffManagementScreen(
    onBack: () -> Unit,
    onAddStaff: () -> Unit = {},
    viewModel: AdminStaffViewModel = hiltViewModel(),
) {
    var selectedFilter by remember { mutableStateOf("All") }
    var showAddDialog by remember { mutableStateOf(false) }
    val filters = listOf("All", "Doctors", "Nurses", "Staff", "Inactive")
    val staff by viewModel.staff.collectAsStateWithLifecycle()
    val isLoading by viewModel.isLoading.collectAsStateWithLifecycle()
    val error by viewModel.error.collectAsStateWithLifecycle()

    val filteredStaff = staff.filter { member ->
        when (selectedFilter) {
            "All" -> true
            "Doctors" -> member.roleType.equals("Doctor", ignoreCase = true)
            "Nurses" -> member.roleType.equals("Nurse", ignoreCase = true)
            "Staff" -> member.roleType !in listOf("Doctor", "Nurse")
            "Inactive" -> !member.isActive
            else -> true
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Staff Management") },
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
                onClick = { showAddDialog = true },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
                shape = RoundedCornerShape(Spacing.lg),
                elevation = FloatingActionButtonDefaults.elevation(defaultElevation = 6.dp),
            ) {
                Icon(Icons.Default.Add, contentDescription = null)
                Spacer(modifier = Modifier.width(Spacing.sm))
                Text("Add Staff", fontWeight = FontWeight.SemiBold)
            }
        },
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
        ) {
            // ── Filter Chips ─────────────────────────────────
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = Spacing.lg, vertical = Spacing.sm),
                horizontalArrangement = Arrangement.spacedBy(Spacing.xs),
            ) {
                filters.forEach { filter ->
                    FilterChip(
                        selected = selectedFilter == filter,
                        onClick = { selectedFilter = filter },
                        label = {
                            Text(
                                text = filter,
                                style = MaterialTheme.typography.labelSmall,
                            )
                        },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                            selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer,
                        ),
                    )
                }
            }

            when {
                isLoading && staff.isEmpty() -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center,
                    ) { CircularProgressIndicator() }
                }
                error != null && staff.isEmpty() -> {
                    ErrorState(message = error ?: "Failed to load", onRetry = viewModel::load)
                }
                filteredStaff.isEmpty() -> {
                    EmptyState(
                        title = "No staff",
                        message = "Staff registered in the EMR will appear here.",
                    )
                }
                else -> {
                    LazyColumn(
                        contentPadding = PaddingValues(horizontal = Spacing.lg, vertical = Spacing.sm),
                        verticalArrangement = Arrangement.spacedBy(Spacing.sm),
                    ) {
                        items(filteredStaff, key = { it.id }) { member ->
                            StaffMemberCard(member = member)
                        }
                    }
                }
            }
        }

        if (showAddDialog) {
            AddStaffDialog(
                onDismiss = { showAddDialog = false },
                onConfirm = { first, last, role, email ->
                    showAddDialog = false
                    viewModel.addStaff(first, last, role, email)
                },
            )
        }
    }
}

@OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class)
@Composable
private fun AddStaffDialog(
    onDismiss: () -> Unit,
    onConfirm: (firstName: String, lastName: String, roleType: String, email: String?) -> Unit,
) {
    var firstName by remember { mutableStateOf("") }
    var lastName by remember { mutableStateOf("") }
    var role by remember { mutableStateOf("Doctor") }
    var email by remember { mutableStateOf("") }
    val roles = listOf("Doctor", "Nurse", "Technician", "Therapist", "Admin", "Support")

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add Staff") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(Spacing.sm)) {
                OutlinedTextField(
                    value = firstName,
                    onValueChange = { firstName = it },
                    label = { Text("First name") },
                    singleLine = true,
                )
                OutlinedTextField(
                    value = lastName,
                    onValueChange = { lastName = it },
                    label = { Text("Last name") },
                    singleLine = true,
                )
                androidx.compose.foundation.layout.FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(Spacing.xs),
                ) {
                    roles.forEach { r ->
                        FilterChip(
                            selected = role == r,
                            onClick = { role = r },
                            label = { Text(r) },
                        )
                    }
                }
                OutlinedTextField(
                    value = email,
                    onValueChange = { email = it },
                    label = { Text("Email (optional)") },
                    singleLine = true,
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = { onConfirm(firstName.trim(), lastName.trim(), role, email.trim()) },
                enabled = firstName.isNotBlank() && lastName.isNotBlank(),
            ) { Text("Add") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        },
    )
}

@Composable
private fun StaffMemberCard(member: Staff) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(Spacing.base),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
    ) {
        Row(
            modifier = Modifier.padding(Spacing.base),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primaryContainer),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    Icons.Default.Person,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onPrimaryContainer,
                    modifier = Modifier.size(24.dp),
                )
            }
            Spacer(modifier = Modifier.width(Spacing.md))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = member.displayName,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Text(
                    text = "${member.roleDisplay} • ${member.department ?: "—"}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                if (!member.email.isNullOrBlank()) {
                    Text(
                        text = member.email,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            StatusBadge(
                text = if (member.isActive) "Active" else "Inactive",
                type = if (member.isActive) StatusType.Active else StatusType.Cancelled,
            )
        }
    }
}
