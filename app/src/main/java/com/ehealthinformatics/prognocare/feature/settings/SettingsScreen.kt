package com.ehealthinformatics.prognocare.feature.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.ehealthinformatics.prognocare.designsystem.theme.Spacing

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onBack: () -> Unit,
    viewModel: SettingsViewModel = hiltViewModel(),
) {
    val config by viewModel.config.collectAsState()
    val isVerifying by viewModel.isVerifying.collectAsState()
    val saveResult by viewModel.saveResult.collectAsState()
    val dateRangeSaving by viewModel.dateRangeSaving.collectAsState()
    val dateRangeMessage by viewModel.dateRangeMessage.collectAsState()

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Text(
                        text = "Settings",
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = MaterialTheme.colorScheme.onSurface,
                        )
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                ),
            )
        },
        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(Spacing.base),
            verticalArrangement = Arrangement.spacedBy(Spacing.base),
        ) {
            ServerConfigContent(
                config = config,
                onSave = { emr, conv, channel, env ->
                    viewModel.saveConfig(emr, conv, channel, env)
                },
                onReset = { viewModel.resetToDefaults() },
                isVerifying = isVerifying,
                saveResult = saveResult,
                modifier = Modifier.fillMaxWidth(),
            )

            QueryDateRangeContent(
                config = config,
                onSave = { period -> viewModel.saveQueryRange(period) },
                isSaving = dateRangeSaving,
                saveMessage = dateRangeMessage,
                modifier = Modifier.fillMaxWidth(),
            )

            Text(
                text = "Web channel ID identifies the clinic's WhatsApp/web " +
                    "channel in the Conversation Engine. You normally do not need to change it.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = Spacing.sm),
            )

            Spacer(modifier = Modifier.height(Spacing.lg))
        }
    }
}
