package com.ehealthwares.prognocare.designsystem.theme

import androidx.compose.ui.unit.dp

/**
 * PrognoCare Spacing System
 * Compact 2/4dp scale (≈20–25% tighter than the original) so clinical screens
 * keep more content above the fold.
 */
object Spacing {
    val xxs = 2.dp
    val xs = 4.dp
    val sm = 6.dp
    val md = 8.dp
    val base = 12.dp
    val lg = 16.dp
    val xl = 20.dp
    val xxl = 24.dp
    val xxxl = 32.dp
    val xxxxl = 40.dp
    val xxxxxl = 56.dp
}

/**
 * Screen-level padding
 */
object ScreenPadding {
    val horizontal = Spacing.lg
    val vertical = Spacing.base
    val top = Spacing.xl
    val bottom = Spacing.xxl
}

/**
 * Card spacing
 */
object CardSpacing {
    val internal = Spacing.base
    val between = Spacing.md
    val grid = Spacing.sm
}

/**
 * Touch target minimum. Material's 48dp default is relaxed to 40dp to keep
 * controls compact; interactive icons/flags still keep a comfortable target.
 */
object TouchTarget {
    val minSize = 40.dp
    val minGap = 6.dp
}
