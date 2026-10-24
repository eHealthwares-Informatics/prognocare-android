package com.ehealthwares.prognocare.feature.dashboard.technician

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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Biotech
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.MedicalServices
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ehealthwares.prognocare.designsystem.components.StatusBadge
import com.ehealthwares.prognocare.designsystem.components.StatusType
import com.ehealthwares.prognocare.designsystem.theme.AppThemeColors
import com.ehealthwares.prognocare.designsystem.theme.Spacing

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TechnicianOrdersScreen(
    onBack: () -> Unit,
    viewModel: TechnicianDashboardViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    var selectedFilter by remember { mutableStateOf("All") }
    val filters = listOf("All", "Received", "In Progress", "Urgent", "Completed")

    val filteredOrders = state.pendingOrdersList.filter { order ->
        when (selectedFilter) {
            "All" -> true
            "Received" -> order.status == OrderStatus.RECEIVED
            "In Progress" -> order.status in listOf(OrderStatus.IN_PROGRESS, OrderStatus.PROCESSING, OrderStatus.SAMPLE_COLLECTED)
            "Urgent" -> order.priority == OrderPriority.STAT || order.priority == OrderPriority.URGENT
            "Completed" -> order.status == OrderStatus.COMPLETED
            else -> true
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Lab Orders") },
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

            // ── Orders List ──────────────────────────────────
            LazyColumn(
                contentPadding = PaddingValues(horizontal = Spacing.lg, vertical = Spacing.sm),
                verticalArrangement = Arrangement.spacedBy(Spacing.sm),
            ) {
                items(filteredOrders) { order ->
                    TechnicianOrderListItem(
                        order = order,
                        onStartProcessing = { viewModel.updateOrderStatus(order.id, OrderStatus.IN_PROGRESS) },
                        onMarkSampleCollected = { viewModel.updateOrderStatus(order.id, OrderStatus.SAMPLE_COLLECTED) },
                        onComplete = { viewModel.updateOrderStatus(order.id, OrderStatus.COMPLETED) },
                    )
                }
            }
        }
    }
}

@Composable
private fun TechnicianOrderListItem(
    order: TechnicianOrder,
    onStartProcessing: () -> Unit,
    onMarkSampleCollected: () -> Unit,
    onComplete: () -> Unit,
) {
    val isStat = order.priority == OrderPriority.STAT

    androidx.compose.material3.Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(Spacing.base),
        colors = androidx.compose.material3.CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface,
        ),
        elevation = androidx.compose.material3.CardDefaults.cardElevation(
            defaultElevation = if (isStat) 4.dp else 1.dp,
        ),
    ) {
        Column(
            modifier = Modifier.padding(Spacing.base),
        ) {
            // ── Header ──────────────────────────────────────
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top,
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = order.testName,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Text(
                        text = order.patientName,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = if (isStat) "STAT" else order.priority.displayName,
                        style = MaterialTheme.typography.labelSmall,
                        color = if (isStat) AppThemeColors.current.critical else MaterialTheme.colorScheme.onSurfaceVariant,
                        fontWeight = FontWeight.Bold,
                    )
                    Spacer(modifier = Modifier.height(Spacing.xs))
                    TechnicianOrderStatusBadge(status = order.status)
                }
            }

            Spacer(modifier = Modifier.height(Spacing.sm))

            // ── Details ──────────────────────────────────────
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(Spacing.md),
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Default.Person,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(12.dp),
                    )
                    Spacer(modifier = Modifier.width(Spacing.xxs))
                    Text(order.orderedBy, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Default.MedicalServices,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(12.dp),
                    )
                    Spacer(modifier = Modifier.width(Spacing.xxs))
                    Text(order.orderType.displayName, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }

            if (order.dueTime != null) {
                Spacer(modifier = Modifier.height(Spacing.xs))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Default.ErrorOutline,
                        contentDescription = null,
                        tint = if (isStat) AppThemeColors.current.critical else AppThemeColors.current.warning,
                        modifier = Modifier.size(14.dp),
                    )
                    Spacer(modifier = Modifier.width(Spacing.xs))
                    Text(
                        "Due: ${order.dueTime}",
                        style = MaterialTheme.typography.labelSmall,
                        color = if (isStat) AppThemeColors.current.critical else AppThemeColors.current.warning,
                        fontWeight = FontWeight.Medium,
                    )
                }
            }

            if (order.notes != null) {
                Spacer(modifier = Modifier.height(Spacing.xs))
                Text(
                    order.notes,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontStyle = androidx.compose.ui.text.font.FontStyle.Italic,
                )
            }

            // ── Progress bar ─────────────────────────────────
            if (order.status == OrderStatus.IN_PROGRESS || order.status == OrderStatus.PROCESSING) {
                Spacer(modifier = Modifier.height(Spacing.sm))
                val progress = if (order.status == OrderStatus.PROCESSING) 0.7f else 0.3f
                LinearProgressIndicator(
                    progress = { progress },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(4.dp)
                        .clip(RoundedCornerShape(2.dp)),
                    color = MaterialTheme.colorScheme.primary,
                    trackColor = MaterialTheme.colorScheme.surfaceVariant,
                )
            }

            Spacer(modifier = Modifier.height(Spacing.sm))

            // ── Actions ──────────────────────────────────────
            when (order.status) {
                OrderStatus.RECEIVED -> {
                    OutlinedButton(
                        onClick = onStartProcessing,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(Spacing.sm),
                    ) {
                        Icon(Icons.Default.Biotech, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(Spacing.xs))
                        Text("Start Processing")
                    }
                }
                OrderStatus.IN_PROGRESS -> {
                    OutlinedButton(
                        onClick = onMarkSampleCollected,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(Spacing.sm),
                    ) {
                        Text("Sample Collected")
                    }
                }
                OrderStatus.SAMPLE_COLLECTED, OrderStatus.PROCESSING -> {
                    TextButton(
                        onClick = onComplete,
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Icon(Icons.Default.CheckCircle, contentDescription = null, modifier = Modifier.size(16.dp), tint = MaterialTheme.colorScheme.primary)
                        Spacer(modifier = Modifier.width(Spacing.xs))
                        Text("Complete")
                    }
                }
                else -> {}
            }
        }
    }
}

@Composable
private fun TechnicianOrderStatusBadge(status: OrderStatus) {
    val (text, type) = when (status) {
        OrderStatus.RECEIVED -> "Received" to StatusType.Pending
        OrderStatus.IN_PROGRESS -> "Processing" to StatusType.InProgress
        OrderStatus.SAMPLE_COLLECTED -> "Sample Ready" to StatusType.Active
        OrderStatus.PROCESSING -> "Processing" to StatusType.InProgress
        OrderStatus.COMPLETED -> "Completed" to StatusType.Completed
        OrderStatus.CANCELLED -> "Cancelled" to StatusType.Cancelled
    }
    StatusBadge(text = text, type = type)
}
