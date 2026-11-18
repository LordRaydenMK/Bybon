package dev.sanastasov.bybon.workout.ui.history

import dev.sanastasov.bybon.workout.domain.OneRmChange
import dev.sanastasov.bybon.workout.domain.Weight
import dev.sanastasov.bybon.workout.domain.WorkoutPlanId
import dev.sanastasov.bybon.workout.domain.WorkoutSession
import dev.sanastasov.bybon.workout.domain.WorkoutSessionId
import dev.sanastasov.bybon.workout.domain.lastSevenDaysSummary
import dev.sanastasov.bybon.workout.ui.formatTrainingTime
import java.time.LocalDate
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds
import org.junit.Test

class WorkoutHistoryUiTest {

    private val today = LocalDate.of(2026, 11, 5)

    @Test
    fun `last 7 days range omits the start month when it matches the end`() {
        assert(
            formatLastSevenDaysRange(
                LocalDate.of(2026, 11, 5),
                LocalDate.of(2026, 11, 11),
            ) == "5–11 Nov 2026",
        )
    }

    @Test
    fun `last 7 days range keeps both months across a month boundary`() {
        assert(
            formatLastSevenDaysRange(
                LocalDate.of(2026, 10, 30),
                LocalDate.of(2026, 11, 5),
            ) == "30 Oct–5 Nov 2026",
        )
    }

    @Test
    fun `last 7 days range keeps both years across a year boundary`() {
        assert(
            formatLastSevenDaysRange(
                LocalDate.of(2025, 12, 30),
                LocalDate.of(2026, 1, 5),
            ) == "30 Dec 2025–5 Jan 2026",
        )
    }

    @Test
    fun `training time omits zero hours or minutes`() {
        assert(0.seconds.formatTrainingTime() == "0m")
        assert(18.minutes.formatTrainingTime() == "18m")
        assert(2.hours.formatTrainingTime() == "2h")
        assert((4.hours + 18.minutes).formatTrainingTime() == "4h 18m")
    }

    @Test
    fun `last 7 days with no workouts shows zero total weight`() {
        val summary = emptyList<WorkoutSession>().lastSevenDaysSummary(today).toUi()
        assert(summary.totalWeight == "0 kg")
    }

    @Test
    fun `last 7 days content description includes stats performance and days trained`() {
        val summary = LastSevenDaysSummaryUi(
            dateRange = "11–17 Nov 2026",
            workoutCount = 3,
            trainingTime = "4h 18m",
            workingSetCount = 42,
            totalWeight = "14820 kg",
            performance = LastSevenDaysPerformanceUi(8, 5, 2),
            daysTrained = DaysTrainedUi(
                weeks = listOf(
                    TrainingWeekUi(
                        label = "Last week",
                        days = listOf(
                            dayUi(LocalDate.of(2026, 11, 10), trained = true),
                            dayUi(LocalDate.of(2026, 11, 12), trained = true),
                        ),
                    ),
                    TrainingWeekUi(
                        label = "This week",
                        days = listOf(
                            dayUi(LocalDate.of(2026, 11, 17), trained = true, isToday = true),
                        ),
                    ),
                ),
            ),
        )
        assert(
            summary.contentDescription() ==
                "Last 7 days, 11–17 Nov 2026, 3 workouts, 4h 18m training time, " +
                "42 working sets, 14820 kg total weight, 8 improved, 5 maintained, 2 declined, " +
                "days trained Last week 10, 12, This week 17",
        )
    }

    @Test
    fun `days trained ui marks today future days and week labels`() {
        val friday = LocalDate.of(2026, 11, 6)
        val ui = emptyList<WorkoutSession>().lastSevenDaysSummary(friday).toUi()

        assert(ui.daysTrained.weeks.map { it.label } == listOf("This week", "Next week"))
        val today = ui.daysTrained.weeks[0].days.single { it.date == friday }
        assert(today.isToday)
        assert(!today.isFuture)
        assert(ui.daysTrained.weeks[1].days.all { it.isFuture && !it.isToday })
    }

    @Test
    fun `days trained content description uses none when a week has no workouts`() {
        val daysTrained = DaysTrainedUi(
            weeks = listOf(
                TrainingWeekUi(
                    "This week",
                    listOf(dayUi(LocalDate.of(2026, 11, 21), trained = true)),
                ),
                TrainingWeekUi("Next week", listOf(dayUi(LocalDate.of(2026, 11, 24)))),
            ),
        )
        assert(daysTrained.contentDescription() == "This week 21, Next week none")
    }

    @Test
    fun `top set content description names load and 1RM change`() {
        val topSet = ExerciseTopSetUi(
            name = "Romanian Deadlift",
            weightKg = "45",
            reps = 12,
            oneRm = Weight.kilograms(63f),
            oneRmChange = OneRmChange.Improved(Weight.kilograms(2.5f)),
        )
        assert(topSet.contentDescription() == "Romanian Deadlift, 45 kg x 12, ↑ 2.5 kg")
    }

    @Test
    fun `unchanged 1RM content description says no change`() {
        val topSet = ExerciseTopSetUi(
            name = "Barbell Bench Press",
            weightKg = "60",
            reps = 8,
            oneRm = Weight.kilograms(76f),
            oneRmChange = OneRmChange.Unchanged,
        )
        assert(topSet.contentDescription() == "Barbell Bench Press, 60 kg x 8, No change")
    }

    @Test
    fun `missing assisted load content description uses zero kilograms`() {
        val topSet = ExerciseTopSetUi(
            name = "Assisted Pull-up",
            weightKg = "0",
            reps = 8,
            oneRm = null,
        )
        assert(topSet.contentDescription() == "Assisted Pull-up, 0 kg x 8, -")
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

    private fun dayUi(
        date: LocalDate,
        trained: Boolean = false,
        isToday: Boolean = false,
        isFuture: Boolean = false,
    ) = TrainingDayUi(date, trained, isToday, isFuture)

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
