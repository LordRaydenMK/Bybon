package dev.sanastasov.bybon.workout.ui.session

import androidx.compose.foundation.pager.rememberPagerState
import app.cash.paparazzi.DeviceConfig
import app.cash.paparazzi.Paparazzi
import dev.sanastasov.bybon.paparazzi.bybonSnapshot
import dev.sanastasov.bybon.workout.domain.SetState
import dev.sanastasov.bybon.workout.domain.Weight
import dev.sanastasov.bybon.workout.domain.WorkoutSession
import dev.sanastasov.bybon.workout.domain.completeSet
import dev.sanastasov.bybon.workout.domain.fullBodyA
import dev.sanastasov.bybon.workout.domain.toWorkoutSession
import java.time.LocalDateTime
import kotlin.time.Duration
import org.junit.Rule
import org.junit.Test

class WorkoutSessionAssistedScreenTest {

    @get:Rule
    val paparazzi: Paparazzi = Paparazzi(
        deviceConfig = DeviceConfig.PIXEL_9,
        theme = "android:Theme.Material.Light.NoActionBar",
    )

    @Test
    fun assistedPullUps() {
        val startedAt = LocalDateTime.of(2026, 10, 24, 12, 0)
        val completedAt = startedAt.plusMinutes(18)
        val session = assistedPullUpSession(startedAt).completeThroughAssistedWorkSet(
            lastWorkIndex = 0,
            now = completedAt,
        )
        paparazzi.bybonSnapshot {
            SessionScreenContent(
                session,
                rememberPagerState(session.currentExerciseIndex ?: 0) { session.exercises.size },
                {},
                {},
                now = { completedAt.plusSeconds(20) },
            )
        }
    }
}

private fun assistedPullUpSession(startedAt: LocalDateTime): WorkoutSession {
    val bodyWeight = Weight.kilograms(80)
    return fullBodyA.toWorkoutSession(
        previousSession = fullBodyA.toWorkoutSession().let { session ->
            session.copy(
                exercises = session.exercises.map { exercise ->
                    if (exercise.id != "assisted-pull-ups") {
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
                    } else {
                        exercise.copy(
                            warmupSets = exercise.warmupSets?.map { set ->
                                set.copy(
                                    weight = Weight.kilograms(40),
                                    reps = 6,
                                    setState = SetState.Completed(),
                                    bodyWeight = bodyWeight,
                                )
                            },
                            sets = exercise.sets.mapIndexed { index, set ->
                                set.copy(
                                    weight = when (index) {
                                        0 -> Weight.kilograms(25)
                                        1 -> null
                                        else -> Weight.kilograms(20)
                                    },
                                    reps = 8,
                                    setState = SetState.Completed(),
                                    bodyWeight = bodyWeight,
                                )
                            },
                        )
                    }
                },
                startedAt = LocalDateTime.of(2026, 1, 1, 12, 0),
                duration = Duration.ZERO,
            )
        },
        startedAt = startedAt,
        bodyWeight = bodyWeight,
    )
}

private fun WorkoutSession.completeThroughAssistedWorkSet(
    lastWorkIndex: Int,
    now: LocalDateTime,
): WorkoutSession {
    val pullUpIndex = exercises.indexOfFirst { it.id == "assisted-pull-ups" }
    var current = this
    for (exerciseIndex in 0..pullUpIndex) {
        val warmupCount = current.exercises[exerciseIndex].warmupSets?.size ?: 0
        val workCount = if (exerciseIndex < pullUpIndex) {
            current.exercises[exerciseIndex].sets.size
        } else {
            lastWorkIndex + 1
        }
        repeat(warmupCount) { index ->
            current = current.completeSet(
                current.exercises[exerciseIndex],
                index,
                isWarmup = true,
                now = now,
            )
        }
        repeat(workCount) { index ->
            current = current.completeSet(
                current.exercises[exerciseIndex],
                index,
                isWarmup = false,
                now = now,
            )
        }
    }
    return current
}
