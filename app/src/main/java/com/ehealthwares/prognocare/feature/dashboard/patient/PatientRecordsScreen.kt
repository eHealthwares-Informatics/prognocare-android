package com.ehealthwares.prognocare.feature.dashboard.patient

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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Attachment
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.outlined.Bloodtype
import androidx.compose.material.icons.outlined.Description
import androidx.compose.material.icons.outlined.MedicalServices
import androidx.compose.material.icons.outlined.Science
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.ehealthwares.prognocare.designsystem.components.EmptyState
import com.ehealthwares.prognocare.designsystem.theme.Spacing

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PatientRecordsScreen(
    onBack: () -> Unit,
) {
    var searchQuery by remember { mutableStateOf("") }
    var selectedFilter by remember { mutableStateOf("All") }

    val allRecords = remember {
        listOf(
            PatientRecord("1", "Lab Results - Complete Blood Count", "LAB_RESULTS",
                "Aug 5, 2026", "Dr. Adebayo", "All values within normal range. Hemoglobin 14.2 g/dL, WBC 6,800/μL, Platelets 245,000/μL.", true),
            PatientRecord("2", "ECG Report", "PROCEDURE",
                "Jul 28, 2026", "Dr. Ibrahim", "Normal sinus rhythm, heart rate 72 bpm, no ST changes, no arrhythmias detected.", true),
            PatientRecord("3", "Clinical Note - Diabetes Follow-up", "CLINICAL_NOTE",
                "Jul 15, 2026", "Dr. Fatima", "HbA1c 7.2%, target < 7.0%. Fasting glucose 126 mg/dL. Continue Metformin 500mg BID. Diet compliance improved.", true),
            PatientRecord("4", "Lab Results - Lipid Panel", "LAB_RESULTS",
                "Jul 1, 2026", "Dr. Adebayo", "Total cholesterol 198 mg/dL, LDL 118 mg/dL, HDL 52 mg/dL, Triglycerides 140 mg/dL. borderline.", true),
            PatientRecord("5", "Prescription - Lisinopril", "PRESCRIPTION",
                "Jan 15, 2026", "Dr. Adebayo", "Lisinopril 10mg, once daily, oral. For hypertension management. Refill x3.", false),
            PatientRecord("6", "Radiology - Chest X-Ray", "IMAGING",
                "Mar 20, 2026", "Dr. Ibrahim", "No acute cardiopulmonary disease. Heart size normal. Lungs clear bilaterally.", true),
            PatientRecord("7", "Clinical Note - Hypertension Follow-up", "CLINICAL_NOTE",
                "Jan 15, 2026", "Dr. Adebayo", "BP 138/86 mmHg. Lisinopril 10mg started. Lifestyle counseling provided. Follow up in 4 weeks.", true),
            PatientRecord("8", "Lab Results - HbA1c", "LAB_RESULTS",
                "Apr 10, 2026", "Dr. Fatima", "HbA1c 7.8%. Previous 8.1%. Improving trend. Continue current regimen.", true),
        )
    }

    val filterOptions = listOf("All", "Lab Results", "Clinical Notes", "Imaging", "Procedures", "Prescriptions")

    val filteredRecords = allRecords.filter { record ->
        val matchesSearch = searchQuery.isEmpty() ||
                record.title.contains(searchQuery, ignoreCase = true) ||
                record.providerName.contains(searchQuery, ignoreCase = true) ||
                record.summary.contains(searchQuery, ignoreCase = true)
        val matchesFilter = when (selectedFilter) {
            "All" -> true
            "Lab Results" -> record.type == "LAB_RESULTS"
            "Clinical Notes" -> record.type == "CLINICAL_NOTE"
            "Imaging" -> record.type == "IMAGING"
            "Procedures" -> record.type == "PROCEDURE"
            "Prescriptions" -> record.type == "PRESCRIPTION"
            else -> true
        }
        matchesSearch && matchesFilter
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Health Records") },
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
            // ── Search Bar ──────────────────────────────────
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = Spacing.lg, vertical = Spacing.sm),
                placeholder = { Text("Search records...") },
                leadingIcon = {
                    Icon(
                        Icons.Outlined.Description,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                },
                singleLine = true,
                shape = RoundedCornerShape(Spacing.base),
                colors = OutlinedTextFieldDefaults.colors(
                    unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                    focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                ),
            )

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

            // ── Records List ────────────────────────────────
            if (filteredRecords.isEmpty()) {
                EmptyState(
                    icon = Icons.Outlined.Description,
                    title = "No records found",
                    message = if (searchQuery.isNotEmpty())
                        "No records match \"$searchQuery\""
                    else
                        "No records in this category yet",
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
                    items(filteredRecords) { record ->
                        PatientRecordDetailCard(record = record)
                    }
                    item { Spacer(modifier = Modifier.height(Spacing.xxxl)) }
                }
            }
        }
    }
}

@Composable
private fun PatientRecordDetailCard(
    record: PatientRecord,
    modifier: Modifier = Modifier,
) {
    val (icon, iconBg, iconTint) = when (record.type) {
        "LAB_RESULTS" -> Triple(
            Icons.Outlined.Science,
            MaterialTheme.colorScheme.tertiaryContainer,
            MaterialTheme.colorScheme.onTertiaryContainer,
        )
        "CLINICAL_NOTE" -> Triple(
            Icons.Outlined.Description,
            MaterialTheme.colorScheme.primaryContainer,
            MaterialTheme.colorScheme.onPrimaryContainer,
        )
        "IMAGING" -> Triple(
            Icons.Outlined.Bloodtype,
            MaterialTheme.colorScheme.secondaryContainer,
            MaterialTheme.colorScheme.onSecondaryContainer,
        )
        "PROCEDURE" -> Triple(
            Icons.Outlined.MedicalServices,
            MaterialTheme.colorScheme.errorContainer,
            MaterialTheme.colorScheme.onErrorContainer,
        )
        "PRESCRIPTION" -> Triple(
            Icons.Outlined.MedicalServices,
            MaterialTheme.colorScheme.primaryContainer,
            MaterialTheme.colorScheme.onPrimaryContainer,
        )
        else -> Triple(
            Icons.Default.Description,
            MaterialTheme.colorScheme.surfaceVariant,
            MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable { /* view record detail */ },
        shape = RoundedCornerShape(Spacing.base),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(Spacing.base),
            verticalAlignment = Alignment.Top,
        ) {
            // ── Type Icon ───────────────────────────────────
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(RoundedCornerShape(Spacing.sm))
                    .background(iconBg),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = iconTint,
                    modifier = Modifier.size(22.dp),
                )
            }

            Spacer(modifier = Modifier.width(Spacing.md))

            // ── Content ─────────────────────────────────────
            Column(modifier = Modifier.weight(1f)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = record.title,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.weight(1f),
                    )
                    if (record.hasAttachment) {
                        Icon(
                            imageVector = Icons.Default.Attachment,
                            contentDescription = "Has attachment",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(16.dp),
                        )
                    }
                }

                Spacer(modifier = Modifier.height(Spacing.xxs))

                Text(
                    text = record.typeDisplay,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Medium,
                )

                Spacer(modifier = Modifier.height(Spacing.xs))

                Text(
                    text = "${record.providerName} · ${record.date}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )

                Spacer(modifier = Modifier.height(Spacing.xs))

                Text(
                    text = record.summary,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 3,
                )
            }
        }
    }
}
