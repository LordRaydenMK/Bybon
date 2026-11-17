package dev.sanastasov.bybon.workout.domain

import dev.sanastasov.bybon.domain.isoWeekStart
import java.time.DayOfWeek
import java.time.LocalDate

data class DaysTrained(
    val weeks: List<TrainingWeek>,
)

data class TrainingWeek(
    val label: TrainingWeekLabel,
    val days: List<TrainingDay>,
)

enum class TrainingWeekLabel {
    LastWeek,
    ThisWeek,
    NextWeek,
}

data class TrainingDay(
    val date: LocalDate,
    val trained: Boolean,
)

fun daysTrained(today: LocalDate, trainedDates: Set<LocalDate>): DaysTrained {
    val thisWeekStart = today.isoWeekStart()
    val showNextWeek = today.dayOfWeek >= DayOfWeek.FRIDAY
    val firstWeekStart = if (showNextWeek) thisWeekStart else thisWeekStart.minusWeeks(1)
    val labels = if (showNextWeek) {
        listOf(TrainingWeekLabel.ThisWeek, TrainingWeekLabel.NextWeek)
    } else {
        listOf(TrainingWeekLabel.LastWeek, TrainingWeekLabel.ThisWeek)
    }
    return DaysTrained(
        weeks = labels.mapIndexed { index, label ->
            trainingWeek(label, firstWeekStart.plusWeeks(index.toLong()), trainedDates)
        },
    )
}

private fun trainingWeek(
    label: TrainingWeekLabel,
    start: LocalDate,
    trainedDates: Set<LocalDate>,
): TrainingWeek = TrainingWeek(
    label = label,
    days = List(DAYS_IN_WEEK) { offset ->
        val date = start.plusDays(offset.toLong())
        TrainingDay(date = date, trained = date in trainedDates)
    },
)

private const val DAYS_IN_WEEK = 7
