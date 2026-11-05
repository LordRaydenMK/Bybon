package dev.sanastasov.bybon.workout.ui.history

import dev.sanastasov.bybon.workout.domain.Weight
import dev.sanastasov.bybon.workout.domain.WorkoutPlanId
import dev.sanastasov.bybon.workout.domain.WorkoutSessionId
import java.time.LocalDate
import org.junit.Test

class WorkoutHistoryUiTest {

    private val today = LocalDate.of(2026, 11, 5)

    @Test
    fun `top set content description names load and 1RM`() {
        val topSet = ExerciseTopSetUi(
            name = "Romanian Deadlift",
            weightKg = "45",
            reps = 12,
            oneRm = Weight.kilograms(63f),
        )
        assert(topSet.contentDescription() == "Romanian Deadlift, 45 kg x 12, 63 kg 1RM")
    }

    @Test
    fun `missing load content description uses dashes`() {
        val topSet = ExerciseTopSetUi(
            name = "Assisted Pull-up",
            weightKg = "-",
            reps = 8,
            oneRm = null,
        )
        assert(topSet.contentDescription() == "Assisted Pull-up, - x 8, -")
    }

    @Test
    fun `history sections use this week this month and named months`() {
        val thisWeek = historySession(LocalDate.of(2026, 11, 4), "This Week Session")
        val thisMonth = historySession(LocalDate.of(2026, 11, 1), "This Month Session")
        val october = historySession(LocalDate.of(2026, 10, 20), "October Session")
        val august = historySession(LocalDate.of(2026, 8, 10), "August Session")

        val sections = listOf(thisWeek, thisMonth, october, august)
            .toHistoryPeriodSections(today)

        assert(sections.map { it.title } == listOf("This Week", "This Month", "October", "August"))
        assert(sections[0].sessions.single().planName == "This Week Session")
        assert(sections[1].sessions.single().planName == "This Month Session")
        assert(sections[2].sessions.single().planName == "October Session")
        assert(sections[3].sessions.single().planName == "August Session")
    }

    @Test
    fun `this week includes dates from the previous month in the current iso week`() {
        val today = LocalDate.of(2026, 4, 2)
        val lastMonthInThisWeek = historySession(LocalDate.of(2026, 3, 30), "Week Session")
        val earlierMarch = historySession(LocalDate.of(2026, 3, 20), "March Session")

        val sections = listOf(lastMonthInThisWeek, earlierMarch)
            .toHistoryPeriodSections(today)

        assert(sections.map { it.title } == listOf("This Week", "March"))
        assert(sections[0].sessions.single().planName == "Week Session")
        assert(sections[1].sessions.single().planName == "March Session")
    }

    @Test
    fun `period headers are omitted when the period has no exercises`() {
        val emptyThisWeek = historySession(
            LocalDate.of(2026, 11, 4),
            "Empty Week",
            exercises = emptyList(),
        )
        val thisMonth = historySession(LocalDate.of(2026, 11, 1), "This Month Session")
        val emptyOctober = historySession(
            LocalDate.of(2026, 10, 20),
            "Empty October",
            exercises = emptyList(),
        )

        val sections = listOf(emptyThisWeek, thisMonth, emptyOctober)
            .toHistoryPeriodSections(today)

        assert(sections.map { it.title } == listOf("This Month"))
        assert(sections.single().sessions.single().planName == "This Month Session")
    }

    @Test
    fun `named month from a previous year includes the year`() {
        val lastYear = historySession(LocalDate.of(2025, 8, 10), "Last August")

        val sections = listOf(lastYear).toHistoryPeriodSections(today)

        assert(sections.single().title == "August 2025")
    }

    private fun historySession(
        date: LocalDate,
        planName: String,
        exercises: List<ExerciseTopSetUi> = listOf(
            ExerciseTopSetUi("Squat", "100", 5, null),
        ),
    ) = WorkoutSessionHistoryUi(
        id = WorkoutSessionId(WorkoutPlanId(planName), date.atStartOfDay()),
        planName = planName,
        date = date,
        startTime = "18:00",
        duration = "00:40",
        exercises = exercises,
    )
}
