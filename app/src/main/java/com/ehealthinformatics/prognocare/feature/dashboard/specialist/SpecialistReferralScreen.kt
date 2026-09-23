package com.ehealthinformatics.prognocare.feature.dashboard.specialist

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ehealthinformatics.prognocare.designsystem.components.EmptyState
import com.ehealthinformatics.prognocare.designsystem.components.StatusBadge
import com.ehealthinformatics.prognocare.designsystem.components.StatusType
import com.ehealthinformatics.prognocare.designsystem.theme.AppThemeColors
import com.ehealthinformatics.prognocare.designsystem.theme.Spacing

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SpecialistReferralScreen(
    onBack: () -> Unit,
    onPatientClick: (String) -> Unit,
    viewModel: SpecialistDashboardViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    var selectedFilter by remember { mutableStateOf("All") }

    val filterOptions = listOf("All", "Pending", "In Review", "Accepted", "Completed")

    val filteredReferrals = state.recentReferrals.filter { referral ->
        when (selectedFilter) {
            "All" -> true
            "Pending" -> referral.status == ReferralStatus.PENDING
            "In Review" -> referral.status == ReferralStatus.IN_REVIEW
            "Accepted" -> referral.status == ReferralStatus.ACCEPTED
            "Completed" -> referral.status == ReferralStatus.COMPLETED
            else -> true
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Referrals") },
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
            FloatingActionButton(
                onClick = { /* new referral */ },
                containerColor = MaterialTheme.colorScheme.primary,
            ) {
                Icon(Icons.Default.Add, contentDescription = "New Referral")
            }
        },
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
        ) {
            // ── Filter Chips ────────────────────────────────
            androidx.compose.foundation.lazy.LazyRow(
                modifier = Modifier.padding(horizontal = Spacing.lg),
                horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
            ) {
                items(filterOptions.size) { index ->
                    FilterChip(
                        selected = selectedFilter == filterOptions[index],
                        onClick = { selectedFilter = filterOptions[index] },
                        label = {
                            Text(
                                text = filterOptions[index],
                                style = MaterialTheme.typography.labelMedium,
                            )
                        },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                            selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer,
                        ),
                    )
                }
            }

            Spacer(modifier = Modifier.height(Spacing.sm))

            // ── Referrals List ──────────────────────────────
            if (filteredReferrals.isEmpty()) {
                EmptyState(
                    icon = Icons.Default.Add,
                    title = "No referrals found",
                    message = when (selectedFilter) {
                        "Pending" -> "No pending referrals at the moment"
                        "In Review" -> "No referrals currently in review"
                        else -> "No referrals match the current filter"
                    },
                )
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(Spacing.sm),
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(
                        horizontal = Spacing.lg,
                        vertical = Spacing.base,
                    ),
                ) {
                    items(filteredReferrals) { referral ->
                        ReferralDetailCard(
                            referral = referral,
                            onClick = { onPatientClick(referral.patientMrn) },
                            onAccept = { { /* referrals not backend-backed yet */ } },
                            onDecline = { { /* referrals not backend-backed yet */ } },
                        )
                    }
                    item { Spacer(modifier = Modifier.height(Spacing.xxxl)) }
                }
            }
        }
    }
}

@Composable
private fun ReferralDetailCard(
    referral: SpecialistReferral,
    onClick: () -> Unit,
    onAccept: () -> Unit,
    onDecline: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val (statusType, statusText) = when (referral.status) {
        ReferralStatus.PENDING -> StatusType.Pending to "Pending"
        ReferralStatus.IN_REVIEW -> StatusType.InProgress to "In Review"
        ReferralStatus.ACCEPTED -> StatusType.Active to "Accepted"
        ReferralStatus.DECLINED -> StatusType.Cancelled to "Declined"
        ReferralStatus.COMPLETED -> StatusType.Completed to "Completed"
    }

    val priorityColor = when (referral.priority) {
        ReferralPriority.URGENT -> MaterialTheme.colorScheme.error
        ReferralPriority.HIGH -> AppThemeColors.current.warning
        ReferralPriority.NORMAL -> MaterialTheme.colorScheme.primary
        ReferralPriority.LOW -> MaterialTheme.colorScheme.onSurfaceVariant
    }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(Spacing.base),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(Spacing.base),
        ) {
            // ── Header ──────────────────────────────────────
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primaryContainer),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            text = referral.patientName.take(1),
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                            fontWeight = FontWeight.Bold,
                        )
                    }
                    Spacer(modifier = Modifier.width(Spacing.md))
                    Column {
                        Text(
                            text = referral.patientName,
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.SemiBold,
                        )
                        Text(
                            text = "${referral.patientMrn} · ${referral.patientAge}y",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
                StatusBadge(text = statusText, type = statusType)
            }

            Spacer(modifier = Modifier.height(Spacing.md))

            // ── Referral Info ───────────────────────────────
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Column {
                    Text(
                        text = "Referred by",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Text(
                        text = referral.referringDoctor,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium,
                    )
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = "Priority",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .clip(CircleShape)
                                .background(priorityColor),
                        )
                        Spacer(modifier = Modifier.width(Spacing.xs))
                        Text(
                            text = referral.priorityDisplay,
                            style = MaterialTheme.typography.bodyMedium,
                            color = priorityColor,
                            fontWeight = FontWeight.Medium,
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(Spacing.sm))

            // ── Reason ──────────────────────────────────────
            Text(
                text = referral.referralReason,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 2,
            )

            if (referral.notes != null) {
                Spacer(modifier = Modifier.height(Spacing.xs))
                Text(
                    text = referral.notes,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            Spacer(modifier = Modifier.height(Spacing.sm))

            // ── Date ────────────────────────────────────────
            Text(
                text = "Received: ${referral.dateReceived}",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            // ── Action Buttons ──────────────────────────────
            if (referral.status == ReferralStatus.PENDING || referral.status == ReferralStatus.IN_REVIEW) {
                Spacer(modifier = Modifier.height(Spacing.md))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
                ) {
                    OutlinedButton(
                        onClick = onDecline,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(Spacing.sm),
                    ) {
                        Text("Decline")
                    }
                    Button(
                        onClick = onAccept,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(Spacing.sm),
                    ) {
                        Text("Accept Referral")
                    }
                }
            }
        }
    }
}
