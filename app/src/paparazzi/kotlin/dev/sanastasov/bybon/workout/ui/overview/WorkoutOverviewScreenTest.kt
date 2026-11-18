package dev.sanastasov.bybon.workout.ui.overview

import app.cash.paparazzi.DeviceConfig
import app.cash.paparazzi.Paparazzi
import dev.sanastasov.bybon.paparazzi.bybonSnapshot
import dev.sanastasov.bybon.workout.domain.PreviousSetPerformance
import dev.sanastasov.bybon.workout.domain.SetState
import dev.sanastasov.bybon.workout.domain.Weight
import dev.sanastasov.bybon.workout.domain.fullBodyA
import dev.sanastasov.bybon.workout.domain.toOverviewSession
import java.time.LocalDateTime
import kotlin.time.Duration
import org.junit.Rule
import org.junit.Test

class WorkoutOverviewScreenTest {

    @get:Rule
    val paparazzi: Paparazzi = Paparazzi(
        deviceConfig = DeviceConfig.PIXEL_9,
        theme = "android:Theme.Material.Light.NoActionBar",
    )

    @Test
    fun fromPlan() {
        paparazzi.bybonSnapshot {
            OverviewScreenContent(
                overviewUiState(),
                {},
                {},
            )
        }
    }

    @Test
    fun withDraft() {
        paparazzi.bybonSnapshot {
            OverviewScreenContent(
                overviewUiState(canDiscardDraft = true),
                {},
                {},
            )
        }
    }
}

private fun overviewUiState(canDiscardDraft: Boolean = false): WorkoutOverviewUiState {
    val previous = fullBodyA.toOverviewSession().let { session ->
        session.copy(
            exercises = session.exercises.map { exercise ->
                exercise.copy(
                    sets = exercise.sets.map { set ->
                        set.copy(
                            weight = Weight.kilograms(45),
                            reps = 9,
                            setState = SetState.Completed(),
                            previous = PreviousSetPerformance(Weight.kilograms(45), 9),
                        )
                    },
                    warmupSets = exercise.warmupSets?.map { set ->
                        set.copy(setState = SetState.Completed())
                    },
                )
            },
            startedAt = LocalDateTime.of(2026, 1, 1, 12, 0),
            duration = Duration.ZERO,
        )
    }
    val session = fullBodyA.toOverviewSession(previous)
    return WorkoutOverviewUiState(
        session.copy(
            exercises = session.exercises.mapIndexed { exerciseIndex, exercise ->
                exercise.copy(
                    sets = exercise.sets.mapIndexed { setIndex, set ->
                        val reps = if (exerciseIndex == 0 && setIndex == exercise.sets.lastIndex) {
                            set.reps - 1
                        } else {
                            set.reps + 1
                        }
                        set.copy(reps = reps)
                    },
                )
            },
        ),
        previousDate = previousPlanDateLabel(previous.startedAt),
        canDiscardDraft = canDiscardDraft,
    )
}
