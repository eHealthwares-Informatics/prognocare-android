package com.ehealthwares.prognocare.designsystem.theme

import androidx.compose.ui.graphics.Color

/**
 * Appearance modes for the application
 */
enum class AppearanceMode(val displayName: String) {
    SYSTEM("System Default"),
    LIGHT("Light"),
    DARK("Dark")
}

/**
 * Predefined color themes
 */
enum class ColorTheme(val displayName: String, val description: String) {
    EHEALTHWARES("eHealthWares", "Default healthcare branding"),
    EMERALD("Emerald", "Fresh green healthcare"),
    TEAL("Teal", "Calming teal"),
    INDIGO("Indigo", "Professional indigo"),
    VIOLET("Violet", "Modern violet"),
    CORAL("Coral", "Warm coral"),
    OCEAN("Ocean", "Deep ocean blue"),
    CUSTOM("Custom", "Your personalized theme")
}

/**
 * Color theme palette - light mode
 */
data class ThemeColors(
    val primary: Color,
    val onPrimary: Color,
    val primaryContainer: Color,
    val onPrimaryContainer: Color,
    val secondary: Color,
    val onSecondary: Color,
    val secondaryContainer: Color,
    val onSecondaryContainer: Color,
    val tertiary: Color,
    val onTertiary: Color,
    val tertiaryContainer: Color,
    val onTertiaryContainer: Color,
    val error: Color,
    val onError: Color,
    val errorContainer: Color,
    val onErrorContainer: Color,
    val background: Color,
    val onBackground: Color,
    val surface: Color,
    val onSurface: Color,
    val surfaceVariant: Color,
    val onSurfaceVariant: Color,
    val outline: Color,
    val outlineVariant: Color,
    // Semantic healthcare colors
    val success: Color,
    val successContainer: Color,
    val warning: Color,
    val warningContainer: Color,
    val info: Color,
    val infoContainer: Color,
    val pending: Color,
    val pendingContainer: Color,
    val scheduled: Color,
    val scheduledContainer: Color,
    val inProgress: Color,
    val inProgressContainer: Color,
    val completed: Color,
    val completedContainer: Color,
    val cancelled: Color,
    val cancelledContainer: Color,
    val critical: Color,
    val criticalContainer: Color,
    // Semantic on-container colors
    val onSuccessContainer: Color,
    val onWarningContainer: Color,
    val onInfoContainer: Color,
    val onCriticalContainer: Color,
    val onPendingContainer: Color,
    val onScheduledContainer: Color,
    val onInProgressContainer: Color,
    val onCancelledContainer: Color,
    val onCompletedContainer: Color,
    // Divider
    val divider: Color,
    // KPI accent colors
    val kpiBlue: Color,
    val kpiBlueLight: Color,
    val kpiGreen: Color,
    val kpiGreenLight: Color,
    val kpiPurple: Color,
    val kpiPurpleLight: Color,
    val kpiOrange: Color,
    val kpiOrangeLight: Color,
    // Notification
    val notificationBadge: Color,
    val onNotificationBadge: Color
)

/**
 * Get light theme colors for a color theme
 */
fun getLightColors(theme: ColorTheme, customPrimary: Color? = null): ThemeColors {
    val primary = customPrimary ?: when (theme) {
        ColorTheme.EHEALTHWARES -> Color(0xFF2563EB) // Blue
        ColorTheme.EMERALD -> Color(0xFF059669) // Emerald green
        ColorTheme.TEAL -> Color(0xFF0D9488) // Teal
        ColorTheme.INDIGO -> Color(0xFF4F46E5) // Indigo
        ColorTheme.VIOLET -> Color(0xFF7C3AED) // Violet
        ColorTheme.CORAL -> Color(0xFFE11D48) // Coral
        ColorTheme.OCEAN -> Color(0xFF0284C7) // Ocean blue
        ColorTheme.CUSTOM -> Color(0xFF2563EB) // Default blue
    }
    
    return ThemeColors(
        // MaterialTheme.colorScheme.primary
        primary = primary,
        onPrimary = Color.White,
        primaryContainer = primary.copy(alpha = 0.12f),
        onPrimaryContainer = primary,
        
        // Secondary
        secondary = Color(0xFF475569),
        onSecondary = Color.White,
        secondaryContainer = Color(0xFFF1F5F9),
        onSecondaryContainer = Color(0xFF334155),
        
        // Tertiary
        tertiary = Color(0xFF16A34A),
        onTertiary = Color.White,
        tertiaryContainer = Color(0xFFDCFCE7),
        onTertiaryContainer = Color(0xFF14532D),
        
        // Error
        error = Color(0xFFDC2626),
        onError = Color.White,
        errorContainer = Color(0xFFFEE2E2),
        onErrorContainer = Color(0xFF7F1D1D),
        
        // Background & Surface
        background = Color(0xFFF8FAFC),
        onBackground = Color(0xFF0F172A),
        surface = Color(0xFFFFFFFF),
        onSurface = Color(0xFF0F172A),
        surfaceVariant = Color(0xFFF1F5F9),
        onSurfaceVariant = Color(0xFF475569),
        
        // Outline
        outline = Color(0xFFCBD5E1),
        outlineVariant = Color(0xFFE2E8F0),
        
        // Semantic Healthcare
        success = Color(0xFF16A34A),
        successContainer = Color(0xFFDCFCE7),
        warning = Color(0xFFF59E0B),
        warningContainer = Color(0xFFFEF3C7),
        info = Color(0xFF3B82F6),
        infoContainer = Color(0xFFDBEAFE),
        pending = Color(0xFFF59E0B),
        pendingContainer = Color(0xFFFEF3C7),
        scheduled = Color(0xFF2563EB),
        scheduledContainer = Color(0xFFDBEAFE),
        inProgress = Color(0xFF16A34A),
        inProgressContainer = Color(0xFFDCFCE7),
        completed = Color(0xFF6B7280),
        completedContainer = Color(0xFFF1F5F9),
        cancelled = Color(0xFF9CA3AF),
        cancelledContainer = Color(0xFFF1F5F9),
        critical = Color(0xFFDC2626),
        criticalContainer = Color(0xFFFEE2E2),
        
        // Semantic on-container colors
        onSuccessContainer = Color(0xFF14532D),
        onWarningContainer = Color(0xFF78350F),
        onInfoContainer = Color(0xFF1E3A5F),
        onCriticalContainer = Color(0xFF7F1D1D),
        onPendingContainer = Color(0xFF78350F),
        onScheduledContainer = Color(0xFF1E3A5F),
        onInProgressContainer = Color(0xFF14532D),
        onCancelledContainer = Color(0xFF475569),
        onCompletedContainer = Color(0xFF334155),
        
        // Divider
        divider = Color(0xFFE2E8F0),
        
        // KPI accents
        kpiBlue = primary,
        kpiBlueLight = primary.copy(alpha = 0.12f),
        kpiGreen = Color(0xFF16A34A),
        kpiGreenLight = Color(0xFFDCFCE7),
        kpiPurple = Color(0xFF7C3AED),
        kpiPurpleLight = Color(0xFFEDE9FE),
        kpiOrange = Color(0xFFF97316),
        kpiOrangeLight = Color(0xFFFFF7ED),
        
        // Notification
        notificationBadge = Color(0xFFDC2626),
        onNotificationBadge = Color.White
    )
}

/**
 * Get dark theme colors for a color theme
 */
fun getDarkColors(theme: ColorTheme, customPrimary: Color? = null): ThemeColors {
    val primary = customPrimary ?: when (theme) {
        ColorTheme.EHEALTHWARES -> Color(0xFF60A5FA) // Light blue
        ColorTheme.EMERALD -> Color(0xFF34D399) // Light emerald
        ColorTheme.TEAL -> Color(0xFF2DD4BF) // Light teal
        ColorTheme.INDIGO -> Color(0xFF818CF8) // Light indigo
        ColorTheme.VIOLET -> Color(0xFFA78BFA) // Light violet
        ColorTheme.CORAL -> Color(0xFFFB7185) // Light coral
        ColorTheme.OCEAN -> Color(0xFF38BDF8) // Light ocean
        ColorTheme.CUSTOM -> Color(0xFF60A5FA) // Default light blue
    }
    
    return ThemeColors(
        // Primary - brighter for dark backgrounds
        primary = primary,
        onPrimary = Color(0xFF0A1628),
        primaryContainer = primary.copy(alpha = 0.18f),
        onPrimaryContainer = primary.copy(alpha = 0.9f),
        
        // Secondary
        secondary = Color(0xFF8899AD),
        onSecondary = Color(0xFF0A1628),
        secondaryContainer = Color(0xFF1C2A3D),
        onSecondaryContainer = Color(0xFFCBD5E1),
        
        // Tertiary - healthcare green
        tertiary = Color(0xFF34D399),
        onTertiary = Color(0xFF0A1628),
        tertiaryContainer = Color(0xFF0D3326),
        onTertiaryContainer = Color(0xFFA7F3D0),
        
        // Error
        error = Color(0xFFFF8A80),
        onError = Color(0xFF3B0A0A),
        errorContainer = Color(0xFF5C1515),
        onErrorContainer = Color(0xFFFFCDD2),
        
        // ── Layered Surface Hierarchy ──────────────────────
        // Level 0: App background (deepest)
        background = Color(0xFF0B1120),
        onBackground = Color(0xFFE0E7EF),
        // Level 1: Cards, sheets on background
        surface = Color(0xFF141D2E),
        onSurface = Color(0xFFE0E7EF),
        // Level 2: Elevated elements, chips, badges
        surfaceVariant = Color(0xFF1C2940),
        onSurfaceVariant = Color(0xFF8899AD),
        
        // Outline - subtle borders
        outline = Color(0xFF2D3E54),
        outlineVariant = Color(0xFF1C2940),
        
        // ── Semantic Healthcare Colors ─────────────────────
        success = Color(0xFF34D399),
        successContainer = Color(0xFF0D3326),
        warning = Color(0xFFFBBF24),
        warningContainer = Color(0xFF5C3D0A),
        info = primary,
        infoContainer = primary.copy(alpha = 0.15f),
        pending = Color(0xFFFBBF24),
        pendingContainer = Color(0xFF5C3D0A),
        scheduled = primary,
        scheduledContainer = primary.copy(alpha = 0.15f),
        inProgress = Color(0xFF34D399),
        inProgressContainer = Color(0xFF0D3326),
        completed = Color(0xFF8899AD),
        completedContainer = Color(0xFF1C2940),
        cancelled = Color(0xFF5E6E82),
        cancelledContainer = Color(0xFF1C2940),
        critical = Color(0xFFFF8A80),
        criticalContainer = Color(0xFF5C1515),
        
        // ── On-container text (must be readable) ───────────
        onSuccessContainer = Color(0xFFA7F3D0),
        onWarningContainer = Color(0xFFFDE68A),
        onInfoContainer = Color(0xFFBFDBFE),
        onCriticalContainer = Color(0xFFFFCDD2),
        onPendingContainer = Color(0xFFFDE68A),
        onScheduledContainer = Color(0xFFBFDBFE),
        onInProgressContainer = Color(0xFFA7F3D0),
        onCancelledContainer = Color(0xFFCBD5E1),
        onCompletedContainer = Color(0xFFCBD5E1),
        
        // Divider
        divider = Color(0xFF1C2940),
        
        // ── KPI Accents (lighter for dark backgrounds) ─────
        kpiBlue = primary,
        kpiBlueLight = primary.copy(alpha = 0.15f),
        kpiGreen = Color(0xFF34D399),
        kpiGreenLight = Color(0xFF0D3326),
        kpiPurple = Color(0xFFA78BFA),
        kpiPurpleLight = Color(0xFF2E1065).copy(alpha = 0.6f),
        kpiOrange = Color(0xFFFB923C),
        kpiOrangeLight = Color(0xFF7C2D12).copy(alpha = 0.6f),
        
        // Notification
        notificationBadge = Color(0xFFFF8A80),
        onNotificationBadge = Color(0xFF3B0A0A)
    )
}

/**
 * Theme settings data class
 */
data class ThemeSettings(
    val appearanceMode: AppearanceMode = AppearanceMode.SYSTEM,
    val colorTheme: ColorTheme = ColorTheme.EHEALTHWARES,
    val customPrimaryColor: Long = 0xFF2563EB,
    val customSecondaryColor: Long? = null,
    val customAccentColor: Long? = null
) {
    val isDark: Boolean
        get() = when (appearanceMode) {
            AppearanceMode.LIGHT -> false
            AppearanceMode.DARK -> true
            AppearanceMode.SYSTEM -> false // Will be overridden by system setting
        }
    
    fun getLightColors(): ThemeColors = getLightColors(
        theme = colorTheme,
        customPrimary = if (colorTheme == ColorTheme.CUSTOM) Color(customPrimaryColor) else null
    )
    
    fun getDarkColors(): ThemeColors = getDarkColors(
        theme = colorTheme,
        customPrimary = if (colorTheme == ColorTheme.CUSTOM) Color(customPrimaryColor) else null
    )
}
