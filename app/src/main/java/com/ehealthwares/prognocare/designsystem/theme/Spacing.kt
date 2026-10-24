package com.ehealthwares.prognocare.designsystem.theme

import androidx.compose.ui.unit.dp

/**
 * PrognoCare Spacing System
 * Based on 4dp grid. All spacing should reference these tokens.
 */
object Spacing {
    val xxs = 2.dp
    val xs = 4.dp
    val sm = 8.dp
    val md = 12.dp
    val base = 16.dp
    val lg = 20.dp
    val xl = 24.dp
    val xxl = 32.dp
    val xxxl = 40.dp
    val xxxxl = 48.dp
    val xxxxxl = 64.dp
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
 * Touch target minimum: 48dp (Material Design)
 */
object TouchTarget {
    val minSize = 48.dp
    val minGap = 8.dp
}
