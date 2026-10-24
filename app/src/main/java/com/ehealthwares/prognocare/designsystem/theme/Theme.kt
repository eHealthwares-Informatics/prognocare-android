package com.ehealthwares.prognocare.designsystem.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.graphics.Color as ComposeColor

/**
 * Local composition for theme settings
 */
val LocalThemeSettings = staticCompositionLocalOf { ThemeSettings() }

/**
 * Local composition for theme colors
 */
val LocalThemeColors = staticCompositionLocalOf { ThemeColors(
    primary = ComposeColor(0xFF2563EB),
    onPrimary = ComposeColor.White,
    primaryContainer = ComposeColor(0xFFDBEAFE),
    onPrimaryContainer = ComposeColor(0xFF1E3A5F),
    secondary = ComposeColor(0xFF475569),
    onSecondary = ComposeColor.White,
    secondaryContainer = ComposeColor(0xFFF1F5F9),
    onSecondaryContainer = ComposeColor(0xFF334155),
    tertiary = ComposeColor(0xFF16A34A),
    onTertiary = ComposeColor.White,
    tertiaryContainer = ComposeColor(0xFFDCFCE7),
    onTertiaryContainer = ComposeColor(0xFF14532D),
    error = ComposeColor(0xFFDC2626),
    onError = ComposeColor.White,
    errorContainer = ComposeColor(0xFFFEE2E2),
    onErrorContainer = ComposeColor(0xFF7F1D1D),
    background = ComposeColor(0xFFF8FAFC),
    onBackground = ComposeColor(0xFF0F172A),
    surface = ComposeColor.White,
    onSurface = ComposeColor(0xFF0F172A),
    surfaceVariant = ComposeColor(0xFFF1F5F9),
    onSurfaceVariant = ComposeColor(0xFF475569),
    outline = ComposeColor(0xFFCBD5E1),
    outlineVariant = ComposeColor(0xFFE2E8F0),
    success = ComposeColor(0xFF16A34A),
    successContainer = ComposeColor(0xFFDCFCE7),
    warning = ComposeColor(0xFFF59E0B),
    warningContainer = ComposeColor(0xFFFEF3C7),
    info = ComposeColor(0xFF3B82F6),
    infoContainer = ComposeColor(0xFFDBEAFE),
    pending = ComposeColor(0xFFF59E0B),
    pendingContainer = ComposeColor(0xFFFEF3C7),
    scheduled = ComposeColor(0xFF2563EB),
    scheduledContainer = ComposeColor(0xFFDBEAFE),
    inProgress = ComposeColor(0xFF16A34A),
    inProgressContainer = ComposeColor(0xFFDCFCE7),
    completed = ComposeColor(0xFF6B7280),
    completedContainer = ComposeColor(0xFFF1F5F9),
    cancelled = ComposeColor(0xFF9CA3AF),
    cancelledContainer = ComposeColor(0xFFF1F5F9),
    critical = ComposeColor(0xFFDC2626),
    criticalContainer = ComposeColor(0xFFFEE2E2),
    onSuccessContainer = ComposeColor(0xFF14532D),
    onWarningContainer = ComposeColor(0xFF78350F),
    onInfoContainer = ComposeColor(0xFF1E3A5F),
    onCriticalContainer = ComposeColor(0xFF7F1D1D),
    onPendingContainer = ComposeColor(0xFF78350F),
    onScheduledContainer = ComposeColor(0xFF1E3A5F),
    onInProgressContainer = ComposeColor(0xFF14532D),
    onCancelledContainer = ComposeColor(0xFF475569),
    onCompletedContainer = ComposeColor(0xFF334155),
    divider = ComposeColor(0xFFE2E8F0),
    kpiBlue = ComposeColor(0xFF2563EB),
    kpiBlueLight = ComposeColor(0xFFDBEAFE),
    kpiGreen = ComposeColor(0xFF16A34A),
    kpiGreenLight = ComposeColor(0xFFDCFCE7),
    kpiPurple = ComposeColor(0xFF7C3AED),
    kpiPurpleLight = ComposeColor(0xFFEDE9FE),
    kpiOrange = ComposeColor(0xFFF97316),
    kpiOrangeLight = ComposeColor(0xFFFFF7ED),
    notificationBadge = ComposeColor(0xFFDC2626),
    onNotificationBadge = ComposeColor.White
) }

@Immutable
data class PrognoCareSpacing(
    val xxs: Dp = Spacing.xxs,
    val xs: Dp = Spacing.xs,
    val sm: Dp = Spacing.sm,
    val md: Dp = Spacing.md,
    val base: Dp = Spacing.base,
    val lg: Dp = Spacing.lg,
    val xl: Dp = Spacing.xl,
    val xxl: Dp = Spacing.xxl,
    val xxxl: Dp = Spacing.xxxl,
    val xxxxl: Dp = Spacing.xxxxl,
)

val LocalPrognoCareSpacing = staticCompositionLocalOf { PrognoCareSpacing() }

@Composable
fun PrognoCareTheme(
    themeSettings: ThemeSettings = ThemeSettings(),
    isSystemDark: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val isDark = when (themeSettings.appearanceMode) {
        AppearanceMode.LIGHT -> false
        AppearanceMode.DARK -> true
        AppearanceMode.SYSTEM -> isSystemDark
    }
    
    val colors = if (isDark) {
        themeSettings.getDarkColors()
    } else {
        themeSettings.getLightColors()
    }
    
    val colorScheme = if (isDark) {
        darkColorScheme(
            primary = colors.primary,
            onPrimary = colors.onPrimary,
            primaryContainer = colors.primaryContainer,
            onPrimaryContainer = colors.onPrimaryContainer,
            secondary = colors.secondary,
            onSecondary = colors.onSecondary,
            secondaryContainer = colors.secondaryContainer,
            onSecondaryContainer = colors.onSecondaryContainer,
            tertiary = colors.tertiary,
            onTertiary = colors.onTertiary,
            tertiaryContainer = colors.tertiaryContainer,
            onTertiaryContainer = colors.onTertiaryContainer,
            error = colors.error,
            onError = colors.onError,
            errorContainer = colors.errorContainer,
            onErrorContainer = colors.onErrorContainer,
            background = colors.background,
            onBackground = colors.onBackground,
            surface = colors.surface,
            onSurface = colors.onSurface,
            surfaceVariant = colors.surfaceVariant,
            onSurfaceVariant = colors.onSurfaceVariant,
            outline = colors.outline,
            outlineVariant = colors.outlineVariant,
        )
    } else {
        lightColorScheme(
            primary = colors.primary,
            onPrimary = colors.onPrimary,
            primaryContainer = colors.primaryContainer,
            onPrimaryContainer = colors.onPrimaryContainer,
            secondary = colors.secondary,
            onSecondary = colors.onSecondary,
            secondaryContainer = colors.secondaryContainer,
            onSecondaryContainer = colors.onSecondaryContainer,
            tertiary = colors.tertiary,
            onTertiary = colors.onTertiary,
            tertiaryContainer = colors.tertiaryContainer,
            onTertiaryContainer = colors.onTertiaryContainer,
            error = colors.error,
            onError = colors.onError,
            errorContainer = colors.errorContainer,
            onErrorContainer = colors.onErrorContainer,
            background = colors.background,
            onBackground = colors.onBackground,
            surface = colors.surface,
            onSurface = colors.onSurface,
            surfaceVariant = colors.surfaceVariant,
            onSurfaceVariant = colors.onSurfaceVariant,
            outline = colors.outline,
            outlineVariant = colors.outlineVariant,
        )
    }
    
    CompositionLocalProvider(
        LocalThemeSettings provides themeSettings,
        LocalThemeColors provides colors,
        LocalPrognoCareSpacing provides PrognoCareSpacing()
    ) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = PrognoCareTypography,
            content = content,
        )
    }
}

/**
 * Extension to access theme colors from composable functions
 */
object AppThemeColors {
    val current: ThemeColors
        @Composable
        get() = LocalThemeColors.current
}
