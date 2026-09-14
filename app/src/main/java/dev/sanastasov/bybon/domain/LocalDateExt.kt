package dev.sanastasov.bybon.domain

import java.time.DayOfWeek
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.temporal.WeekFields
import java.util.Locale

val LocalDate.weekOfYear: Int
    get() = get(WeekFields.ISO.weekOfYear())

fun LocalDate.isoWeekStart(): LocalDate = with(DayOfWeek.MONDAY)

private val DISPLAY_DATE_FORMAT: DateTimeFormatter =
    DateTimeFormatter.ofPattern("d MMM yyyy", Locale.US)

fun LocalDate.toDisplayDate(): String = format(DISPLAY_DATE_FORMAT)
