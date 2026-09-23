package com.ehealthinformatics.prognocare.feature.dashboard.technician

import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ehealthinformatics.prognocare.designsystem.components.StatusBadge
import com.ehealthinformatics.prognocare.designsystem.components.StatusType
import com.ehealthinformatics.prognocare.designsystem.theme.AppThemeColors
import com.ehealthinformatics.prognocare.designsystem.theme.Spacing

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TechnicianResultsScreen(
    onBack: () -> Unit,
    viewModel: TechnicianDashboardViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    var selectedFilter by remember { mutableStateOf("All") }
    val filters = listOf("All", "Normal", "Abnormal", "Pending Review")

    val filteredResults = state.recentResults.filter { result ->
        when (selectedFilter) {
            "All" -> true
            "Normal" -> !result.isAbnormal
            "Abnormal" -> result.isAbnormal
            "Pending Review" -> result.reviewedBy == null
            else -> true
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Results") },
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
            com.ehealthinformatics.prognocare.designsystem.components.DemoDataChip(
                text = "Result files live in the LIS — details below come from completed lab orders",
            )

            // ── Filter Chips ─────────────────────────────────
            androidx.compose.foundation.layout.Row(
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

            // ── Results List ─────────────────────────────────
            LazyColumn(
                contentPadding = PaddingValues(horizontal = Spacing.lg, vertical = Spacing.sm),
                verticalArrangement = Arrangement.spacedBy(Spacing.sm),
            ) {
                items(filteredResults) { result ->
                    TechnicianResultListItem(result = result)
                }
            }
        }
    }
}

@Composable
private fun TechnicianResultListItem(result: TechnicianResult) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(Spacing.base),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface,
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
    ) {
        Column(
            modifier = Modifier.padding(Spacing.base),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = result.testName,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Text(
                        text = result.patientName,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                if (result.isAbnormal) {
                    StatusBadge(text = "Abnormal", type = StatusType.Urgent)
                } else {
                    StatusBadge(text = "Normal", type = StatusType.Completed)
                }
            }

            Spacer(modifier = Modifier.height(Spacing.xs))

            Text(
                text = result.resultSummary,
                style = MaterialTheme.typography.bodySmall,
                color = if (result.isAbnormal) AppThemeColors.current.critical else MaterialTheme.colorScheme.onSurfaceVariant,
                fontWeight = if (result.isAbnormal) FontWeight.Medium else FontWeight.Normal,
                maxLines = 3,
                overflow = TextOverflow.Ellipsis,
            )

            Spacer(modifier = Modifier.height(Spacing.sm))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = "Uploaded ${result.uploadedAt}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                if (result.reviewedBy != null) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Default.CheckCircle,
                            contentDescription = null,
                            tint = AppThemeColors.current.success,
                            modifier = Modifier.size(12.dp),
                        )
                        Spacer(modifier = Modifier.width(Spacing.xxs))
                        Text(
                            result.reviewedBy,
                            style = MaterialTheme.typography.labelSmall,
                            color = AppThemeColors.current.success,
                        )
                    }
                } else {
                    Text(
                        text = "Awaiting review",
                        style = MaterialTheme.typography.labelSmall,
                        color = AppThemeColors.current.warning,
                    )
                }
            }
        }
    }
}
