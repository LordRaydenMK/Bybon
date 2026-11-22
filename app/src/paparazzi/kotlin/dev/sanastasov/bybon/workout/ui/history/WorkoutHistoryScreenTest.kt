package dev.sanastasov.bybon.workout.ui.history

import app.cash.paparazzi.DeviceConfig
import app.cash.paparazzi.Paparazzi
import dev.sanastasov.bybon.paparazzi.bybonSnapshot
import dev.sanastasov.bybon.workout.domain.OneRmChange
import dev.sanastasov.bybon.workout.domain.Weight
import dev.sanastasov.bybon.workout.domain.WorkoutPlanId
import dev.sanastasov.bybon.workout.domain.WorkoutSessionId
import dev.sanastasov.bybon.workout.domain.catalogExercise
import dev.sanastasov.bybon.workout.domain.estimateOneRmKg
import dev.sanastasov.bybon.workout.ui.formatElapsedClock
import dev.sanastasov.bybon.workout.ui.toLoadLabel
import dev.sanastasov.bybon.workout.ui.toStartTimeLabel
import java.time.LocalDate
import java.time.LocalDateTime
import kotlin.time.Duration.Companion.seconds
import org.junit.Rule
import org.junit.Test

class WorkoutHistoryScreenTest {

    @get:Rule
    val paparazzi: Paparazzi = Paparazzi(
        deviceConfig = DeviceConfig.PIXEL_9,
        theme = "android:Theme.Material.Light.NoActionBar",
    )

    @Test
    fun empty() {
        paparazzi.bybonSnapshot {
            WorkoutHistoryTab(
                uiState = WorkoutHistoryUiState.Empty,
                onSessionClick = {},
                onImportHistoryClick = {},
                onImportDone = {},
            )
        }
    }

    @Test
    fun importSummary() {
        paparazzi.bybonSnapshot {
            WorkoutHistoryTab(
                uiState = WorkoutHistoryUiState.Summary(
                    ImportSummaryUi(
                        sessionCount = 52,
                        sessionsByPlan = listOf(
                            PlanSessionCountUi("Full Body B", 23),
                            PlanSessionCountUi("Full Body A", 23),
                            PlanSessionCountUi("Upper body A", 3),
                            PlanSessionCountUi("Upper body B", 3),
                        ),
                        plansCreatedCount = 2,
                        plansArchivedCount = 2,
                        exercisesImportedCount = 1,
                        firstSessionDate = LocalDate.of(2026, 2, 17),
                        lastSessionDate = LocalDate.of(2026, 8, 20),
                        workingSetCount = 825,
                        sessionsSkipped = 0,
                    ),
                ),
                onSessionClick = {},
                onImportHistoryClick = {},
                onImportDone = {},
            )
        }
    }
}

class WorkoutHistoryListScreenTest {

    @get:Rule
    val paparazzi: Paparazzi = Paparazzi(
        deviceConfig = DeviceConfig.PIXEL_9.copy(screenHeight = 2720),
        theme = "android:Theme.Material.Light.NoActionBar",
        useDeviceResolution = true,
    )

    @Test
    fun history() {
        val today = LocalDate.of(2026, 11, 17)
        paparazzi.bybonSnapshot {
            WorkoutHistoryTab(
                uiState = WorkoutHistoryUiState.History(
                    weekSummary(),
                    historySessions(today).toHistoryPeriodSections(today),
                ),
                onSessionClick = {},
                onImportHistoryClick = {},
                onImportDone = {},
            )
        }
    }
}

private fun weekSummary(): LastSevenDaysSummaryUi = LastSevenDaysSummaryUi(
    dateRange = "11–17 Nov 2026",
    workoutCount = 3,
    trainingTime = "4h 18m",
    workingSetCount = 42,
    totalWeight = "14820 kg",
    performance = LastSevenDaysPerformanceUi(
        improved = 8,
        maintained = 5,
        declined = 2,
    ),
    daysTrained = daysTrained(),
)

private fun daysTrained(): DaysTrainedUi {
    val today = LocalDate.of(2026, 11, 17)
    val trained = setOf(
        LocalDate.of(2026, 11, 10),
        LocalDate.of(2026, 11, 12),
        LocalDate.of(2026, 11, 14),
        today,
    )
    return DaysTrainedUi(
        weeks = listOf(
            trainingWeek("Last week", LocalDate.of(2026, 11, 10), today, trained),
            trainingWeek("This week", LocalDate.of(2026, 11, 17), today, trained),
        ),
    )
}

private fun trainingWeek(
    label: String,
    start: LocalDate,
    today: LocalDate,
    trained: Set<LocalDate>,
): TrainingWeekUi = TrainingWeekUi(
    label = label,
    days = List(7) { offset ->
        val date = start.plusDays(offset.toLong())
        TrainingDayUi(
            date = date,
            trained = date in trained,
            isToday = date == today,
            isFuture = date > today,
        )
    },
)

private fun historySessions(today: LocalDate): List<WorkoutSessionHistoryUi> = listOf(
    historySession(
        planId = "upper-body-b",
        planName = "Upper body B",
        startedAt = today.atTime(17, 59),
        durationSeconds = 3367,
        topSet("incline-db-press", 20f, 14, OneRmChange.Improved(Weight.kilograms(0.66f))),
        topSet("chest-supported-db-row", 22f, 15, OneRmChange.Decreased(Weight.kilograms(0.42f))),
        topSet("incline-db-curl", 9f, 15, OneRmChange.Unchanged),
        ExerciseTopSetUi(
            name = catalogExercise("assisted-pull-ups").name,
            weightKg = "-20",
            reps = 8,
            oneRm = null,
            oneRmChange = OneRmChange.LessAssistance(Weight.kilograms(5)),
        ),
    ),
    historySession(
        planId = "full-body-a",
        planName = "Full Body A",
        startedAt = LocalDate.of(2026, 11, 16).atTime(11, 54),
        durationSeconds = 3877,
        topSet("bench-press", 57.5f, 10),
        topSet("squat", 45f, 12),
    ),
)

private fun historySession(
    planId: String,
    planName: String,
    startedAt: LocalDateTime,
    durationSeconds: Int,
    vararg exercises: ExerciseTopSetUi,
): WorkoutSessionHistoryUi = WorkoutSessionHistoryUi(
    id = WorkoutSessionId(WorkoutPlanId(planId), startedAt),
    planName = planName,
    date = startedAt.toLocalDate(),
    startTime = startedAt.toStartTimeLabel(),
    duration = durationSeconds.seconds.formatElapsedClock(),
    exercises = exercises.toList(),
)

private fun topSet(
    exerciseId: String,
    kg: Float,
    reps: Int,
    oneRmChange: OneRmChange? = null,
): ExerciseTopSetUi {
    val exercise = catalogExercise(exerciseId)
    val weight = Weight.kilograms(kg)
    return ExerciseTopSetUi(
        name = exercise.name,
        weightKg = weight.toLoadLabel(exercise.equipment),
        reps = reps,
        oneRm = Weight.kilograms(estimateOneRmKg(kg, reps)),
        oneRmChange = oneRmChange,
    )
}
