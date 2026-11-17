package dev.sanastasov.bybon.workout.domain

import dev.sanastasov.bybon.workout.data.completedExercise
import dev.sanastasov.bybon.workout.data.completedSession
import java.time.LocalDate
import org.junit.Test

class DaysTrainedTest {

    @Test
    fun `before Friday shows last week and this week`() {
        val thursday = LocalDate.of(2026, 11, 5)

        val days = daysTrained(thursday, trainedDates = emptySet())

        assert(
            days.weeks.map { it.label } ==
                listOf(TrainingWeekLabel.LastWeek, TrainingWeekLabel.ThisWeek),
        )
        assert(days.weeks[0].days.first().date == LocalDate.of(2026, 10, 26))
        assert(days.weeks[0].days.last().date == LocalDate.of(2026, 11, 1))
        assert(days.weeks[1].days.first().date == LocalDate.of(2026, 11, 2))
        assert(days.weeks[1].days.last().date == LocalDate.of(2026, 11, 8))
    }

    @Test
    fun `Friday or later shows this week and next week`() {
        val friday = LocalDate.of(2026, 11, 6)
        val sunday = LocalDate.of(2026, 11, 8)

        val fridayDays = daysTrained(friday, trainedDates = emptySet())
        val sundayDays = daysTrained(sunday, trainedDates = emptySet())

        assert(
            fridayDays.weeks.map { it.label } ==
                listOf(TrainingWeekLabel.ThisWeek, TrainingWeekLabel.NextWeek),
        )
        assert(fridayDays.weeks[0].days.first().date == LocalDate.of(2026, 11, 2))
        assert(fridayDays.weeks[1].days.last().date == LocalDate.of(2026, 11, 15))
        assert(
            sundayDays.weeks.map { week -> week.days.map { it.date } } ==
                fridayDays.weeks.map { week -> week.days.map { it.date } },
        )
    }

    @Test
    fun `fills days that have a completed workout`() {
        val thursday = LocalDate.of(2026, 11, 5)
        val trained = setOf(
            LocalDate.of(2026, 10, 26),
            LocalDate.of(2026, 10, 28),
            LocalDate.of(2026, 11, 2),
            LocalDate.of(2026, 11, 5),
            LocalDate.of(2026, 10, 25),
            LocalDate.of(2026, 11, 9),
        )

        val days = daysTrained(thursday, trained)

        assert(
            days.trainedDates() == listOf(
                LocalDate.of(2026, 10, 26),
                LocalDate.of(2026, 10, 28),
                LocalDate.of(2026, 11, 2),
                LocalDate.of(2026, 11, 5),
            ),
        )
    }

    @Test
    fun `summary ignores drafts and still fills completed days outside the 7 day stats window`() {
        val thursday = LocalDate.of(2026, 11, 5)
        val lastWeek = sessionOn(LocalDate.of(2026, 10, 28))
        val todaySession = sessionOn(thursday)
        val draft = fullBodyA.toOverviewSession()
        val inProgress = todaySession.copy(
            exercises = todaySession.exercises.map { exercise ->
                exercise.copy(sets = exercise.sets.map { it.copy(setState = SetState.InProgress) })
            },
        )

        val summary = listOf(
            lastWeek,
            todaySession,
            draft,
            inProgress,
        ).lastSevenDaysSummary(thursday)

        assert(summary.workoutCount == 1)
        assert(
            summary.daysTrained.trainedDates() == listOf(
                LocalDate.of(2026, 10, 28),
                thursday,
            ),
        )
    }

    @Test
    fun `multiple sessions on the same day fill once`() {
        val friday = LocalDate.of(2026, 11, 6)
        val morning = sessionOn(friday).copy(startedAt = friday.atTime(8, 0))
        val evening = sessionOn(friday).copy(startedAt = friday.atTime(18, 0))

        val summary = listOf(morning, evening).lastSevenDaysSummary(friday)

        assert(summary.workoutCount == 2)
        assert(
            summary.daysTrained.weeks.map { it.label } ==
                listOf(TrainingWeekLabel.ThisWeek, TrainingWeekLabel.NextWeek),
        )
        assert(summary.daysTrained.trainedDates() == listOf(friday))
    }

    private fun DaysTrained.trainedDates(): List<LocalDate> =
        weeks.flatMap { week -> week.days }.filter { it.trained }.map { it.date }

    private fun sessionOn(date: LocalDate) = completedSession(
        planId = "plan",
        planName = "Plan",
        startedAt = date.atTime(18, 0),
        exercises = listOf(completedExercise("bench-press", 60f to 8)),
    )
}
