package dev.sanastasov.bybon.workout.ui.history

import dev.sanastasov.bybon.workout.domain.DaysTrained
import dev.sanastasov.bybon.workout.domain.TrainingDay
import dev.sanastasov.bybon.workout.domain.TrainingWeek
import dev.sanastasov.bybon.workout.domain.TrainingWeekLabel
import java.time.LocalDate

data class DaysTrainedUi(
    val weeks: List<TrainingWeekUi>,
)

data class TrainingWeekUi(
    val label: String,
    val days: List<TrainingDayUi>,
)

data class TrainingDayUi(
    val date: LocalDate,
    val trained: Boolean,
    val isToday: Boolean,
    val isFuture: Boolean,
)

internal fun DaysTrained.toUi(today: LocalDate): DaysTrainedUi =
    DaysTrainedUi(weeks = weeks.map { it.toUi(today) })

private fun TrainingWeek.toUi(today: LocalDate): TrainingWeekUi = TrainingWeekUi(
    label = label.toUi(),
    days = days.map { it.toUi(today) },
)

private fun TrainingDay.toUi(today: LocalDate): TrainingDayUi = TrainingDayUi(
    date = date,
    trained = trained,
    isToday = date == today,
    isFuture = date > today,
)

private fun TrainingWeekLabel.toUi(): String = when (this) {
    TrainingWeekLabel.LastWeek -> "Last week"
    TrainingWeekLabel.ThisWeek -> "This week"
    TrainingWeekLabel.NextWeek -> "Next week"
}
