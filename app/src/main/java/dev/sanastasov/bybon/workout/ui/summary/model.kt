package dev.sanastasov.bybon.workout.ui.summary

import dev.sanastasov.bybon.workout.domain.Weight
import dev.sanastasov.bybon.workout.domain.WorkoutExercise
import dev.sanastasov.bybon.workout.domain.WorkoutSession
import dev.sanastasov.bybon.workout.domain.WorkoutSessionId
import dev.sanastasov.bybon.workout.domain.WorkoutState

sealed class WorkoutSummaryUiState {
    data object Loading : WorkoutSummaryUiState()
    data class Content(
        val title: String,
        val exercises: List<WorkoutSummaryExerciseUi>,
        val note: String? = null,
    ) : WorkoutSummaryUiState()
}

data class WorkoutSummaryExerciseUi(
    val id: String,
    val name: String,
    val sets: List<WorkoutSummarySetUi>,
    val note: String? = null,
)

data class WorkoutSummarySetUi(
    val number: Int,
    val weightKg: String,
    val reps: Int,
    val oneRm: Weight,
)

internal fun List<WorkoutSession>.requireCompletedSummary(
    sessionId: WorkoutSessionId,
): WorkoutSummaryUiState.Content =
    first { it.id == sessionId && it.state is WorkoutState.Completed }.toSummaryUi()

internal fun WorkoutSession.toSummaryUi(): WorkoutSummaryUiState.Content =
    WorkoutSummaryUiState.Content(
        title = planName,
        exercises = exercises.mapNotNull { it.toSummaryExerciseUi() },
        note = note,
    )

private fun WorkoutExercise.toSummaryExerciseUi(): WorkoutSummaryExerciseUi? {
    if (sets.isEmpty()) return null
    return WorkoutSummaryExerciseUi(
        id = id,
        name = exerciseDefinition.name,
        sets = sets.mapIndexed { index, set ->
            WorkoutSummarySetUi(
                number = index + 1,
                weightKg = set.weight.kilograms,
                reps = set.reps,
                oneRm = set.oneRm,
            )
        },
        note = note,
    )
}
