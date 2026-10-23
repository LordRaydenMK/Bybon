package dev.sanastasov.bybon.workout.ui

import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale

fun LocalDateTime.toStartTimeLabel(): String = START_TIME_FORMAT.format(this)

private val START_TIME_FORMAT: DateTimeFormatter =
    DateTimeFormatter.ofPattern("HH:mm", Locale.US)
