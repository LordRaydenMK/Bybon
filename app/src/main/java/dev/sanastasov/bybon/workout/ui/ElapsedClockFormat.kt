package dev.sanastasov.bybon.workout.ui

import kotlin.time.Duration

fun Duration.formatElapsedClock(): String {
    val totalMinutes = inWholeMinutes.coerceAtLeast(0)
    val hours = totalMinutes / 60
    val minutes = totalMinutes % 60
    return "${hours.toString().padStart(2, '0')}:${minutes.toString().padStart(2, '0')}"
}
