package com.ehealthwares.prognocare.feature.dashboard.finance

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.ehealthwares.prognocare.designsystem.components.StatusBadge
import com.ehealthwares.prognocare.designsystem.components.StatusType
import com.ehealthwares.prognocare.designsystem.theme.AppThemeColors
import com.ehealthwares.prognocare.designsystem.theme.Spacing

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FinanceBillDetailScreen(
    billId: String,
    onBack: () -> Unit,
    onRecordPayment: (String) -> Unit,
    viewModel: FinanceDashboardViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsState()
    val bill = state.recentBills.find { it.id == billId }

    if (bill == null) {
        onBack()
        return
    }

    val billStatusType = when (bill.status) {
        BillStatus.DRAFT -> StatusType.Draft
        BillStatus.PENDING -> StatusType.Pending
        BillStatus.PARTIAL -> StatusType.InProgress
        BillStatus.PAID -> StatusType.Completed
        BillStatus.OVERDUE -> StatusType.Urgent
        BillStatus.CANCELLED -> StatusType.Cancelled
    }
    val billStatusText = bill.status.name.lowercase().replaceFirstChar { it.uppercase() }
    val items = bill.lineItems

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(bill.billNumber) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface),
            )
        },
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(innerPadding),
            verticalArrangement = Arrangement.spacedBy(Spacing.base),
        ) {
            // Bill Header
            item {
                Card(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = Spacing.lg),
                    shape = RoundedCornerShape(Spacing.base),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                ) {
                    Column(modifier = Modifier.fillMaxWidth().padding(Spacing.base)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Column {
                                Text(bill.patientName, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                                Text("MRN: ${bill.patientMrn}", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            StatusBadge(text = billStatusText, type = billStatusType)
                        }
                        Spacer(modifier = Modifier.height(Spacing.md))
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Column {
                                Text("Bill Date", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Text(bill.date, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
                            }
                            Column(horizontalAlignment = Alignment.End) {
                                Text("Due Date", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Text(bill.dueDate, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium,
                                    color = if (bill.status == BillStatus.OVERDUE) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface)
                            }
                        }
                        if (bill.notes != null) {
                            Spacer(modifier = Modifier.height(Spacing.md))
                            Text("Notes", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text(bill.notes, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
            }

            // Payment Summary
            item {
                Card(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = Spacing.lg),
                    shape = RoundedCornerShape(Spacing.base),
                    colors = CardDefaults.cardColors(containerColor = AppThemeColors.current.kpiBlueLight),
                ) {
                    Column(modifier = Modifier.fillMaxWidth().padding(Spacing.base)) {
                        Text("Payment Summary", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.primary)
                        Spacer(modifier = Modifier.height(Spacing.md))
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Column {
                                Text("Total Amount", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Text(bill.totalDisplay, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                            }
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text("Paid", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Text(bill.paidDisplay, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = AppThemeColors.current.kpiGreen)
                            }
                            Column(horizontalAlignment = Alignment.End) {
                                Text("Balance", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Text(bill.balanceDisplay, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold,
                                    color = if (bill.balanceAmount > 0) AppThemeColors.current.kpiOrange else AppThemeColors.current.kpiGreen)
                            }
                        }
                    }
                }
            }

            // Line Items Header
            item {
                Column(modifier = Modifier.padding(horizontal = Spacing.lg)) {
                    Text("Line Items", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                }
            }

            // Line Items
            item {
                Column(
                    modifier = Modifier.padding(horizontal = Spacing.lg),
                    verticalArrangement = Arrangement.spacedBy(Spacing.sm),
                ) {
                    items.forEach { item ->
                        LineItemRow(item = item)
                    }
                }
            }

            // Total
            item {
                Card(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = Spacing.lg),
                    shape = RoundedCornerShape(Spacing.base),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                ) {
                    Column(modifier = Modifier.fillMaxWidth().padding(Spacing.base)) {
                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                        Spacer(modifier = Modifier.height(Spacing.md))
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                            Text("Total (${items.size} items)", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                            Text(bill.totalDisplay, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                        }
                    }
                }
            }

            // Record Payment Button
            if (bill.balanceAmount > 0) {
                item {
                    Button(
                        onClick = { onRecordPayment(bill.id) },
                        modifier = Modifier.fillMaxWidth().padding(horizontal = Spacing.lg).height(56.dp),
                        shape = RoundedCornerShape(Spacing.md),
                    ) {
                        Text("Record Payment", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                    }
                }
            }

            item { Spacer(modifier = Modifier.height(Spacing.xxl)) }
        }
    }
}

@Composable
private fun LineItemRow(item: BillLineItem) {
    val (iconBg, iconTint) = when (item.category) {
        ItemCategory.CONSULTATION -> AppThemeColors.current.kpiBlueLight to AppThemeColors.current.kpiBlue
        ItemCategory.PROCEDURE -> AppThemeColors.current.kpiGreenLight to AppThemeColors.current.kpiGreen
        ItemCategory.LAB_TEST -> MaterialTheme.colorScheme.errorContainer to MaterialTheme.colorScheme.error
        ItemCategory.IMAGING -> AppThemeColors.current.kpiPurpleLight to AppThemeColors.current.kpiPurple
        ItemCategory.MEDICATION -> AppThemeColors.current.kpiOrangeLight to AppThemeColors.current.kpiOrange
        ItemCategory.ROOM_CHARGE -> AppThemeColors.current.kpiPurpleLight to AppThemeColors.current.kpiPurple
        ItemCategory.OTHER -> MaterialTheme.colorScheme.surfaceVariant to MaterialTheme.colorScheme.onSurfaceVariant
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(Spacing.sm),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.5.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(Spacing.md),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier.size(36.dp).clip(CircleShape).background(iconBg),
                contentAlignment = Alignment.Center,
            ) {
                Text(item.category.name.take(1), style = MaterialTheme.typography.labelMedium, color = iconTint, fontWeight = FontWeight.Bold)
            }
            Spacer(modifier = Modifier.width(Spacing.md))
            Column(modifier = Modifier.weight(1f)) {
                Text(item.description, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
                Text(
                    "${item.category.name.replace("_", " ").lowercase().replaceFirstChar { it.uppercase() }} · Qty: ${item.quantity}",
                    style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Column(horizontalAlignment = Alignment.End) {
                Text(item.totalDisplay, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurface)
                if (item.quantity > 1) {
                    Text("@ ${item.unitPriceDisplay} each", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
    }
}
