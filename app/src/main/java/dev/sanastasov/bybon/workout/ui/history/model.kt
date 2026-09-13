package dev.sanastasov.bybon.workout.ui.history

import dev.sanastasov.bybon.workout.domain.SetState
import dev.sanastasov.bybon.workout.domain.WorkoutExercise
import dev.sanastasov.bybon.workout.domain.WorkoutSession
import dev.sanastasov.bybon.workout.domain.WorkoutState
import java.time.LocalDate

data class WorkoutSessionHistoryUi(
    val key: String,
    val planName: String,
    val date: LocalDate,
    val exercises: List<ExerciseTopSetUi>,
)

data class ExerciseTopSetUi(
    val name: String,
    val weightKg: String,
    val reps: Int,
    val estimatedOneRmKg: Float?,
)

fun List<WorkoutSession>.toHistoryUi(): List<WorkoutSessionHistoryUi> =
    filter { it.state is WorkoutState.Completed }
        .sortedByDescending { (it.state as WorkoutState.Completed).startedAt }
        .map { it.toHistoryUi() }

internal fun WorkoutSession.toHistoryUi(): WorkoutSessionHistoryUi {
    val completed = state as WorkoutState.Completed
    return WorkoutSessionHistoryUi(
        key = "${planId.id}-${completed.startedAt}",
        planName = planName,
        date = completed.startedAt.toLocalDate(),
        exercises = exercises.mapNotNull { it.toTopSetUi() },
    )
}

private fun WorkoutExercise.toTopSetUi(): ExerciseTopSetUi? {
    val topSet = sets
        .filter { it.setState == SetState.Completed }
        .maxWithOrNull(
            compareBy(
                { it.weight.kilogramsValue },
                { it.reps },
            )
        ) ?: return null
    return ExerciseTopSetUi(
        name = exerciseDefinition.name,
        weightKg = topSet.weight.kilograms,
        reps = topSet.reps,
        estimatedOneRmKg = topSet.oneRm,
    )
}
