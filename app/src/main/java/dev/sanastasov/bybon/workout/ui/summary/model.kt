package dev.sanastasov.bybon.workout.ui.summary

import dev.sanastasov.bybon.workout.domain.SetState
import dev.sanastasov.bybon.workout.domain.WorkoutExercise
import dev.sanastasov.bybon.workout.domain.WorkoutSession

sealed class WorkoutSummaryUiState {
    data object Loading : WorkoutSummaryUiState()
    data object NotFound : WorkoutSummaryUiState()
    data class Content(
        val title: String,
        val exercises: List<WorkoutSummaryExerciseUi>,
    ) : WorkoutSummaryUiState()
}

data class WorkoutSummaryExerciseUi(
    val id: String,
    val name: String,
    val sets: List<WorkoutSummarySetUi>,
)

data class WorkoutSummarySetUi(
    val number: Int,
    val weightKg: String,
    val reps: Int,
    val estimatedOneRmKg: Float?,
)

internal fun WorkoutSession.toSummaryUi(): WorkoutSummaryUiState.Content =
    WorkoutSummaryUiState.Content(
        title = planName,
        exercises = exercises.mapNotNull { it.toSummaryExerciseUi() },
    )

private fun WorkoutExercise.toSummaryExerciseUi(): WorkoutSummaryExerciseUi? {
    val completedSets = sets.mapIndexedNotNull { index, set ->
        if (set.setState != SetState.Completed) {
            null
        } else {
            WorkoutSummarySetUi(
                number = index + 1,
                weightKg = set.weight.kilograms,
                reps = set.reps,
                estimatedOneRmKg = set.oneRm,
            )
        }
    }
    if (completedSets.isEmpty()) return null
    return WorkoutSummaryExerciseUi(
        id = id,
        name = exerciseDefinition.name,
        sets = completedSets,
    )
}
