package dev.sanastasov.bybon.workout.ui.summary

import dev.sanastasov.bybon.workout.domain.ExerciseSet
import dev.sanastasov.bybon.workout.domain.Weight
import dev.sanastasov.bybon.workout.domain.WorkoutExercise
import dev.sanastasov.bybon.workout.domain.WorkoutSession
import dev.sanastasov.bybon.workout.domain.WorkoutSessionId
import dev.sanastasov.bybon.workout.domain.WorkoutState
import dev.sanastasov.bybon.workout.ui.toLoadLabel

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
    val notes: List<String> = emptyList(),
)

data class WorkoutSummarySetUi(
    val number: Int?,
    val weightKg: String,
    val reps: Int,
    val oneRm: Weight?,
    val isWarmup: Boolean = false,
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
    val warmups = warmupSets.orEmpty().map { it.toWarmupSummaryUi() }
    val work = sets.mapIndexed { index, set -> set.toWorkSummaryUi(index + 1) }
    return WorkoutSummaryExerciseUi(
        id = id,
        name = exerciseDefinition.name,
        sets = warmups + work,
        notes = notes,
    )
}

private fun ExerciseSet.toWarmupSummaryUi(): WorkoutSummarySetUi = WorkoutSummarySetUi(
    number = null,
    weightKg = weight.toLoadLabel(),
    reps = reps,
    oneRm = null,
    isWarmup = true,
)

private fun ExerciseSet.toWorkSummaryUi(number: Int): WorkoutSummarySetUi = WorkoutSummarySetUi(
    number = number,
    weightKg = weight.toLoadLabel(),
    reps = reps,
    oneRm = oneRm,
)
