package dev.sanastasov.bybon.domain

import java.time.DayOfWeek
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import java.time.temporal.WeekFields
import java.util.Locale

val LocalDate.weekOfYear: Int
    get() = get(WeekFields.ISO.weekOfYear())

fun LocalDate.isoWeekStart(): LocalDate = with(DayOfWeek.MONDAY)

private val DISPLAY_DATE_FORMAT: DateTimeFormatter =
    DateTimeFormatter.ofPattern("d MMM yyyy", Locale.US)

private val SHORT_DISPLAY_DATE_FORMAT: DateTimeFormatter =
    DateTimeFormatter.ofPattern("d MMM", Locale.US)

fun LocalDate.toDisplayDate(): String = format(DISPLAY_DATE_FORMAT)

fun LocalDate.toShortDisplayDate(): String = format(SHORT_DISPLAY_DATE_FORMAT)

fun LocalDate.toDisplayDateWithRelative(today: LocalDate): String =
    "${toDisplayDate()} (${toRelativeAgo(today)})"

fun LocalDate.toDisplayDateDotRelative(today: LocalDate): String =
    "${toDisplayDate()} · ${toRelativeAgo(today)}"

private fun LocalDate.toRelativeAgo(today: LocalDate): String {
    val days = ChronoUnit.DAYS.between(this, today).coerceAtLeast(0)
    if (days <= 30) return days.toAgoLabel("day")
    return ChronoUnit.MONTHS.between(this, today).coerceAtLeast(1).toAgoLabel("month")
}

private fun Long.toAgoLabel(unit: String): String = when (this) {
    0L -> "today"
    1L -> "1 $unit ago"
    else -> "$this ${unit}s ago"
}
