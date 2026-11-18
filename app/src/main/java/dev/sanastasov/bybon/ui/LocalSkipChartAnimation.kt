package dev.sanastasov.bybon.ui

import androidx.compose.runtime.staticCompositionLocalOf

/**
 * When true, compose-charts intro tweens are skipped so the first frame is the finished series.
 * Production leaves this false; Paparazzi provides true from `bybonSnapshot`.
 */
val LocalSkipChartAnimation = staticCompositionLocalOf { false }
