package dev.sanastasov.bybon.workout.ui

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Card
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import dev.sanastasov.bybon.workout.domain.SetState
import dev.sanastasov.bybon.workout.domain.Weight
import dev.sanastasov.bybon.workout.domain.WorkoutSession
import dev.sanastasov.bybon.workout.domain.completeSet
import dev.sanastasov.bybon.workout.domain.fullBodyA
import dev.sanastasov.bybon.workout.domain.toWorkoutSession
import java.time.LocalDateTime
import kotlin.time.Duration

private fun previewSessionWithPrevious(): WorkoutSession {
    val previous = fullBodyA.toWorkoutSession().let { session ->
        session.copy(
            exercises = session.exercises.map { exercise ->
                exercise.copy(
                    sets = exercise.sets.mapIndexed { index, set ->
                        set.copy(
                            weight = Weight.kilograms(40 + index),
                            reps = 9,
                            setState = SetState.Completed,
                        )
                    },
                    warmupSets = exercise.warmupSets?.mapIndexed { index, set ->
                        set.copy(
                            weight = Weight.kilograms(22 + index),
                            reps = 6,
                            setState = SetState.Completed,
                        )
                    },
                )
            },
            startedAt = LocalDateTime.of(2026, 1, 1, 12, 0),
            duration = Duration.ZERO,
        )
    }
    return fullBodyA.toWorkoutSession(previous)
}

@Preview(showBackground = true, name = "Overview card")
@Composable
private fun OverviewExerciseCardPreview() {
    Card(Modifier.fillMaxWidth()) {
        ExerciseCard(
            previewSessionWithPrevious().exercises.first(),
            ExerciseCardMode.Overview,
            {},
            overflow = ExerciseOverflow(false, true, {}, {}),
        )
    }
}

@Preview(showBackground = true, name = "Session card")
@Composable
private fun SessionExerciseCardPreview() {
    val session = previewSessionWithPrevious()
    val progressed = session.completeSet(session.exercises.first(), 0, isWarmup = true)
    Card(Modifier.fillMaxWidth()) {
        ExerciseCard(
            progressed.exercises.first(),
            ExerciseCardMode.Session,
            {},
        )
    }
}

@Preview(showBackground = true, name = "Session card — warmups done")
@Composable
private fun SessionExerciseCardWarmupsCompletedPreview() {
    val session = previewSessionWithPrevious()
    val exercise = session.exercises.first()
    val warmupCount = exercise.warmupSets?.size ?: 0
    val progressed = (0 until warmupCount).fold(session) { current, index ->
        current.completeSet(current.exercises.first(), index, isWarmup = true)
    }
    Card(Modifier.fillMaxWidth()) {
        ExerciseCard(
            progressed.exercises.first(),
            ExerciseCardMode.Session,
            {},
        )
    }
}
