package dev.sanastasov.bybon.workout.ui.session

import androidx.compose.foundation.pager.rememberPagerState
import app.cash.paparazzi.DeviceConfig
import app.cash.paparazzi.Paparazzi
import dev.sanastasov.bybon.paparazzi.bybonSnapshot
import dev.sanastasov.bybon.workout.domain.SetState
import dev.sanastasov.bybon.workout.domain.Weight
import dev.sanastasov.bybon.workout.domain.completeSet
import dev.sanastasov.bybon.workout.domain.fullBodyA
import dev.sanastasov.bybon.workout.domain.toWorkoutSession
import java.time.LocalDateTime
import kotlin.time.Duration
import org.junit.Rule
import org.junit.Test

class WorkoutSessionScreenTest {

    @get:Rule
    val paparazzi: Paparazzi = Paparazzi(
        deviceConfig = DeviceConfig.PIXEL_9,
        theme = "android:Theme.Material.Light.NoActionBar",
    )

    @Test
    fun withPrevious() {
        val startedAt = LocalDateTime.of(2026, 10, 24, 12, 0)
        paparazzi.bybonSnapshot {
            SessionScreenContent(
                sessionWithPrevious(startedAt),
                rememberPagerState(0) { fullBodyA.sets.size },
                {},
                {},
                now = { startedAt.plusMinutes(8) },
            )
        }
    }

    @Test
    fun restAfterWarmup() {
        val startedAt = LocalDateTime.of(2026, 10, 24, 12, 0)
        val completedAt = startedAt.plusMinutes(3)
        val session = (0..2).fold(fullBodyA.toWorkoutSession(startedAt = startedAt)) { current, index ->
            current.completeSet(current.exercises.first(), index, isWarmup = true, now = completedAt)
        }
        paparazzi.bybonSnapshot {
            SessionScreenContent(
                session,
                rememberPagerState(session.currentExerciseIndex ?: 0) { session.exercises.size },
                {},
                {},
                now = { completedAt.plusSeconds(45) },
            )
        }
    }
}

private fun sessionWithPrevious(startedAt: LocalDateTime) = fullBodyA.toWorkoutSession(
    previousSession = fullBodyA.toWorkoutSession().let { session ->
        session.copy(
            exercises = session.exercises.map { exercise ->
                exercise.copy(
                    sets = exercise.sets.mapIndexed { index, set ->
                        set.copy(
                            weight = Weight.kilograms(40 + index),
                            reps = 9,
                            setState = SetState.Completed(),
                        )
                    },
                    warmupSets = exercise.warmupSets?.mapIndexed { index, set ->
                        set.copy(
                            weight = Weight.kilograms(22 + index),
                            reps = 6,
                            setState = SetState.Completed(),
                        )
                    },
                )
            },
            startedAt = LocalDateTime.of(2026, 1, 1, 12, 0),
            duration = Duration.ZERO,
        )
    },
    startedAt = startedAt,
)
