package com.ehealthinformatics.prognocare.feature.profile

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
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
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.automirrored.outlined.Help
import androidx.compose.material.icons.filled.Badge
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.outlined.Business
import androidx.compose.material.icons.outlined.Email
import androidx.compose.material.icons.outlined.Phone
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ehealthinformatics.prognocare.data.config.AppConfig
import com.ehealthinformatics.prognocare.designsystem.theme.AppearanceMode
import com.ehealthinformatics.prognocare.designsystem.theme.AppThemeColors
import com.ehealthinformatics.prognocare.designsystem.theme.Primary
import com.ehealthinformatics.prognocare.designsystem.theme.Spacing
import com.ehealthinformatics.prognocare.designsystem.theme.ThemeViewModel
import com.ehealthinformatics.prognocare.designsystem.theme.ThemeSettingsScreen
import com.ehealthinformatics.prognocare.feature.settings.ServerConfigContent
import com.ehealthinformatics.prognocare.feature.settings.TapCounter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UserProfileScreen(
    onBack: () -> Unit,
    onSignOut: () -> Unit,
    onOpenServerSettings: () -> Unit = {},
    viewModel: UserProfileViewModel = hiltViewModel(),
    themeViewModel: ThemeViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val signOutComplete by viewModel.signOutComplete.collectAsStateWithLifecycle()
    val themeSettings by themeViewModel.themeSettings.collectAsStateWithLifecycle()
    val appConfig by viewModel.appConfig.collectAsStateWithLifecycle()
    var showSignOutDialog by remember { mutableStateOf(false) }
    var notificationsEnabled by remember { mutableStateOf(true) }
    var showThemeSettings by remember { mutableStateOf(false) }
    var showServerConfig by remember { mutableStateOf(false) }
    var tapProgress by remember { mutableStateOf(0) }
    val tapCounter = remember { TapCounter() }

    LaunchedEffect(signOutComplete) {
        if (signOutComplete) {
            onSignOut()
        }
    }

    if (showThemeSettings) {
        ThemeSettingsScreen(
            onBack = { showThemeSettings = false },
            viewModel = themeViewModel
        )
        return
    }

    if (showSignOutDialog) {
        AlertDialog(
            onDismissRequest = { showSignOutDialog = false },
            title = {
                Text(
                    text = "Sign Out",
                    fontWeight = FontWeight.Bold,
                )
            },
            text = {
                Text("Are you sure you want to sign out from PrognoCare?")
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        showSignOutDialog = false
                        viewModel.signOut()
                    },
                ) {
                    Text(
                        text = "Sign Out",
                        color = MaterialTheme.colorScheme.error,
                        fontWeight = FontWeight.SemiBold,
                    )
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { showSignOutDialog = false },
                ) {
                    Text(text = "Cancel")
                }
            },
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Profile",
                        fontWeight = FontWeight.Bold,
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.Filled.ChevronRight,
                            contentDescription = "Back",
                            modifier = Modifier
                                .size(28.dp)
                                .padding(0.dp),
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                ),
            )
        },
    ) { innerPadding ->
        if (state.isLoading) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                contentAlignment = Alignment.Center,
            ) {
                CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
            }
        } else {
            val profile = state.profile ?: return@Scaffold

            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                contentPadding = PaddingValues(bottom = 100.dp),
                verticalArrangement = Arrangement.spacedBy(Spacing.base),
            ) {
                // ── Profile Header ─────────────────────────────
                item {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = Spacing.lg),
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        // Avatar
                        Box(
                            modifier = Modifier
                                .size(100.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.primary),
                            contentAlignment = Alignment.Center,
                        ) {
                            Text(
                                text = profile.name.take(1),
                                style = MaterialTheme.typography.displaySmall,
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                            )
                        }

                        Spacer(modifier = Modifier.height(Spacing.md))

                        // Name
                        Text(
                            text = profile.name,
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface,
                        )

                        Spacer(modifier = Modifier.height(Spacing.xs))

                        // Role badge
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(20.dp))
                                .background(AppThemeColors.current.kpiBlueLight)
                                .padding(horizontal = Spacing.md, vertical = Spacing.xs),
                        ) {
                            Text(
                                text = profile.role.displayName,
                                style = MaterialTheme.typography.labelMedium,
                                color = AppThemeColors.current.kpiBlue,
                                fontWeight = FontWeight.SemiBold,
                            )
                        }

                        Spacer(modifier = Modifier.height(Spacing.sm))

                        // Department & Facility
                        Text(
                            text = profile.department,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        Text(
                            text = profile.facility,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }

                // ── Contact Information ───────────────────────
                item {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = Spacing.lg),
                        shape = RoundedCornerShape(Spacing.base),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surface,
                        ),
                        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                    ) {
                        Column(
                            modifier = Modifier.padding(Spacing.base),
                        ) {
                            Text(
                                text = "Contact Information",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurface,
                            )
                            Spacer(modifier = Modifier.height(Spacing.md))

                            ProfileInfoRow(
                                icon = Icons.Outlined.Email,
                                iconTint = AppThemeColors.current.kpiBlue,
                                iconBg = AppThemeColors.current.kpiBlueLight,
                                label = "Email",
                                value = profile.email,
                            )
                            HorizontalDivider(
                                modifier = Modifier.padding(vertical = Spacing.sm),
                                color = MaterialTheme.colorScheme.surfaceVariant,
                            )
                            ProfileInfoRow(
                                icon = Icons.Outlined.Phone,
                                iconTint = AppThemeColors.current.kpiGreen,
                                iconBg = AppThemeColors.current.kpiGreenLight,
                                label = "Phone",
                                value = profile.phone,
                            )
                            HorizontalDivider(
                                modifier = Modifier.padding(vertical = Spacing.sm),
                                color = MaterialTheme.colorScheme.surfaceVariant,
                            )
                            ProfileInfoRow(
                                icon = Icons.Outlined.Business,
                                iconTint = AppThemeColors.current.kpiPurple,
                                iconBg = AppThemeColors.current.kpiPurpleLight,
                                label = "Facility",
                                value = profile.facility,
                            )
                        }
                    }
                }

                // ── Employment Details ────────────────────────
                item {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = Spacing.lg),
                        shape = RoundedCornerShape(Spacing.base),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surface,
                        ),
                        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                    ) {
                        Column(
                            modifier = Modifier.padding(Spacing.base),
                        ) {
                            Text(
                                text = "Employment Details",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurface,
                            )
                            Spacer(modifier = Modifier.height(Spacing.md))

                            if (profile.employeeId != null) {
                                ProfileInfoRow(
                                    icon = Icons.Filled.Badge,
                                    iconTint = AppThemeColors.current.kpiOrange,
                                    iconBg = AppThemeColors.current.kpiOrangeLight,
                                    label = "Employee ID",
                                    value = profile.employeeId,
                                )
                                HorizontalDivider(
                                    modifier = Modifier.padding(vertical = Spacing.sm),
                                    color = MaterialTheme.colorScheme.surfaceVariant,
                                )
                            }

                            if (profile.joinDate != null) {
                                ProfileInfoRow(
                                    icon = Icons.Filled.DateRange,
                                    iconTint = AppThemeColors.current.kpiBlue,
                                    iconBg = AppThemeColors.current.kpiBlueLight,
                                    label = "Join Date",
                                    value = profile.joinDate,
                                )
                                HorizontalDivider(
                                    modifier = Modifier.padding(vertical = Spacing.sm),
                                    color = MaterialTheme.colorScheme.surfaceVariant,
                                )
                            }

                            if (profile.licenseNumber != null) {
                                ProfileInfoRow(
                                    icon = Icons.Filled.Security,
                                    iconTint = AppThemeColors.current.kpiPurple,
                                    iconBg = AppThemeColors.current.kpiPurpleLight,
                                    label = "License Number",
                                    value = profile.licenseNumber,
                                )
                            }
                        }
                    }
                }

                // ── Settings ──────────────────────────────────
                item {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = Spacing.lg),
                        shape = RoundedCornerShape(Spacing.base),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surface,
                        ),
                        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                    ) {
                        Column(
                            modifier = Modifier.padding(Spacing.base),
                        ) {
                            Text(
                                text = "Settings",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurface,
                            )
                            Spacer(modifier = Modifier.height(Spacing.md))

                            // Notifications
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { notificationsEnabled = !notificationsEnabled },
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(36.dp)
                                        .clip(CircleShape)
                                        .background(AppThemeColors.current.kpiBlueLight),
                                    contentAlignment = Alignment.Center,
                                ) {
                                    Icon(
                                        imageVector = Icons.Filled.Notifications,
                                        contentDescription = null,
                                        tint = AppThemeColors.current.kpiBlue,
                                        modifier = Modifier.size(20.dp),
                                    )
                                }
                                Spacer(modifier = Modifier.width(Spacing.md))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "Notifications",
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.Medium,
                                    )
                                    Text(
                                        text = "Push & email notifications",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    )
                                }
                                Switch(
                                    checked = notificationsEnabled,
                                    onCheckedChange = { notificationsEnabled = it },
                                    colors = SwitchDefaults.colors(
                                        checkedTrackColor = AppThemeColors.current.kpiBlue,
                                    ),
                                )
                            }

                            HorizontalDivider(
                                modifier = Modifier.padding(vertical = Spacing.sm),
                                color = MaterialTheme.colorScheme.surfaceVariant,
                            )

                            // Theme / Appearance
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { showThemeSettings = true },
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(36.dp)
                                        .clip(CircleShape)
                                        .background(AppThemeColors.current.kpiPurpleLight),
                                    contentAlignment = Alignment.Center,
                                ) {
                                    Icon(
                                        imageVector = Icons.Filled.DarkMode,
                                        contentDescription = null,
                                        tint = AppThemeColors.current.kpiPurple,
                                        modifier = Modifier.size(20.dp),
                                    )
                                }
                                Spacer(modifier = Modifier.width(Spacing.md))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "Appearance",
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.Medium,
                                    )
                                    Text(
                                        text = "${themeSettings.appearanceMode.displayName} · ${themeSettings.colorTheme.displayName}",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    )
                                }
                                Icon(
                                    imageVector = Icons.Filled.ChevronRight,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(24.dp),
                                )
                            }

                            HorizontalDivider(
                                modifier = Modifier.padding(vertical = Spacing.sm),
                                color = MaterialTheme.colorScheme.surfaceVariant,
                            )

                            // Help & Support
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { /* open help */ },
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(36.dp)
                                        .clip(CircleShape)
                                        .background(AppThemeColors.current.kpiGreenLight),
                                    contentAlignment = Alignment.Center,
                                ) {
                                    Icon(
                                        imageVector = Icons.AutoMirrored.Outlined.Help,
                                        contentDescription = null,
                                        tint = AppThemeColors.current.kpiGreen,
                                        modifier = Modifier.size(20.dp),
                                    )
                                }
                                Spacer(modifier = Modifier.width(Spacing.md))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "Help & Support",
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.Medium,
                                    )
                                    Text(
                                        text = "FAQ, contact support",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    )
                                }
                                Icon(
                                    imageVector = Icons.Filled.ChevronRight,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(24.dp),
                                )
                            }

                            HorizontalDivider(
                                modifier = Modifier.padding(vertical = Spacing.sm),
                                color = MaterialTheme.colorScheme.surfaceVariant,
                            )

                            // Server settings
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { onOpenServerSettings() },
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(36.dp)
                                        .clip(CircleShape)
                                        .background(AppThemeColors.current.kpiBlueLight),
                                    contentAlignment = Alignment.Center,
                                ) {
                                    Icon(
                                        imageVector = Icons.Filled.Settings,
                                        contentDescription = null,
                                        tint = AppThemeColors.current.kpiBlue,
                                        modifier = Modifier.size(20.dp),
                                    )
                                }
                                Spacer(modifier = Modifier.width(Spacing.md))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "Server settings",
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.Medium,
                                    )
                                    Text(
                                        text = "EMR & conversation engine URLs",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    )
                                }
                                Icon(
                                    imageVector = Icons.Filled.ChevronRight,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(24.dp),
                                )
                            }
                        }
                    }
                }

                // ── Sign Out Button ───────────────────────────
                item {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = Spacing.lg)
                            .clickable { showSignOutDialog = true },
                        shape = RoundedCornerShape(Spacing.base),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.errorContainer,
                        ),
                        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(Spacing.base),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center,
                        ) {
                            AnimatedVisibility(
                                visible = !state.isSigningOut,
                                enter = fadeIn(),
                                exit = fadeOut(),
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.AutoMirrored.Filled.Logout,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.error,
                                        modifier = Modifier.size(20.dp),
                                    )
                                    Spacer(modifier = Modifier.width(Spacing.sm))
                                    Text(
                                        text = "Sign Out",
                                        style = MaterialTheme.typography.bodyLarge,
                                        color = MaterialTheme.colorScheme.error,
                                        fontWeight = FontWeight.SemiBold,
                                    )
                                }
                            }
                            AnimatedVisibility(
                                visible = state.isSigningOut,
                                enter = fadeIn(),
                                exit = fadeOut(),
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(20.dp),
                                        color = MaterialTheme.colorScheme.error,
                                        strokeWidth = 2.dp,
                                    )
                                    Spacer(modifier = Modifier.width(Spacing.sm))
                                    Text(
                                        text = "Signing out...",
                                        style = MaterialTheme.typography.bodyLarge,
                                        color = MaterialTheme.colorScheme.error,
                                        fontWeight = FontWeight.SemiBold,
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(Spacing.xs))

                    // App version — 10-tap easter egg reveals the server-config panel
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = Spacing.lg)
                            .clickable {
                                if (showServerConfig) {
                                    showServerConfig = false
                                    tapCounter.reset()
                                    tapProgress = 0
                                } else {
                                    tapCounter.onTap()
                                    tapProgress = tapCounter.count
                                    showServerConfig = tapCounter.revealed
                                }
                            },
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        Text(
                            text = if (showServerConfig) {
                                "Hide server config"
                            } else if (tapProgress >= 3) {
                                "${10 - tapProgress} more tap${if (10 - tapProgress == 1) "" else "s"} to unlock server config…"
                            } else {
                                "PrognoCare v1.0.0"
                            },
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                        )
                    }

                    AnimatedVisibility(visible = showServerConfig, enter = fadeIn(), exit = fadeOut()) {
                        Column(modifier = Modifier.padding(horizontal = Spacing.lg, vertical = Spacing.base)) {
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(Spacing.md),
                                colors = CardDefaults.cardColors(
                                    containerColor = MaterialTheme.colorScheme.surfaceVariant,
                                ),
                                elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
                            ) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(Spacing.md),
                                    verticalArrangement = Arrangement.spacedBy(Spacing.xs),
                                ) {
                                    Text(
                                        text = "Current server URLs",
                                        style = MaterialTheme.typography.labelMedium,
                                        fontWeight = FontWeight.SemiBold,
                                        color = MaterialTheme.colorScheme.onSurface,
                                    )
                                    Text(
                                        text = "EMR: ${appConfig.emrBaseUrl}",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    )
                                    Text(
                                        text = "Chat: ${appConfig.conversationBaseUrl}",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    )
                                    Text(
                                        text = "Web channel: ${appConfig.webChannelId}",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.height(Spacing.md))
                            ServerConfigContent(
                                config = appConfig,
                                onSave = { emr, conv, channel ->
                                    viewModel.saveServerConfig(emr, conv, channel)
                                },
                                onReset = { viewModel.resetServerConfig() },
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(Spacing.xxl))
                }
            }
        }
    }
}

@Composable
private fun ProfileInfoRow(
    icon: ImageVector,
    iconTint: Color,
    iconBg: Color,
    label: String,
    value: String,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(36.dp)
                .clip(CircleShape)
                .background(iconBg),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = iconTint,
                modifier = Modifier.size(20.dp),
            )
        }
        Spacer(modifier = Modifier.width(Spacing.md))
        Column {
            Text(
                text = label,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                text = value,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface,
                fontWeight = FontWeight.Medium,
            )
        }
    }
}
