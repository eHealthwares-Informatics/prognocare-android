package com.ehealthinformatics.prognocare.feature.requests

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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ehealthinformatics.prognocare.designsystem.components.EmptyState
import com.ehealthinformatics.prognocare.designsystem.components.ErrorState
import com.ehealthinformatics.prognocare.designsystem.components.StatusBadge
import com.ehealthinformatics.prognocare.designsystem.components.StatusType
import com.ehealthinformatics.prognocare.designsystem.theme.Spacing

private val TYPE_FILTERS = listOf(
    "All" to null,
    "Prescription" to "PRESCRIPTION",
    "Lab" to "LAB",
    "Radiology" to "RADIOLOGY",
    "Other Test" to "OTHER_TEST",
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RequestsListScreen(
    title: String = "Requests",
    onBack: () -> Unit,
    onRequestClick: (String) -> Unit,
    onCreateRequest: (() -> Unit)? = null,
    viewModel: RequestsListViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    Scaffold(
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
            LazyRow(
                contentPadding = PaddingValues(horizontal = Spacing.lg),
                horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
            ) {
                items(TYPE_FILTERS) { (label, value) ->
                    FilterChip(
                        selected = state.filterType == value,
                        onClick = { viewModel.filter(value) },
                        label = { Text(label) },
                    )
                }
            }

            when {
                state.isLoading -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center,
                    ) { CircularProgressIndicator() }
                }
                state.error != null -> {
                    ErrorState(message = state.error ?: "Failed to load", onRetry = { viewModel.load() })
                }
                state.requests.isEmpty() -> {
                    EmptyState(
                        title = "No requests",
                        message = if (state.filterType == null) {
                            "Clinical requests you place will appear here."
                        } else {
                            "No ${TYPE_FILTERS.first { it.second == state.filterType }.first} requests yet."
                        },
                    )
                }
                else -> {
                    LazyColumn(
                        contentPadding = PaddingValues(Spacing.lg),
                        verticalArrangement = Arrangement.spacedBy(Spacing.md),
                    ) {
                        items(state.requests, key = { it.id }) { request ->
                            RequestCard(request = request, onClick = { onRequestClick(request.id) })
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun RequestStatusBadge(status: String, modifier: Modifier = Modifier) {
    val type = when (status) {
        "COMPLETED" -> StatusType.Completed
        "CANCELLED", "REJECTED" -> StatusType.Cancelled
        "IN_PROGRESS" -> StatusType.InProgress
        else -> StatusType.Pending
    }
    StatusBadge(
        text = status.replace("_", " ").lowercase().replaceFirstChar { it.uppercase() },
        type = type,
        modifier = modifier,
    )
}

@Composable
private fun RequestCard(
    request: com.ehealthinformatics.prognocare.data.remote.models.ClinicalRequest,
    onClick: () -> Unit,
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(Spacing.base),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
    ) {
        Column(modifier = Modifier.padding(Spacing.base)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = request.typeDisplay,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                )
                RequestStatusBadge(request.status)
            }
            Spacer(modifier = Modifier.padding(top = Spacing.xs))
            Text(
                text = listOfNotNull(
                    request.patientName.takeIf { it.isNotBlank() },
                    request.requestNumber,
                    request.diagnosis,
                ).joinToString(" · "),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(
                    text = "${request.items.size} item(s) · ${request.priorityDisplay}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary,
                )
                Text(
                    text = request.requestedAt?.take(10) ?: "",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}
